# GhostMesh Android App (Kotlin + Jetpack Compose)

Modern, privacy-first nearby peer discovery application built with Jetpack Compose & Material 3.

---

## Key Features

1. **Live Input Validation**:
   - Real-time syntax checking as the user types their Instagram username.
   - Rejects spaces, periods at the start/end, consecutive periods (`..`), and invalid symbols.
   - Visual feedback: Red error message below the field when invalid; green checkmark when valid.
   - Form button remains disabled until both Display Name and Instagram username are valid.

2. **Strict Privacy Shield**:
   - In Nearby Peer discovery, Instagram usernames are **never shown or requested**.
   - Cards display Display Name, approximate distance (e.g. `~14m away`), and interest tags with a locked badge: *"🔒 Instagram profile hidden"*.

3. **Mutual-Consent Flow**:
   - A connection request displays: *"Arun K wants to connect"*.
   - Only when the receiver taps **Accept** does the mutual connection unlock.

4. **1-Tap Native Instagram Launch**:
   - Once connected, tapping **"Open on Instagram"** fires an Android Intent directly launching the official Instagram app to that user's profile (`instagram://user?username=...`).

5. **Ghost Mode**:
   - 1-tap switch in the top app bar instantly pauses radar and makes the user invisible to nearby peers.
