"""
Data access layer powered entirely by Supabase REST API (supabase-py).
Zero raw SQL passwords, zero pooler/port connection issues.
"""

import uuid
from datetime import datetime, timezone, timedelta
from typing import List, Optional, Tuple, Dict, Any
from app.supabase_client import get_supabase
from app.schemas import UserCreate, NearbyUserCard, MutualConnectionReveal
from app.services.geo import haversine_distance_meters

def get_utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()

# ============================================================================
# USERS
# ============================================================================

def create_user(user_in: UserCreate) -> Dict[str, Any]:
    supabase = get_supabase()
    user_id = str(uuid.uuid4())
    ble_token = str(uuid.uuid4())
    interests_str = ",".join([tag.strip() for tag in user_in.interests if tag.strip()])

    payload = {
        "id": user_id,
        "display_name": user_in.display_name.strip(),
        "sync_username": user_in.sync_username.strip().lower() if user_in.sync_username else None,
        "instagram_username": user_in.instagram_username.strip().lower(),
        "bio": user_in.bio.strip() if user_in.bio else "",
        "location_name": user_in.location.strip() if user_in.location else "",
        "avatar_url": user_in.avatar_url,
        "ble_token": ble_token,
        "interests_csv": interests_str,
        "fcm_token": user_in.fcm_token,
        "is_discovery_active": True,
        "created_at": get_utc_now_iso(),
        "updated_at": get_utc_now_iso()
    }

    res = supabase.table("users").insert(payload).execute()
    if res.data:
        return res.data[0]
    return payload

def get_user_by_id(user_id: str) -> Optional[Dict[str, Any]]:
    supabase = get_supabase()
    res = supabase.table("users").select("*").eq("id", user_id).limit(1).execute()
    return res.data[0] if res.data else None

def get_user_by_instagram(instagram_username: str) -> Optional[Dict[str, Any]]:
    supabase = get_supabase()
    clean = instagram_username.strip().lower()
    res = supabase.table("users").select("*").eq("instagram_username", clean).limit(1).execute()
    return res.data[0] if res.data else None

def get_user_by_ble_token(ble_token: str) -> Optional[Dict[str, Any]]:
    supabase = get_supabase()
    raw = ble_token.strip().lower()
    clean = raw.replace("-", "")

    if len(clean) == 16:
        # 8-byte token (16 hex chars): match exact 16-char token OR prefix of UUID ble_token / id
        prefix_uuid = f"{clean[:8]}-{clean[8:12]}-{clean[12:16]}"
        res = supabase.table("users").select("*").or_(
            f"ble_token.eq.{clean},ble_token.ilike.{prefix_uuid}%,id.ilike.{prefix_uuid}%"
        ).limit(1).execute()
        if res.data:
            return res.data[0]

    # Full UUID or other token: check raw, clean, and dashed UUID format
    or_clauses = [f"ble_token.eq.{raw}", f"id.eq.{raw}"]
    if clean != raw:
        or_clauses.extend([f"ble_token.eq.{clean}", f"id.eq.{clean}"])
    if len(clean) == 32:
        formatted_uuid = f"{clean[:8]}-{clean[8:12]}-{clean[12:16]}-{clean[16:20]}-{clean[20:]}"
        or_clauses.extend([f"ble_token.eq.{formatted_uuid}", f"id.eq.{formatted_uuid}"])

    res = supabase.table("users").select("*").or_(",".join(set(or_clauses))).limit(1).execute()
    return res.data[0] if res.data else None

def update_user_discovery(user_id: str, is_active: bool) -> Optional[Dict[str, Any]]:
    supabase = get_supabase()
    payload = {
        "is_discovery_active": is_active,
        "updated_at": get_utc_now_iso()
    }
    res = supabase.table("users").update(payload).eq("id", user_id).execute()
    return res.data[0] if res.data else None

def is_location_fresh(updated_at_str: Optional[str], max_age_minutes: int = 15) -> bool:
    if not updated_at_str:
        return False
    try:
        cleaned = updated_at_str.replace("Z", "+00:00")
        loc_time = datetime.fromisoformat(cleaned)
        diff_minutes = (datetime.now(timezone.utc) - loc_time).total_seconds() / 60.0
        return diff_minutes <= max_age_minutes
    except Exception:
        return True

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
        connection_status=conn_status
    )

