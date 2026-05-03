import asyncio
import json
import logging
import time
from datetime import datetime, timezone

from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
from aiokafka.errors import KafkaError

from app.config import settings
from app.models import MessageModeratedEvent, MessageSentEvent
from app.moderator import moderate

logger = logging.getLogger(__name__)

_consumer: AIOKafkaConsumer | None = None
_producer: AIOKafkaProducer | None = None
_running = False


def _utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


async def _publish_verdict(producer: AIOKafkaProducer, event: MessageModeratedEvent) -> None:
    payload = event.model_dump_json().encode("utf-8")
    partition_key = event.messageId.encode("utf-8")
    await producer.send_and_wait(settings.kafka_output_topic, value=payload, key=partition_key)
    logger.info(
        "Published verdict: messageId=%s verdict=%s",
        event.messageId,
        event.verdict,
    )


async def _process_message(raw_value: bytes, producer: AIOKafkaProducer) -> None:
    from app.main import (
        messages_moderated_total,
        messages_rejected_total,
        moderation_fallback_total,
        moderation_processing_seconds,
    )

    try:
        data = json.loads(raw_value.decode("utf-8"))
        incoming = MessageSentEvent(**data)
    except Exception as exc:
        logger.error("Failed to deserialize MessageSent event: %s — raw: %s", exc, raw_value)
        return

    start = time.perf_counter()
    verdict, used_fallback = await moderate(incoming.content)
    elapsed = time.perf_counter() - start

    messages_moderated_total.inc()
    moderation_processing_seconds.observe(elapsed)

    if used_fallback:
        moderation_fallback_total.inc()

    if verdict == "REJECTED":
        messages_rejected_total.inc()

    outgoing = MessageModeratedEvent(
        messageId=incoming.messageId,
        roomId=incoming.roomId,
        verdict=verdict,
        timestamp=_utc_now(),
    )

    # At-least-once: publish verdict before committing offset
    await _publish_verdict(producer, outgoing)


async def start_consumer() -> None:
    global _consumer, _producer, _running
    _running = True

    _consumer = AIOKafkaConsumer(
        settings.kafka_input_topic,
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_consumer_group,
        enable_auto_commit=False,  # manual commit for at-least-once delivery
        auto_offset_reset="earliest",
    )
    _producer = AIOKafkaProducer(
        bootstrap_servers=settings.kafka_bootstrap_servers,
    )

    # Retry connecting to Kafka — it may not be ready when this pod starts
    max_attempts = 20
    retry_delay = 5  # seconds
    for attempt in range(1, max_attempts + 1):
        try:
            await _consumer.start()
            await _producer.start()
            logger.info(
                "Kafka consumer started. Listening on topic '%s'", settings.kafka_input_topic
            )
            break
        except Exception as exc:
            logger.warning(
                "Kafka not ready (attempt %d/%d): %s — retrying in %ds",
                attempt, max_attempts, exc, retry_delay,
            )
            await asyncio.sleep(retry_delay)
    else:
        logger.error(
            "Could not connect to Kafka after %d attempts. Consumer will not run.", max_attempts
        )
        return

    try:
        async for msg in _consumer:
            if not _running:
                break
            try:
                await _process_message(msg.value, _producer)
                # Commit offset only after verdict is successfully published
                await _consumer.commit()
            except KafkaError as exc:
                logger.error(
                    "Kafka error while processing message offset=%s: %s",
                    msg.offset,
                    exc,
                )
            except Exception as exc:
                logger.error(
                    "Unexpected error processing message offset=%s: %s",
                    msg.offset,
                    exc,
                )
    finally:
        await _consumer.stop()
        await _producer.stop()
        logger.info("Kafka consumer/producer stopped.")


async def stop_consumer() -> None:
    global _running
    _running = False
    if _consumer:
        await _consumer.stop()
    if _producer:
        await _producer.stop()
