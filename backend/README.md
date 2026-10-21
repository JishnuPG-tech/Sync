# GhostMesh Backend API (FastAPI)

Privacy-first, zero-content-creation proximity discovery API for mutual Instagram networking.

---

## Quickstart

### 1. Install Dependencies
```bash
pip install -r requirements.txt
```

### 2. Run the Development Server
```bash
uvicorn app.main:app --reload --port 8000
```
Open interactive Swagger API Documentation: [http://localhost:8000/docs](http://localhost:8000/docs)

### 3. Run Automated Tests
```bash
$env:PYTHONPATH="backend"; python -m pytest backend/tests -v
```

---

## API Architecture & Endpoints

### 1. Users (`/api/v1/users`)
- `POST /register`: Register display name, private Instagram handle, and interest tags.
- `GET /{user_id}`: Retrieve private profile (for account owner only).
- `PATCH /{user_id}/discovery`: Toggle Ghost Mode / Radar discovery ON/OFF.

### 2. Radar & Proximity (`/api/v1/radar`)
- `POST /ping`: Upload coarse coordinates when displacement occurs.
- `GET /nearby/{user_id}`: Discovers active peers within radius (e.g. 40 meters).
  - **Privacy Guarantee**: `instagram_username` is strictly excluded from responses. Only display name, distance, and tags are shown.

### 3. Mutual Connections (`/api/v1/connections`)
- `POST /request`: Send connection request.
- `GET /pending/{user_id}`: View incoming connection requests.
- `POST /{request_id}/respond`: Accept or Decline a request.
- `GET /mutual/{user_id}`: **The Reveal Endpoint** — Returns confirmed mutual connections with unlocked Instagram handles for 1-tap following.
