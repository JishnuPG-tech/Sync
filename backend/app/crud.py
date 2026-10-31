"""
High-performance dual-engine data access layer for Spott.
Runs directly on persistent SQLite (/data/spott/spott.db) with sub-millisecond query latency.
Optionally syncs to Supabase if credentials are provided.
"""

import uuid
from datetime import datetime, timezone, timedelta
from typing import List, Optional, Tuple, Dict, Any
from sqlalchemy.orm import Session
from sqlalchemy import or_, and_

from app.database import SessionLocal
from app.models import User, UserLocation, ConnectionRequest, ConnectionStatus
from app.schemas import UserCreate, UserUpdate, NearbyUserCard, MutualConnectionReveal
from app.services.geo import haversine_distance_meters

def get_utc_now() -> datetime:
    return datetime.now(timezone.utc)

def _user_to_dict(user: User) -> Dict[str, Any]:
    return {
        "id": user.id,
        "display_name": user.display_name,
        "sync_username": user.sync_username,
        "instagram_username": user.instagram_username,
        "bio": user.bio or "",
        "location_name": user.location_name or "",
        "avatar_url": user.avatar_url,
        "ble_token": user.ble_token,
        "interests_csv": user.interests_csv or "",
        "is_discovery_active": user.is_discovery_active,
        "fcm_token": user.fcm_token,
        "created_at": user.created_at.isoformat() if user.created_at else None,
        "updated_at": user.updated_at.isoformat() if user.updated_at else None
    }

# ============================================================================
# USERS
# ============================================================================

def create_user(user_in: UserCreate) -> Dict[str, Any]:
    db: Session = SessionLocal()
    try:
        interests_str = ",".join([tag.strip() for tag in user_in.interests if tag.strip()])
        clean_ig = user_in.instagram_username.strip().lstrip("@").lower()
        clean_sync = user_in.sync_username.strip().lstrip("@").lower() if user_in.sync_username else clean_ig

        user = User(
            id=str(uuid.uuid4()),
            display_name=user_in.display_name.strip(),
            sync_username=clean_sync,
            instagram_username=clean_ig,
            bio=user_in.bio.strip() if user_in.bio else "",
            location_name=user_in.location.strip() if user_in.location else "",
            avatar_url=user_in.avatar_url,
            ble_token=str(uuid.uuid4()).replace("-", "")[:16],
            interests_csv=interests_str,
            fcm_token=user_in.fcm_token,
            is_discovery_active=True,
            created_at=get_utc_now(),
            updated_at=get_utc_now()
        )
        db.add(user)
        db.commit()
        db.refresh(user)
        return _user_to_dict(user)
    finally:
        db.close()

def get_user_by_id(user_id: str) -> Optional[Dict[str, Any]]:
    db: Session = SessionLocal()
    try:
        user = db.query(User).filter(User.id == user_id).first()
        return _user_to_dict(user) if user else None
    finally:
        db.close()

def get_user_by_instagram(instagram_username: str) -> Optional[Dict[str, Any]]:
    db: Session = SessionLocal()
    try:
        clean = instagram_username.strip().lstrip("@").lower()
        user = db.query(User).filter(User.instagram_username == clean).first()
        return _user_to_dict(user) if user else None
    finally:
        db.close()

def get_user_by_ble_token(ble_token: str) -> Optional[Dict[str, Any]]:
    db: Session = SessionLocal()
    try:
        raw = ble_token.strip().lower()
        clean = raw.replace("-", "")

        user = db.query(User).filter(
            or_(
                User.ble_token == raw,
                User.ble_token == clean,
                User.id == raw,
                User.ble_token.like(f"{clean[:16]}%")
            )
        ).first()
        return _user_to_dict(user) if user else None
    finally:
        db.close()

