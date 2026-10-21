# AuraRadar (GhostMesh) — Real-Device Calibration & Field Testing Protocol

---

## 1. Physical Device Testing Matrix

To validate the Bluetooth 5.0 LE Coded PHY and legacy BLE discovery pipelines across real hardware, conduct testing using at least two physical Android devices across these calibrated distance tiers:

| Distance | Expected Tier | Target Detection Rate | Notification Policy |
| :--- | :--- | :--- | :--- |
| **5 metres** | `VERY_CLOSE` | > 95% | Vibrate / Push Notification Triggered |
| **10 metres** | `VERY_CLOSE` | > 90% | Vibrate / Push Notification Triggered |
| **25 metres** | `NEARBY` | > 85% | Vibrate / Push Notification Triggered |
| **40 metres** | `EXTENDED` | > 75% | **Silent**: Appears in list only, NO vibration |
| **50 metres** | `EXTENDED` | > 65% (BT5 Coded PHY) | **Silent**: Appears in list only, NO vibration |
| **60 metres+** | `EXTENDED` | Opportunistic | **Silent**: Appears in list only, NO vibration |

---

## 2. Test Environments

Repeat measurements under 4 physical conditions:
1. **Open Space (LOS - Line of Sight)**: Campus field, open quadrangle, park.
2. **Indoor Corridors**: University hallway or office corridor with partial drywall reflection.
3. **Crowded Indoor Zone**: Cafeteria, food court, or library with body shielding (phones in pockets/backpacks).
4. **Partition / Wall Attenuation**: Adjoining rooms separated by drywall or glass doors.

---

## 3. Core Acceptance Criteria

- **No False Distance Claims**: UI presents calibrated categories (`"Very Close"`, `"Nearby"`, `"Extended Range"`), avoiding speculative exact meter claims.
- **Absence Timeout Verification**: When Device B powers down or moves out of range, Device A's list must automatically drop Device B after **20s** (foreground) or **35s** (screen-off) without flickering.
- **Reboot Invalidation**: All `elapsedRealtime()` timestamps are maintained strictly in memory; any device reboot automatically resets the active encounters cache to prevent monotonic clock collision.
- **Privacy Enforcement**: Network inspection of the `POST /api/v1/radar/resolve-ble-token` request must confirm that `instagram_username` is **never transmitted** before mutual connection acceptance.
- **Battery Benchmark**: Measure battery drain using Android Studio Power Profiler over a 2-hour active radar session; target consumption < 1.5% per hour during active foreground scanning.
