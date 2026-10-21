import uuid
from datetime import datetime, timezone
from sqlalchemy import Column, String, Boolean, Float, DateTime, ForeignKey, Index
from sqlalchemy.orm import relationship
from app.database import Base

def generate_uuid() -> str:
    return str(uuid.uuid4())

def get_utc_now() -> datetime:
    return datetime.now(timezone.utc)

class User(Base):
    __tablename__ = "users"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    display_name = Column(String(50), nullable=False) # e.g. "Arun K"
    sync_username = Column(String(30), nullable=True, unique=True) # e.g. "arun_k"
    instagram_username = Column(String(30), nullable=False, unique=True) # e.g. "arun_k99"
    bio = Column(String(160), default="")
    location_name = Column(String(100), default="")
    avatar_url = Column(String(500), nullable=True) # Saved photo URL or relative path
    ble_token = Column(String(36), unique=True, index=True, default=generate_uuid) # 16-byte rotating BLE advertising token
    interests_csv = Column(String(255), default="") # Comma-separated tags, e.g. "Tech,Fitness,Gaming"
    is_discovery_active = Column(Boolean, default=True) # Ghost mode toggle (Radar ON/OFF)
    fcm_token = Column(String(255), nullable=True) # For push notifications
    created_at = Column(DateTime, default=get_utc_now)
    updated_at = Column(DateTime, default=get_utc_now, onupdate=get_utc_now)

    # Relationships
    location = relationship("UserLocation", back_populates="user", uselist=False, cascade="all, delete-orphan")
    sent_requests = relationship("ConnectionRequest", foreign_keys="ConnectionRequest.sender_id", back_populates="sender")
    received_requests = relationship("ConnectionRequest", foreign_keys="ConnectionRequest.receiver_id", back_populates="receiver")

    @property
    def interests_list(self) -> list[str]:
        if not self.interests_csv:
            return []
        return [tag.strip() for tag in self.interests_csv.split(",") if tag.strip()]

class UserLocation(Base):
    __tablename__ = "user_locations"

    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    updated_at = Column(DateTime, default=get_utc_now, onupdate=get_utc_now)

    user = relationship("User", back_populates="location")

    __table_args__ = (
        Index("idx_location_lat_lng", "latitude", "longitude"),
    )

class ConnectionStatus:
    PENDING = "PENDING"
    ACCEPTED = "ACCEPTED"
    DECLINED = "DECLINED"

class ConnectionRequest(Base):
    __tablename__ = "connection_requests"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    sender_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    receiver_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    status = Column(String(20), default=ConnectionStatus.PENDING, nullable=False)
    created_at = Column(DateTime, default=get_utc_now)
    responded_at = Column(DateTime, nullable=True)

    sender = relationship("User", foreign_keys=[sender_id], back_populates="sent_requests")
    receiver = relationship("User", foreign_keys=[receiver_id], back_populates="received_requests")

    __table_args__ = (
        Index("idx_conn_sender_receiver", "sender_id", "receiver_id"),
        Index("idx_conn_status", "status"),
    )