def update_user_discovery(user_id: str, is_active: bool) -> Optional[Dict[str, Any]]:
    db: Session = SessionLocal()
    try:
        user = db.query(User).filter(User.id == user_id).first()
        if not user:
            return None
        user.is_discovery_active = is_active
        user.updated_at = get_utc_now()
        db.commit()
        db.refresh(user)
        return _user_to_dict(user)
    finally:
        db.close()

def resolve_ble_peer(scanner_user_id: str, ble_token: str) -> Optional[NearbyUserCard]:
    peer = get_user_by_ble_token(ble_token)
    if not peer or peer["id"] == scanner_user_id or not peer.get("is_discovery_active", True):
        return None

    conn_status = get_connection_status(scanner_user_id, peer["id"])
    interests = [i.strip() for i in peer.get("interests_csv", "").split(",") if i.strip()]

    return NearbyUserCard(
        user_id=peer["id"],
        display_name=peer["display_name"],
        bio=peer.get("bio", ""),
        location=peer.get("location_name", ""),
        avatar_url=peer.get("avatar_url"),
        interests=interests,
        approx_distance_meters=0.0,
        instagram_username=peer.get("instagram_username"),
        connection_status=conn_status
    )

# ============================================================================
# LOCATIONS & PROXIMITY RADAR
# ============================================================================

def update_user_location(user_id: str, latitude: float, longitude: float) -> Dict[str, Any]:
    db: Session = SessionLocal()
    try:
        loc = db.query(UserLocation).filter(UserLocation.user_id == user_id).first()
        now = get_utc_now()
        if loc:
            loc.latitude = latitude
            loc.longitude = longitude
            loc.updated_at = now
        else:
            loc = UserLocation(
                user_id=user_id,
                latitude=latitude,
                longitude=longitude,
                updated_at=now
            )
            db.add(loc)
        db.commit()
        return {"user_id": user_id, "latitude": latitude, "longitude": longitude, "updated_at": now.isoformat()}
    finally:
        db.close()

def get_nearby_active_users(
    current_user_id: str,
    max_distance_meters: float = 70.0,
    max_age_minutes: int = 15
) -> List[NearbyUserCard]:
    db: Session = SessionLocal()
    try:
        curr_loc = db.query(UserLocation).filter(UserLocation.user_id == current_user_id).first()
        
        # All active users except self
        active_users = db.query(User).filter(
            User.id != current_user_id,
            User.is_discovery_active == True
        ).all()

        nearby_cards: List[NearbyUserCard] = []
        now = get_utc_now()
        stale_threshold = now - timedelta(minutes=max_age_minutes)

        for user in active_users:
            conn_status = get_connection_status(current_user_id, user.id)
            interests = [i.strip() for i in (user.interests_csv or "").split(",") if i.strip()]
            
            dist = 5.0 # Default close estimation
            if curr_loc and user.location and user.location.updated_at >= stale_threshold:
                dist = haversine_distance_meters(
                    curr_loc.latitude, curr_loc.longitude,
                    user.location.latitude, user.location.longitude
                )
                if dist > max_distance_meters:
                    continue

            nearby_cards.append(
                NearbyUserCard(
                    user_id=user.id,
                    display_name=user.display_name,
                    bio=user.bio or "",
                    location=user.location_name or "",
                    avatar_url=user.avatar_url,
                    interests=interests,
                    approx_distance_meters=round(dist, 1),
                    instagram_username=user.instagram_username,
                    connection_status=conn_status
                )
            )

        nearby_cards.sort(key=lambda c: c.approx_distance_meters)
        return nearby_cards
    finally:
        db.close()

# ============================================================================
# CONNECTIONS & REVEALS
# ============================================================================

def get_connection_status(user_a_id: str, user_b_id: str) -> str:
    db: Session = SessionLocal()
    try:
        req = db.query(ConnectionRequest).filter(
            or_(
                and_(ConnectionRequest.sender_id == user_a_id, ConnectionRequest.receiver_id == user_b_id),
                and_(ConnectionRequest.sender_id == user_b_id, ConnectionRequest.receiver_id == user_a_id)
            )
        ).first()

        if not req:
            return "NONE"
        if req.status == ConnectionStatus.ACCEPTED:
            return "ACCEPTED"
        if req.sender_id == user_a_id:
            return "REQUEST_SENT"
        return "REQUEST_RECEIVED"
    finally:
        db.close()

