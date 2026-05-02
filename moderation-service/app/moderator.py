import logging
from typing import Literal

import httpx

from app.config import settings

logger = logging.getLogger(__name__)

# Detoxify thresholds
DETOXIFY_THRESHOLDS = {
    "toxicity": 0.8,
    "severe_toxicity": 0.5,
    "obscene": 0.8,
    "sexual_explicit": 0.8,
}

# Lazy-loaded model instance
_detoxify_model = None


def _get_detoxify_model():
    global _detoxify_model
    if _detoxify_model is None:
        from detoxify import Detoxify
        _detoxify_model = Detoxify("original")
    return _detoxify_model


def _analyze_with_detoxify(content: str) -> Literal["APPROVED", "REJECTED"]:
    model = _get_detoxify_model()
    scores = model.predict(content)
    for category, threshold in DETOXIFY_THRESHOLDS.items():
        score = scores.get(category, 0.0)
        if isinstance(score, (list, tuple)):
            score = score[0]
        if score > threshold:
            logger.info(
                "Message REJECTED by detoxify: category=%s score=%.4f threshold=%.4f",
                category, score, threshold,
            )
            return "REJECTED"
    return "APPROVED"


async def _analyze_with_perspective(content: str) -> Literal["APPROVED", "REJECTED"]:
    """Call the Perspective API as a fallback moderator."""
    api_key = settings.perspective_api_key
    if not api_key:
        raise ValueError("PERSPECTIVE_API_KEY environment variable is not set")

    url = (
        f"https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze"
        f"?key={api_key}"
    )
    payload = {
        "comment": {"text": content},
        "requestedAttributes": {
            "TOXICITY": {},
            "SEXUALLY_EXPLICIT": {},
        },
    }

    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.post(url, json=payload)

    if response.status_code != 200:
        raise RuntimeError(
            f"Perspective API returned non-200 status: {response.status_code} — {response.text}"
        )

    data = response.json()
    attribute_scores = data.get("attributeScores", {})

    toxicity_score = (
        attribute_scores.get("TOXICITY", {})
        .get("summaryScore", {})
        .get("value", 0.0)
    )
    sexual_score = (
        attribute_scores.get("SEXUALLY_EXPLICIT", {})
        .get("summaryScore", {})
        .get("value", 0.0)
    )

    if toxicity_score > settings.toxicity_threshold:
        logger.info(
            "Message REJECTED by Perspective API: TOXICITY=%.4f", toxicity_score
        )
        return "REJECTED"
    if sexual_score > settings.sexual_explicit_threshold:
        logger.info(
            "Message REJECTED by Perspective API: SEXUALLY_EXPLICIT=%.4f", sexual_score
        )
        return "REJECTED"

    return "APPROVED"


async def moderate(content: str) -> tuple[Literal["APPROVED", "REJECTED"], bool]:
    """
    Moderate content using detoxify, falling back to Perspective API.

    Returns (verdict, used_fallback).
    If both fail, returns ("APPROVED", True) with a warning log.
    """
    # Primary: detoxify
    try:
        verdict = _analyze_with_detoxify(content)
        return verdict, False
    except Exception as exc:
        logger.warning("detoxify model failed, attempting Perspective API fallback: %s", exc)

    # Fallback: Perspective API
    try:
        verdict = await _analyze_with_perspective(content)
        return verdict, True
    except Exception as exc:
        logger.warning(
            "Perspective API fallback also failed — defaulting to APPROVED to preserve availability: %s",
            exc,
        )
        return "APPROVED", True
