import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.database import Base, get_db
from app.main import app

# In-memory SQLite for high-speed isolated automated testing
TEST_DATABASE_URL = "sqlite:///./test_ghostmesh.db"
test_engine = create_engine(TEST_DATABASE_URL, connect_args={"check_same_thread": False})
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_engine)

def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()

app.dependency_overrides[get_db] = override_get_db

@pytest.fixture(autouse=True)
def setup_database():
    Base.metadata.drop_all(bind=test_engine)
    Base.metadata.create_all(bind=test_engine)
    yield
    Base.metadata.drop_all(bind=test_engine)

client = TestClient(app)

def test_full_privacy_first_flow():
    # 1. Register User 1 (Arun K)
    res_arun = client.post("/api/v1/users/register", json={
        "display_name": "Arun K",
        "instagram_username": "arun_k99",
        "interests": ["Tech", "Gym", "Gaming"]
    })
    assert res_arun.status_code == 201
    arun_data = res_arun.json()
    arun_id = arun_data["id"]
    assert arun_data["display_name"] == "Arun K"
    assert arun_data["instagram_username"] == "arun_k99" # Visible only to owner on registration

    # 2. Register User 2 (Priya M)
    res_priya = client.post("/api/v1/users/register", json={
        "display_name": "Priya M",
        "instagram_username": "priya_m_official",
        "interests": ["Design", "Tech", "Photography"]
    })
    assert res_priya.status_code == 201
    priya_data = res_priya.json()
    priya_id = priya_data["id"]

    # 3. Register User 3 (Far Away User - 5km away)
    res_far = client.post("/api/v1/users/register", json={
        "display_name": "Far Away User",
        "instagram_username": "far_away_user",
        "interests": ["Music"]
    })
    far_id = res_far.json()["id"]

    # 4. Location Updates (Pings)
    # Arun & Priya are in the same campus cafeteria (15 meters apart)
    client.post("/api/v1/radar/ping", json={"user_id": arun_id, "latitude": 12.971600, "longitude": 77.594600})
    client.post("/api/v1/radar/ping", json={"user_id": priya_id, "latitude": 12.971700, "longitude": 77.594680})
    # Far user is 5km away
    client.post("/api/v1/radar/ping", json={"user_id": far_id, "latitude": 13.010000, "longitude": 77.620000})

    # 5. Arun Scans Radar for Nearby Peers
    res_nearby = client.get(f"/api/v1/radar/nearby/{arun_id}?max_distance=40.0")
    assert res_nearby.status_code == 200
    nearby_list = res_nearby.json()

    # Priya must be detected; Far user must NOT be detected
    assert len(nearby_list) == 1
    found_peer = nearby_list[0]
    assert found_peer["user_id"] == priya_id
    assert found_peer["display_name"] == "Priya M"
    assert "Tech" in found_peer["interests"]
    assert found_peer["approx_distance_meters"] < 30.0
    assert found_peer["connection_status"] == "NONE"

    # CRITICAL PRIVACY ASSERTION:
    # Instagram username MUST NOT exist in the nearby peer payload!
    assert "instagram_username" not in found_peer

    # 6. Test Ghost Mode (Priya turns off radar)
    client.patch(f"/api/v1/users/{priya_id}/discovery?is_active=false")
    res_nearby_after_ghost = client.get(f"/api/v1/radar/nearby/{arun_id}")
    assert len(res_nearby_after_ghost.json()) == 0 # Priya is now completely invisible!

    # Priya turns radar back ON
    client.patch(f"/api/v1/users/{priya_id}/discovery?is_active=true")
    res_reappear = client.get(f"/api/v1/radar/nearby/{arun_id}")
    assert len(res_reappear.json()) == 1

    # 7. Arun Sends Connection Request to Priya
    req_res = client.post("/api/v1/connections/request", json={
        "sender_id": arun_id,
        "receiver_id": priya_id
    })
    assert req_res.status_code == 201
    request_id = req_res.json()["request_id"]

    # 8. Priya Checks Incoming Requests
    pending_res = client.get(f"/api/v1/connections/pending/{priya_id}")
    assert pending_res.status_code == 200
    pending_list = pending_res.json()
    assert len(pending_list) == 1
    assert pending_list[0]["sender_display_name"] == "Arun K"
    # CRITICAL PRIVACY ASSERTION:
    # Still NO Instagram username revealed before acceptance!
    assert "instagram_username" not in pending_list[0]

    # 9. Before Acceptance: Mutual reveals must be empty
    mutual_pre = client.get(f"/api/v1/connections/mutual/{arun_id}").json()
    assert len(mutual_pre) == 0

    # 10. Priya Accepts Connection Request
    accept_res = client.post(f"/api/v1/connections/{request_id}/respond?user_id={priya_id}", json={
        "action": "ACCEPT"
    })
    assert accept_res.status_code == 200

    # 11. THE MUTUAL REVEAL:
    # Both users now call /connections/mutual and their Instagram handles are unlocked!
    arun_reveals = client.get(f"/api/v1/connections/mutual/{arun_id}").json()
    assert len(arun_reveals) == 1
    assert arun_reveals[0]["display_name"] == "Priya M"
    assert arun_reveals[0]["instagram_username"] == "priya_m_official" # UNLOCKED!

    priya_reveals = client.get(f"/api/v1/connections/mutual/{priya_id}").json()
    assert len(priya_reveals) == 1
    assert priya_reveals[0]["display_name"] == "Arun K"
    assert priya_reveals[0]["instagram_username"] == "arun_k99" # UNLOCKED!

    # 12. Cooldown check: Duplicate request is rejected
    dup_res = client.post("/api/v1/connections/request", json={
        "sender_id": arun_id,
        "receiver_id": priya_id
    })
    assert dup_res.status_code == 400

