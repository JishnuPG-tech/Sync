from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List
from app.database import get_db
from app.schemas import (
    ConnectionRequestCreate,
    ConnectionRequestOut,
    ConnectionRespond,
    MutualConnectionReveal
)
from app import crud

router = APIRouter(prefix="/connections", tags=["Connections & Mutual Reveal"])

@router.post("/request", status_code=status.HTTP_201_CREATED)
def send_connection_request(payload: ConnectionRequestCreate, db: Session = Depends(get_db)):
    """
    Sends a connection request to a nearby user.
    The receiver sees ONLY display name and interests.
    """
    req, msg = crud.create_connection_request(db, payload.sender_id, payload.receiver_id)
    if not req:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=msg)
    
    return {
        "status": "success",
        "message": msg,
        "request_id": req.id
    }

@router.get("/pending/{user_id}", response_model=List[ConnectionRequestOut])
def get_pending_incoming_requests(user_id: str, db: Session = Depends(get_db)):
    """
    Fetches all incoming requests for the user.
    Shows sender display name and tags, NEVER their Instagram ID.
    """
    requests = crud.get_pending_requests_for_user(db, user_id)
    out: List[ConnectionRequestOut] = []
    for r in requests:
        if r.sender:
            out.append(
                ConnectionRequestOut(
                    id=r.id,
                    sender_id=r.sender_id,
                    sender_display_name=r.sender.display_name,
                    sender_bio=r.sender.bio or "",
                    sender_location=r.sender.location_name or "",
                    sender_avatar_url=r.sender.avatar_url,
                    sender_interests=r.sender.interests_list,
                    status=r.status,
                    created_at=r.created_at
                )
            )
    return out

@router.post("/{request_id}/respond", status_code=status.HTTP_200_OK)
def respond_to_request(
    request_id: str,
    payload: ConnectionRespond,
    user_id: str,
    db: Session = Depends(get_db)
):
    """
    Accept or Decline a connection request.
    Only the receiver can perform this action.
    """
    success, msg = crud.respond_to_connection_request(db, request_id, user_id, payload.action)
    if not success:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=msg)
    
    return {
        "status": "success",
        "action": payload.action,
        "message": msg
    }

@router.get("/mutual/{user_id}", response_model=List[MutualConnectionReveal])
def get_unlocked_connections(user_id: str, db: Session = Depends(get_db)):
    """
    THE MUTUAL REVEAL ENDPOINT:
    Only when BOTH users have accepted does this endpoint unlock and return
    the actual Instagram handles for 1-tap following!
    """
    user = crud.get_user_by_id(db, user_id)
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
        
    return crud.get_mutual_connections(db, user_id)
