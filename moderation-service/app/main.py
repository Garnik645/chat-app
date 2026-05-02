import asyncio
import logging
import os
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI
from prometheus_client import Counter, Histogram, make_asgi_app

from app.consumer import start_consumer, stop_consumer

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


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting Moderation Service...")
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
    return {"status": "UP"}