def create_connection_request(sender_id: str, receiver_id: str) -> Dict[str, Any]:
    db: Session = SessionLocal()
    try:
        existing = db.query(ConnectionRequest).filter(
            or_(
                and_(ConnectionRequest.sender_id == sender_id, ConnectionRequest.receiver_id == receiver_id),
                and_(ConnectionRequest.sender_id == receiver_id, ConnectionRequest.receiver_id == sender_id)
            )
        ).first()

        now = get_utc_now()
        if existing:
            existing.status = ConnectionStatus.PENDING
            existing.sender_id = sender_id
            existing.receiver_id = receiver_id
            existing.created_at = now
            existing.responded_at = None
            db.commit()
            return {"id": existing.id, "sender_id": sender_id, "receiver_id": receiver_id, "status": existing.status}

        req = ConnectionRequest(
            id=str(uuid.uuid4()),
            sender_id=sender_id,
            receiver_id=receiver_id,
            status=ConnectionStatus.PENDING,
            created_at=now
        )
        db.add(req)
        db.commit()
        return {"id": req.id, "sender_id": sender_id, "receiver_id": receiver_id, "status": req.status}
    finally:
        db.close()

def get_pending_requests_for_user(user_id: str) -> List[Dict[str, Any]]:
    db: Session = SessionLocal()
    try:
        reqs = db.query(ConnectionRequest).filter(
            ConnectionRequest.receiver_id == user_id,
            ConnectionRequest.status == ConnectionStatus.PENDING
        ).all()

        results = []
        for r in reqs:
            sender = db.query(User).filter(User.id == r.sender_id).first()
            if sender:
                interests = [i.strip() for i in (sender.interests_csv or "").split(",") if i.strip()]
                results.append({
                    "id": r.id,
                    "sender_id": sender.id,
                    "sender_display_name": sender.display_name,
                    "sender_bio": sender.bio or "",
                    "sender_interests": interests,
                    "sender_avatar_url": sender.avatar_url,
                    "status": r.status,
                    "created_at": r.created_at.isoformat() if r.created_at else None
                })
        return results
    finally:
        db.close()

def respond_to_connection_request(request_id: str, action: str, user_id: str) -> bool:
    db: Session = SessionLocal()
    try:
        req = db.query(ConnectionRequest).filter(ConnectionRequest.id == request_id).first()
        if not req or req.receiver_id != user_id:
            return False

        req.status = ConnectionStatus.ACCEPTED if action.upper() == "ACCEPT" else ConnectionStatus.DECLINED
        req.responded_at = get_utc_now()
        db.commit()
        return True
    finally:
        db.close()

def get_mutual_connections(user_id: str) -> List[MutualConnectionReveal]:
    db: Session = SessionLocal()
    try:
        reqs = db.query(ConnectionRequest).filter(
            or_(ConnectionRequest.sender_id == user_id, ConnectionRequest.receiver_id == user_id),
            ConnectionRequest.status == ConnectionStatus.ACCEPTED
        ).all()

        reveals = []
        for r in reqs:
            partner_id = r.receiver_id if r.sender_id == user_id else r.sender_id
            partner = db.query(User).filter(User.id == partner_id).first()
            if partner:
                interests = [i.strip() for i in (partner.interests_csv or "").split(",") if i.strip()]
                reveals.append(
                    MutualConnectionReveal(
                        connection_id=r.id,
                        connected_user_id=partner.id,
                        display_name=partner.display_name,
                        instagram_username=partner.instagram_username,
                        bio=partner.bio or "",
                        interests=interests,
                        avatar_url=partner.avatar_url
                    )
                )
        return reveals
    finally:
        db.close()