# ============================================================================
# LOCATIONS & PROXIMITY RADAR
# ============================================================================

def update_user_location(user_id: str, latitude: float, longitude: float) -> Dict[str, Any]:
    supabase = get_supabase()
    payload = {
        "user_id": user_id,
        "latitude": latitude,
        "longitude": longitude,
        "updated_at": get_utc_now_iso()
    }
    res = supabase.table("user_locations").upsert(payload).execute()
    return res.data[0] if res.data else payload

def get_nearby_active_users(
    current_user_id: str,
    max_distance_meters: float = 70.0,
    max_age_minutes: int = 15
) -> List[NearbyUserCard]:
    """
    Finds active nearby peers.
    1. If GPS coordinates exist, calculates haversine distance.
    2. If no GPS coordinate pinged yet, shows active registered peers so users immediately see each other!
    CRITICAL: Never returns instagram_username!
    """
    supabase = get_supabase()

    # Get current user's location if available
    curr_res = supabase.table("user_locations").select("*").eq("user_id", current_user_id).limit(1).execute()
    curr_loc = curr_res.data[0] if curr_res.data else None

    # Fetch all active discovery users except self
    users_res = supabase.table("users").select("*").neq("id", current_user_id).eq("is_discovery_active", True).execute()
    all_users = users_res.data or []

    nearby_cards: List[NearbyUserCard] = []

    for user in all_users:
        conn_status = get_connection_status(current_user_id, user["id"])
        interests = [i.strip() for i in user.get("interests_csv", "").split(",") if i.strip()]

        if not curr_loc or not is_location_fresh(curr_loc.get("updated_at"), max_age_minutes):
            # Current user has no fresh GPS ping recorded — proximity requires recent physical location
            continue

        # Check partner's location
        partner_loc_res = supabase.table("user_locations").select("*").eq("user_id", user["id"]).limit(1).execute()
        if not partner_loc_res.data:
            # Partner has no GPS ping recorded — skip so we only show real physical peers
            continue

        partner_loc = partner_loc_res.data[0]
        if not is_location_fresh(partner_loc.get("updated_at"), max_age_minutes):
            # Partner location is stale (> max_age_minutes) — skip
            continue
        dist = haversine_distance_meters(
            curr_loc["latitude"], curr_loc["longitude"],
            partner_loc["latitude"], partner_loc["longitude"]
        )
        if dist > max_distance_meters:
            # Outside physical proximity radius — skip!
            continue
        dist = round(dist, 1)

        nearby_cards.append(
            NearbyUserCard(
                user_id=user["id"],
                display_name=user["display_name"],
                bio=user.get("bio", ""),
                location=user.get("location_name", ""),
                avatar_url=user.get("avatar_url"),
                interests=interests,
                approx_distance_meters=dist,
                connection_status=conn_status
            )
        )

    nearby_cards.sort(key=lambda x: x.approx_distance_meters)
    return nearby_cards

# ============================================================================
# CONNECTIONS & MUTUAL REVEALS
# ============================================================================

def get_connection_status(user_a_id: str, user_b_id: str) -> str:
    supabase = get_supabase()
    res = supabase.table("connection_requests").select("*").or_(
        f"and(sender_id.eq.{user_a_id},receiver_id.eq.{user_b_id}),and(sender_id.eq.{user_b_id},receiver_id.eq.{user_a_id})"
    ).order("created_at", desc=True).limit(1).execute()

    if not res.data:
        return "NONE"

    req = res.data[0]
    if req["status"] == "ACCEPTED":
        return "CONNECTED"
    if req["status"] == "PENDING":
        return "PENDING_SENT" if req["sender_id"] == user_a_id else "PENDING_RECEIVED"
    return "NONE"

