from datetime import datetime, timezone, timedelta
from typing import List, Optional, Tuple
from sqlalchemy.orm import Session
from sqlalchemy import or_, and_
from app.models import User, UserLocation, ConnectionRequest, ConnectionStatus
from app.schemas import UserCreate, UserUpdate, NearbyUserCard, MutualConnectionReveal
from app.services.geo import haversine_distance_meters

def get_utc_now() -> datetime:
    return datetime.now(timezone.utc)

def create_user(db: Session, user_in: UserCreate) -> User:
    interests_str = ",".join([tag.strip() for tag in user_in.interests if tag.strip()])
    db_user = User(
        display_name=user_in.display_name.strip(),
        sync_username=user_in.sync_username.strip().lower() if user_in.sync_username else None,
        instagram_username=user_in.instagram_username.strip().lower(),
        bio=user_in.bio.strip() if user_in.bio else "",
        location_name=user_in.location.strip() if user_in.location else "",
        avatar_url=user_in.avatar_url,
        interests_csv=interests_str,
        fcm_token=user_in.fcm_token,
        is_discovery_active=True
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user

def get_user_by_id(db: Session, user_id: str) -> Optional[User]:
    return db.query(User).filter(User.id == user_id).first()

def get_user_by_instagram(db: Session, instagram_username: str) -> Optional[User]:
    return db.query(User).filter(User.instagram_username == instagram_username.strip().lower()).first()

def get_user_by_ble_token(db: Session, ble_token: str) -> Optional[User]:
    return db.query(User).filter(User.ble_token == ble_token.strip()).first()

def resolve_ble_peer(db: Session, scanner_user_id: str, ble_token: str) -> Optional[NearbyUserCard]:
    peer = get_user_by_ble_token(db, ble_token)
    if not peer:
        return None

    # Must not match self
    if peer.id == scanner_user_id:
        return None

    # Discovery must be active
    if not peer.is_discovery_active:
        return None

    conn_status = get_connection_status(db, scanner_user_id, peer.id)
    return NearbyUserCard(
        user_id=peer.id,
        display_name=peer.display_name,
        bio=peer.bio or "",
        location=peer.location_name or "",
        avatar_url=peer.avatar_url,
        interests=peer.interests_list,
        approx_distance_meters=0.0, # Determined by BLE RSSI on device
        connection_status=conn_status
    )

def update_user_discovery(db: Session, user_id: str, is_active: bool) -> Optional[User]:
    user = get_user_by_id(db, user_id)
    if not user:
        return None
    user.is_discovery_active = is_active
    db.commit()
    db.refresh(user)
    return user

def update_user_location(db: Session, user_id: str, latitude: float, longitude: float) -> UserLocation:
    location = db.query(UserLocation).filter(UserLocation.user_id == user_id).first()
    now = get_utc_now()
    if location:
        location.latitude = latitude
        location.longitude = longitude
        location.updated_at = now
    else:
        location = UserLocation(
            user_id=user_id,
            latitude=latitude,
            longitude=longitude,
            updated_at=now
        )
        db.add(location)
    db.commit()
    db.refresh(location)
    return location

def get_connection_status(db: Session, user_a_id: str, user_b_id: str) -> str:
    """Determines relationship status between user_a and user_b."""
    req = db.query(ConnectionRequest).filter(
        or_(
            and_(ConnectionRequest.sender_id == user_a_id, ConnectionRequest.receiver_id == user_b_id),
            and_(ConnectionRequest.sender_id == user_b_id, ConnectionRequest.receiver_id == user_a_id),
        )
    ).order_by(ConnectionRequest.created_at.desc()).first()

    if not req:
        return "NONE"
    if req.status == ConnectionStatus.ACCEPTED:
        return "CONNECTED"
    if req.status == ConnectionStatus.PENDING:
        return "PENDING_SENT" if req.sender_id == user_a_id else "PENDING_RECEIVED"
    return "NONE"

def get_nearby_active_users(
    db: Session,
    current_user_id: str,
    max_distance_meters: float = 40.0,
    max_age_minutes: int = 10
) -> List[NearbyUserCard]:
    """
    Finds active nearby peers within radius.
    CRITICAL: Never leaks instagram_username!
    """
    current_loc = db.query(UserLocation).filter(UserLocation.user_id == current_user_id).first()
    if not current_loc:
        return []

    cutoff_time = get_utc_now() - timedelta(minutes=max_age_minutes)

    # Query all other users whose discovery is active and location is fresh
    candidates = db.query(User, UserLocation).join(
        UserLocation, User.id == UserLocation.user_id
    ).filter(
        User.id != current_user_id,
        User.is_discovery_active == True,
        UserLocation.updated_at >= cutoff_time
    ).all()

    nearby_cards: List[NearbyUserCard] = []

    for user, loc in candidates:
        distance = haversine_distance_meters(
            current_loc.latitude, current_loc.longitude,
            loc.latitude, loc.longitude
        )

        if distance <= max_distance_meters:
            conn_status = get_connection_status(db, current_user_id, user.id)
            nearby_cards.append(
                NearbyUserCard(
                    user_id=user.id,
                    display_name=user.display_name,
                    bio=user.bio or "",
                    location=user.location_name or "",
                    avatar_url=user.avatar_url,
                    interests=user.interests_list,
                    approx_distance_meters=round(distance, 1),
                    connection_status=conn_status
                )
            )

    # Sort closest first
    nearby_cards.sort(key=lambda x: x.approx_distance_meters)
    return nearby_cards

def create_connection_request(db: Session, sender_id: str, receiver_id: str) -> Tuple[Optional[ConnectionRequest], str]:
    if sender_id == receiver_id:
        return None, "Cannot connect with yourself"

    sender = get_user_by_id(db, sender_id)
    receiver = get_user_by_id(db, receiver_id)
    if not sender or not receiver:
        return None, "User not found"

    # Check for existing request within 24 hours
    cutoff = get_utc_now() - timedelta(hours=24)
    existing = db.query(ConnectionRequest).filter(
        or_(
            and_(ConnectionRequest.sender_id == sender_id, ConnectionRequest.receiver_id == receiver_id),
            and_(ConnectionRequest.sender_id == receiver_id, ConnectionRequest.receiver_id == sender_id),
        ),
        ConnectionRequest.created_at >= cutoff
    ).first()

    if existing:
        if existing.status == ConnectionStatus.ACCEPTED:
            return None, "Already connected"
        if existing.status == ConnectionStatus.PENDING:
            return None, "Request already pending"
        return None, "Please wait before requesting again (24-hour cooldown)"

    req = ConnectionRequest(
        sender_id=sender_id,
        receiver_id=receiver_id,
        status=ConnectionStatus.PENDING,
        created_at=get_utc_now()
    )
    db.add(req)
    db.commit()
    db.refresh(req)
    return req, "Request sent successfully"

def respond_to_connection_request(db: Session, request_id: str, receiver_id: str, action: str) -> Tuple[bool, str]:
    req = db.query(ConnectionRequest).filter(
        ConnectionRequest.id == request_id,
        ConnectionRequest.receiver_id == receiver_id,
        ConnectionRequest.status == ConnectionStatus.PENDING
    ).first()

    if not req:
        return False, "Pending request not found"

    req.responded_at = get_utc_now()
    if action == "ACCEPT":
        req.status = ConnectionStatus.ACCEPTED
        db.commit()
        return True, "Connection accepted"
    else:
        req.status = ConnectionStatus.DECLINED
        db.commit()
        return True, "Connection declined"

def get_pending_requests_for_user(db: Session, user_id: str) -> List[ConnectionRequest]:
    return db.query(ConnectionRequest).filter(
        ConnectionRequest.receiver_id == user_id,
        ConnectionRequest.status == ConnectionStatus.PENDING
    ).order_by(ConnectionRequest.created_at.desc()).all()

def get_mutual_connections(db: Session, user_id: str) -> List[MutualConnectionReveal]:
    """
    THE REVEAL FUNCTION:
    Only returns Instagram usernames for confirmed, ACCEPTED mutual connections.
    """
    accepted_requests = db.query(ConnectionRequest).filter(
        or_(ConnectionRequest.sender_id == user_id, ConnectionRequest.receiver_id == user_id),
        ConnectionRequest.status == ConnectionStatus.ACCEPTED
    ).order_by(ConnectionRequest.responded_at.desc()).all()

    reveals: List[MutualConnectionReveal] = []
    for req in accepted_requests:
        partner_id = req.receiver_id if req.sender_id == user_id else req.sender_id
        partner = get_user_by_id(db, partner_id)
        if partner:
            reveals.append(
                MutualConnectionReveal(
                    connection_id=req.id,
                    connected_user_id=partner.id,
                    display_name=partner.display_name,
                    instagram_username=partner.instagram_username, # SECURELY UNLOCKED!
                    bio=partner.bio or "",
                    location=partner.location_name or "",
                    avatar_url=partner.avatar_url,
                    interests=partner.interests_list,
                    connected_at=req.responded_at
                )
            )
    return reveals