def test_instagram_handle_validation():
    # 1. Spaces rejected
    res_space = client.post("/api/v1/users/register", json={
        "display_name": "Test User",
        "instagram_username": "invalid user name",
        "interests": ["Tech"]
    })
    assert res_space.status_code == 422
    assert "cannot contain spaces" in res_space.text

    # 2. Starting with period rejected
    res_start_dot = client.post("/api/v1/users/register", json={
        "display_name": "Test User",
        "instagram_username": ".invalid_user",
        "interests": ["Tech"]
    })
    assert res_start_dot.status_code == 422
    assert "cannot start with a period" in res_start_dot.text

    # 3. Ending with period rejected
    res_end_dot = client.post("/api/v1/users/register", json={
        "display_name": "Test User",
        "instagram_username": "invalid_user.",
        "interests": ["Tech"]
    })
    assert res_end_dot.status_code == 422
    assert "cannot end with a period" in res_end_dot.text

    # 4. Consecutive periods rejected
    res_consec_dots = client.post("/api/v1/users/register", json={
        "display_name": "Test User",
        "instagram_username": "invalid..user",
        "interests": ["Tech"]
    })
    assert res_consec_dots.status_code == 422
    assert "consecutive periods" in res_consec_dots.text

    # 5. Invalid special characters rejected
    res_special = client.post("/api/v1/users/register", json={
        "display_name": "Test User",
        "instagram_username": "user$name!#",
        "interests": ["Tech"]
    })
    assert res_special.status_code == 422

    # 6. Valid handle with leading '@' is cleaned and accepted
    res_clean_at = client.post("/api/v1/users/register", json={
        "display_name": "Valid User",
        "instagram_username": "@valid_handle.123",
        "interests": ["Tech"]
    })
    assert res_clean_at.status_code == 201
    assert res_clean_at.json()["instagram_username"] == "valid_handle.123"

def test_verify_instagram_endpoint(monkeypatch):
    # Test valid profile verification
    async def mock_verify_valid(username):
        return {
            "exists": True,
            "username": username,
            "full_name": "Arun Kumar",
            "avatar_url": "https://cdn.instagram.com/pic.jpg",
            "description": "Tech & Fitness"
        }

    monkeypatch.setattr("app.routers.users.verify_instagram_profile", mock_verify_valid)
    res_valid = client.get("/api/v1/users/verify-instagram/arun_k99")
    assert res_valid.status_code == 200
    data = res_valid.json()
    assert data["exists"] is True
    assert data["username"] == "arun_k99"
    assert data["full_name"] == "Arun Kumar"

    # Test non-existent profile
    async def mock_verify_invalid(username):
        return {
            "exists": False,
            "username": username,
            "error": "Account does not exist"
        }

    monkeypatch.setattr("app.routers.users.verify_instagram_profile", mock_verify_invalid)
    res_invalid = client.get("/api/v1/users/verify-instagram/fake_account_99999")
    assert res_invalid.status_code == 200
    assert res_invalid.json()["exists"] is False

def test_ble_token_resolution():
    # 1. Register User A and User B
    res_a = client.post("/api/v1/users/register", json={
        "display_name": "Scanner User",
        "instagram_username": "scanner_user_ig",
        "interests": ["Tech"]
    })
    user_a = res_a.json()
    
    res_b = client.post("/api/v1/users/register", json={
        "display_name": "Target User",
        "instagram_username": "target_user_ig",
        "interests": ["Fitness", "Design"]
    })
    user_b = res_b.json()
    ble_token_b = user_b["ble_token"]
    assert ble_token_b is not None

    # 2. Scanner phone detects Target's BLE token and resolves it
    res_resolve = client.post("/api/v1/radar/resolve-ble-token", json={
        "ble_token": ble_token_b,
        "scanner_user_id": user_a["id"]
    })
    assert res_resolve.status_code == 200
    resolved_card = res_resolve.json()
    assert resolved_card["user_id"] == user_b["id"]
    assert resolved_card["display_name"] == "Target User"
    assert "Fitness" in resolved_card["interests"]
    # CRITICAL: Instagram handle MUST NOT be leaked in BLE resolution!
    assert "instagram_username" not in resolved_card

    # 3. Target turns off discovery (Ghost mode) -> Resolution fails with 404
    client.patch(f"/api/v1/users/{user_b['id']}/discovery?is_active=false")
    res_ghost = client.post("/api/v1/radar/resolve-ble-token", json={
        "ble_token": ble_token_b,
        "scanner_user_id": user_a["id"]
    })
    assert res_ghost.status_code == 404

    # 4. Scanner cannot resolve their own BLE token
    res_self = client.post("/api/v1/radar/resolve-ble-token", json={
        "ble_token": user_a["ble_token"],
        "scanner_user_id": user_a["id"]
    })
    assert res_self.status_code == 404

def test_photo_upload():
    import base64
    # 1x1 transparent png in base64
    tiny_b64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
    res = client.post("/api/v1/users/upload-photo", json={
        "image_base64": tiny_b64,
        "filename": "my_photo.png"
    })
    assert res.status_code == 200
    data = res.json()
    assert data["success"] is True
    assert "/static/uploads/" in data["photo_url"]

