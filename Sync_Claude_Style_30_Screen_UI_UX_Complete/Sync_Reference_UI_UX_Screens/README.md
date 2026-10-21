# Sync — Complete UI/UX Screens (Reference-Matched)

This pack recreates the 30-screen flow from the supplied UI board as individual, full-screen Android mockups. It includes light and dark variants for every screen plus a mixed-theme ReferenceFlow folder that follows the visual rhythm of the supplied board.

## Contents
- `Light/`: 30 individually rendered 1080 × 2400 PNG screens.
- `Dark/`: 30 individually rendered 1080 × 2400 PNG screens.
- `ReferenceFlow/`: the 30 primary screens using the mixed light/dark presentation style of the supplied reference board.
- `States/`: separate screens for no people nearby, no requests, no connections, loading/reconnecting, and offline/error, in both themes.
- `Assets/Portraits/`: generated sample portrait assets used in mockups.
- `Previews/contact_sheet_light.jpg`, `contact_sheet_dark.jpg`, and `reference_flow_board.jpg`.
- `screen_index.csv`: screen names and matching file paths.

## Screen sequence
01 Splash; 02 Welcome/Intro; 03 Feature Highlights; 04 Permissions; 05 Create Account; 06 Verify Instagram; 07 Profile Setup; 08 Add Profile Photo (optional); 09 Choose Discovery Mode; 10 Radar Home; 11 Radar Scanning; 12 Nearby People List; 13 Person Preview (before connection); 14 Send Connection Request; 15 Incoming Connection Request; 16 Request Sent; 17 Mutual Connection; 18 Chat; 19 Connections List; 20 Connection Profile; 21 My Profile; 22 Edit Profile; 23 Discovery Settings; 24 Privacy Settings; 25 Notifications; 26 Appearance; 27 Help & Support; 28 About; 29 Delete Account; 30 Loading & Empty States.

The supplied board included an Interests screen, but this pack replaces it with an optional profile-photo screen to respect the previously stated requirement that interests and college/community details are not needed. Profile setup is limited to display name, private Instagram username, optional profile photo, and an optional short introduction.

## Design language
- Warm light canvas: `#FAF9F5`.
- Dark canvas: `#141413`.
- Clay/terracotta orange: `#D97757`.
- Warm secondary surface: `#F0EEE6`.
- Main dark-mode surface: `#1F1E1D`.
- Interface text uses Inter; editorial headings use EB Garamond. These are available licensed alternatives for this mockup pack, not Anthropic's proprietary typefaces. The exact Anthropic Sans/Serif font files are not included because they are proprietary and should not be redistributed without permission.

## Branding
The pack uses `Sync` as the working app label from the latest project-name discussion. The images are visual design references only, not a functioning APK. Replace sample names, photos, and data with authorized data in the Android implementation.

## Privacy and implementation reminders
- Keep Instagram usernames out of nearby-profile responses and push payloads before mutual acceptance.
- Show exact coordinates to no other user.
- Treat BLE range labels as signal-confidence categories, not guaranteed distances.
- Implement all forms, permissions, navigation, persistence, backend requests, BLE scanning, battery/thermal controls, and accessibility behaviours in Jetpack Compose/Kotlin. Raster images are references, not an interactive UI implementation.