def create_connection_request(sender_id: str, receiver_id: str) -> Tuple[Optional[Dict[str, Any]], str]:
    if sender_id == receiver_id:
        return None, "Cannot connect with yourself"

    supabase = get_supabase()
    sender = get_user_by_id(sender_id)
    receiver = get_user_by_id(receiver_id)
    if not sender or not receiver:
        return None, "User not found"

    # Check for existing request within 24 hours
    cutoff = (datetime.now(timezone.utc) - timedelta(hours=24)).isoformat()
    res = supabase.table("connection_requests").select("*").or_(
        f"and(sender_id.eq.{sender_id},receiver_id.eq.{receiver_id}),and(sender_id.eq.{receiver_id},receiver_id.eq.{sender_id})"
    ).gte("created_at", cutoff).limit(1).execute()

    if res.data:
        existing = res.data[0]
        if existing["status"] == "ACCEPTED":
            return None, "Already connected"
        if existing["status"] == "PENDING":
            return None, "Request already pending"
        return None, "Please wait before requesting again (24-hour cooldown)"

    req_id = str(uuid.uuid4())
    payload = {
        "id": req_id,
        "sender_id": sender_id,
        "receiver_id": receiver_id,
        "status": "PENDING",
        "created_at": get_utc_now_iso()
    }
    ins_res = supabase.table("connection_requests").insert(payload).execute()
    return (ins_res.data[0] if ins_res.data else payload), "Request sent successfully"

def respond_to_connection_request(request_id: str, receiver_id: str, action: str) -> Tuple[bool, str]:
    supabase = get_supabase()
    res = supabase.table("connection_requests").select("*").eq("id", request_id).eq("receiver_id", receiver_id).eq("status", "PENDING").limit(1).execute()
    if not res.data:
        return False, "Pending request not found"

    new_status = "ACCEPTED" if action == "ACCEPT" else "DECLINED"
    payload = {
        "status": new_status,
        "responded_at": get_utc_now_iso()
    }
    supabase.table("connection_requests").update(payload).eq("id", request_id).execute()
    msg = "Connection accepted" if action == "ACCEPT" else "Connection declined"
    return True, msg

def get_pending_requests_for_user(user_id: str) -> List[Dict[str, Any]]:
    supabase = get_supabase()
    res = supabase.table("connection_requests").select("*, sender:users!connection_requests_sender_id_fkey(*)").eq("receiver_id", user_id).eq("status", "PENDING").order("created_at", desc=True).execute()

    pending_list = []
    for row in res.data or []:
        sender = row.get("sender") or {}
        interests = [i.strip() for i in sender.get("interests_csv", "").split(",") if i.strip()]
        pending_list.append({
            "id": row["id"],
            "sender_id": row["sender_id"],
            "sender_display_name": sender.get("display_name", "Nearby User"),
            "sender_bio": sender.get("bio", ""),
            "sender_avatar_url": sender.get("avatar_url"),
            "sender_interests": interests,
            "created_at": row["created_at"]
        })
    return pending_list

def get_mutual_connections(user_id: str) -> List[MutualConnectionReveal]:
    """
    THE REVEAL FUNCTION:
    Only returns Instagram usernames for confirmed, ACCEPTED mutual connections.
    """
    supabase = get_supabase()
    res = supabase.table("connection_requests").select("*, sender:users!connection_requests_sender_id_fkey(*), receiver:users!connection_requests_receiver_id_fkey(*)").or_(
        f"sender_id.eq.{user_id},receiver_id.eq.{user_id}"
    ).eq("status", "ACCEPTED").order("responded_at", desc=True).execute()

    reveals: List[MutualConnectionReveal] = []
    for req in res.data or []:
        partner = req.get("receiver") if req["sender_id"] == user_id else req.get("sender")
        if partner:
            interests = [i.strip() for i in partner.get("interests_csv", "").split(",") if i.strip()]
            reveals.append(
                MutualConnectionReveal(
                    connection_id=req["id"],
                    connected_user_id=partner["id"],
                    display_name=partner["display_name"],
                    instagram_username=partner["instagram_username"], # Unlocked securely
                    bio=partner.get("bio", ""),
                    location=partner.get("location_name", ""),
                    avatar_url=partner.get("avatar_url"),
                    interests=interests,
                    connected_at=datetime.fromisoformat(req["responded_at"]) if req.get("responded_at") else datetime.now(timezone.utc)
                )
            )
    return reveals
