from pydantic import BaseModel
from typing import Literal


class MessageSentEvent(BaseModel):
    messageId: str
    roomId: str
    userId: str
    content: str
    timestamp: str


class MessageModeratedEvent(BaseModel):
    messageId: str
    roomId: str
    verdict: Literal["APPROVED", "REJECTED"]
    timestamp: str
