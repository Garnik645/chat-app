import os
from dataclasses import dataclass, field


@dataclass
class Settings:
    kafka_bootstrap_servers: str = field(
        default_factory=lambda: os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
    )
    perspective_api_key: str = field(
        default_factory=lambda: os.getenv("PERSPECTIVE_API_KEY", "")
    )
    toxicity_threshold: float = field(
        default_factory=lambda: float(os.getenv("TOXICITY_THRESHOLD", "0.8"))
    )
    sexual_explicit_threshold: float = field(
        default_factory=lambda: float(os.getenv("SEXUAL_EXPLICIT_THRESHOLD", "0.8"))
    )
    kafka_input_topic: str = "chat.messages"
    kafka_output_topic: str = "moderation.results"
    kafka_consumer_group: str = "moderation-service-consumer"


settings = Settings()
