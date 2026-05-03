import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Response
from prometheus_client import Counter, Histogram, make_asgi_app

from app.consumer import start_consumer, stop_consumer
from app.moderator import _get_detoxify_model

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

# --- Prometheus metrics ---
messages_moderated_total = Counter(
    "messages_moderated_total",
    "Total number of messages processed by the moderation service",
)
messages_rejected_total = Counter(
    "messages_rejected_total",
    "Total number of messages with a REJECTED verdict",
)
moderation_fallback_total = Counter(
    "moderation_fallback_total",
    "Total number of times the Perspective API fallback was used",
)
moderation_processing_seconds = Histogram(
    "moderation_processing_seconds",
    "Time in seconds to process each message through moderation",
)

# Track whether the model loaded successfully
_model_ready = False


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _model_ready
    logger.info("Starting Moderation Service...")

    # Eagerly load the detoxify model at startup so the first message
    # is not penalized by model loading time (~5-10s)
    logger.info("Loading detoxify model into memory...")
    try:
        await asyncio.get_event_loop().run_in_executor(None, _get_detoxify_model)
        _model_ready = True
        logger.info("detoxify model loaded successfully.")
    except Exception as exc:
        logger.warning("Failed to pre-load detoxify model: %s — will retry on first message", exc)

    consumer_task = asyncio.create_task(start_consumer())
    yield

    logger.info("Shutting down Moderation Service...")
    await stop_consumer()
    consumer_task.cancel()
    try:
        await consumer_task
    except asyncio.CancelledError:
        pass


app = FastAPI(title="Moderation Service", lifespan=lifespan)

# Mount Prometheus metrics endpoint
metrics_app = make_asgi_app()
app.mount("/metrics", metrics_app)


@app.get("/health")
async def health():
    from app.consumer import _consumer, _running
    consumer_healthy = _running and _consumer is not None
    if not consumer_healthy:
        return Response(
            content='{"status": "DOWN", "reason": "Kafka consumer not running"}',
            status_code=503,
            media_type="application/json",
        )
    return {"status": "UP"}
