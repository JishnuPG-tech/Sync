# AuraRadar Android App
## Complete UI/UX Design System and Jetpack Compose Architecture

**Product type:** Opt-in nearby social discovery with mutual-consent Instagram profile reveal  
**Platform:** Android, Kotlin, Jetpack Compose, Material 3  
**Design direction:** Claude-inspired warm editorial minimalism, not an Instagram clone  
**Core principle:** People can discover permitted profile details nearby. Instagram usernames remain private until both people accept a connection.

---

## 1. Product and design goals

AuraRadar should feel calm, premium, recognisable, privacy-first, and fast. It should avoid looking like a generic admin dashboard or a direct Instagram copy.

### UX goals

1. A new user can set up a profile without a long form.
2. The Radar screen immediately explains whether discovery is active.
3. Nearby users appear only when the backend confirms eligibility and their short-lived discovery presence is valid.
4. Only permitted profile details appear before connection acceptance.
5. The app makes it easy to stop discovery, block someone, report abuse, and delete an account.
6. Light and dark themes have equal design quality.
7. BLE and backend failures are explained honestly. Never show fabricated nearby users or a fake precise distance.
8. Battery-heavy extended range is an explicit choice, not the default.

### Non-goals for the first release

- Do not implement chat or direct messages unless they become a separately scoped feature.
- Do not scrape Instagram or promise automatic follows.
- Do not display a live map, exact coordinates, routes, home address, or a person's location history.
- Do not infer exact metres from BLE RSSI.
- Do not let users select a remote place to appear physically nearby.

---

## 2. Visual identity: Claude-inspired, AuraRadar-owned

Use the visual language of warm paper, ink-like text, subtle dividers, restrained terracotta, generous whitespace, and careful typography. AuraRadar needs its own wordmark and radar symbol. Do not use Claude's or Instagram's logo as the app icon.

### 2.1 Colour tokens

Use named tokens in code. Never scatter raw hex values across composables.

#### Light theme

| Token | Hex | Usage |
|---|---|---|
| `canvas` | `#FAF9F5` | App background, warm paper |
| `surface` | `#FFFFFF` | Main cards, sheets, text fields |
| `surfaceSubtle` | `#F2F0E8` | Secondary sections, selected chips |
| `surfaceElevated` | `#FFFFFF` | Menus and modal surfaces |
| `ink` | `#24231F` | Primary text and icons |
| `inkSecondary` | `#68655D` | Supporting text |
| `inkMuted` | `#89857B` | Hints and metadata, only when contrast passes |
| `border` | `#E5E1D8` | Card outlines and dividers |
| `accent` | `#D97757` | Primary action and brand mark |
| `accentHover` | `#BD6246` | Pressed/darker accent state |
| `accentSubtle` | `#F4E5DD` | Selected states and icon containers |
| `success` | `#367653` | Active/success confirmation |
| `warning` | `#94621D` | Battery caution or degraded state |
| `danger` | `#B5473A` | Block/delete/error actions |
| `focusRing` | `#8B5E4B` | Keyboard/accessibility focus |

#### Dark theme

| Token | Hex | Usage |
|---|---|---|
| `canvas` | `#191918` | Main background, warm charcoal |
| `surface` | `#222220` | Cards and list rows |
| `surfaceSubtle` | `#2B2A27` | Secondary panels and selected surfaces |
| `surfaceElevated` | `#30302C` | Dialogs, menus, bottom sheets |
| `ink` | `#F3F0E8` | Primary text |
| `inkSecondary` | `#C3BFB5` | Supporting text |
| `inkMuted` | `#A19C91` | Metadata and helper text, verify contrast |
| `border` | `#3B3934` | Dividers and outlines |
| `accent` | `#E28A6A` | Primary action with better dark-surface contrast |
| `accentHover` | `#F0A184` | Pressed/hover state |
| `accentSubtle` | `#3A2923` | Selected accent surface |
| `success` | `#85C59B` | Active/success confirmation |
| `warning` | `#E0B66E` | Battery caution or degraded state |
| `danger` | `#F08B7F` | Block/delete/error actions |
| `focusRing` | `#F0B49C` | Keyboard/accessibility focus |

All final colors must pass contrast tests. Do not communicate status by color alone. Pair status colours with text and icons.

### 2.2 Typography

The exact fonts used in Anthropic's products may be proprietary or served under separate webfont terms. Do not copy, extract, or package those fonts into the APK without a valid licence.

**Recommended shippable approximation:**
- **UI/body:** Inter, weights 400, 500, 600, 700, under its applicable open-source licence.
- **Editorial display headings:** Source Serif 4, regular or semibold, under its applicable open-source licence.
- **Fallback:** Android system sans-serif and serif.
- Use the correct licence notices when bundling font files. Verify current font licences before release.
- If you already own a separate commercial licence for a Claude/Anthropic-associated font, use it only within the licence's permitted app embedding and distribution terms.

Use serif type sparingly: onboarding statement, empty-state title, and major section heading. Buttons, labels, navigation, status, profile names, forms, and body copy should use Inter for consistency and readability.

| Role | Family | Size | Weight | Line height |
|---|---|---:|---|---:|
| `displaySmall` | Source Serif 4 | 32sp | Regular | 38sp |
| `headlineLarge` | Source Serif 4 | 28sp | Regular | 34sp |
| `headlineMedium` | Source Serif 4 | 24sp | Regular | 30sp |
| `titleLarge` | Inter | 20sp | Semibold | 26sp |
| `titleMedium` | Inter | 16sp | Semibold | 22sp |
| `titleSmall` | Inter | 14sp | Semibold | 20sp |
| `bodyLarge` | Inter | 16sp | Regular | 24sp |
| `bodyMedium` | Inter | 14sp | Regular | 20sp |
| `bodySmall` | Inter | 12sp | Regular | 16sp |
| `labelLarge` | Inter | 14sp | Semibold | 20sp |
| `labelMedium` | Inter | 12sp | Medium | 16sp |
| `labelSmall` | Inter | 11sp | Medium | 14sp |

Do not hardcode typefaces inside individual screens. All text styles must come from `MaterialTheme.typography` or a named app typography extension.

### 2.3 Spacing, shapes, and surfaces

Use a 4dp spacing base.

- Screen horizontal padding: 20dp on phones; 24dp on wider layouts.
- Section spacing: 24dp.
- Card internal padding: 16dp.
- Tight component spacing: 8dp.
- Standard component gap: 12dp.
- Primary button height: 52dp.
- Minimum interactive target: 48dp.
- Text field height: 54 to 58dp, with helper/error text below.
- Standard card radius: 18dp.
- Main modal/sheet radius: 24dp at top corners.
- Chip radius: pill shape.
- Avatars: 44dp compact, 56dp standard, 88dp profile.
- Dividers: 1dp; use sparingly.
- Avoid heavy shadows. Prefer surface contrast and subtle borders.
- Support Android font scaling and display-size changes without clipping.

### 2.4 Icon system

Use one consistent outlined icon family. Recommended: Material Symbols Rounded or Material Icons Extended in Compose, mapped through a central `AuraIcon` wrapper. Do not mix filled emoji, random SVG packs, and different stroke weights.

| Meaning | Suggested icon |
|---|---|
| Radar/discovery | `Radar` or custom vector radar symbol |
| Nearby people | `Groups` / `PersonSearch` |
| Requests | `PersonAddAlt1` / `Handshake` if available |
| Connections | `PeopleAlt` / `Link` |
| Profile | `Person` |
| Edit | `Edit` |
| Location permission | `LocationOn` |
| Bluetooth | `BluetoothSearching` |
| Notifications | `NotificationsNone` |
| Privacy | `Shield` / `VerifiedUser` |
| Block | `Block` |
| Report | `Flag` |
| Battery | `BatteryChargingFull` / `BatteryAlert` |
| Thermal warning | `DeviceThermostat` |
| Settings | `Settings` |
| Appearance | `Contrast` / `DarkMode` |
| Help | `HelpOutline` |
| External Instagram link | `OpenInNew` beside a clearly labelled “Open Instagram” action |
| Error | `ErrorOutline` |
| Offline | `CloudOff` |
| Empty state | `Radar` or `PeopleOutline` |

Keep common icons 24dp, small inline status icons 16dp, and prominent empty-state illustrations 48 to 64dp. Provide content descriptions for meaningful icons. Do not add content descriptions to decorative icons that are already explained by adjacent text.

---

## 3. App navigation and screen inventory

Use four primary destinations in a Material 3 bottom navigation bar:

1. **Radar**
2. **Requests** (with a small count badge for pending requests)
3. **Connections**
4. **Profile**

Nearby People is a section within Radar, not a fifth tab. Settings is reached from Profile. This keeps the navigation concise.

### Screen list

| ID | Screen | Required |
|---|---|---|
| S01 | Splash / session restore | Yes |
| S02 | Welcome | First launch |
| S03 | Sign up / sign in | If account auth is enabled |
| S04 | Profile setup | First run; mandatory minimal fields |
| S05 | Discovery explanation and permissions | Before enabling detection |
| S06 | Radar home | Yes |
| S07 | Nearby person preview | Yes |
| S08 | Connection request compose/confirmation | Yes |
| S09 | Requests inbox | Yes |
| S10 | Request detail | Yes |
| S11 | Connections list | Yes |
| S12 | Connection detail / reveal | Yes |
| S13 | My profile | Yes |
| S14 | Edit profile | Yes |
| S15 | Discovery location and zone options | Yes, without remote spoofing |
| S16 | Radar mode and power settings | Yes |
| S17 | Privacy and safety | Yes |
| S18 | Blocked accounts | Yes |
| S19 | Notifications settings | Yes |
| S20 | Appearance settings | Yes |
| S21 | App settings and about | Yes |
| S22 | Help and policy links | Yes |
| S23 | Report user | Yes |
| S24 | Account deletion confirmation flow | Yes |
| S25 | Permission recovery / system setting guidance | Yes |
| S26 | Offline / backend error state | Yes, shared state |
| S27 | Empty states | Yes, shared state variants |
| S28 | Loading / reconnecting states | Yes, shared state |
| S29 | Instagram open fallback sheet | Yes |
| S30 | Generic confirmation / destructive-action dialogs | Yes, shared components |

Do not create 30 separate navigation destinations just because the inventory lists 30 screens. Dialogs, empty states, and error states should be reusable UI states within parent destinations.

---

## 4. Detailed screen specifications

### S01. Splash and session restore

**Purpose:** Restore the user's session and decide where to navigate.

**Layout:** Centered AuraRadar symbol; wordmark; short subtle fade/scale transition; no marketing content. Keep the screen brief.

**Logic:**
- No valid session → Welcome.
- Signed in but no profile → Profile Setup.
- Profile complete but permissions not considered → Discovery Explanation.
- Ready account → Radar Home.
- Backend unavailable → show offline/retry state without deleting local profile data.

**Do not:** Ask for permissions on splash or show fake nearby counts.

### S02. Welcome

**Purpose:** Explain the service in one short statement.

**Layout:** Wordmark at top; simple radar illustration; editorial heading “Meet nearby. Connect by choice.”; one-line explanation; primary “Get started”; secondary “I already have an account”; privacy note.

**Actions:** Get started, sign in, privacy details.

**Motion:** One slow radar sweep while visible. Respect reduced-motion preferences. Stop animation when app is backgrounded.

### S03. Sign up / sign in

Use the existing authentication provider. Do not invent a local-only sign-in if backend identity is required.

**Layout:** Back navigation; title; fields or supported sign-in method; primary action; validation message near the relevant field; Terms and Privacy links.

**States:** Idle, validating, submitting, invalid input, authentication failure, offline, success.

**Rules:** Disable duplicate submissions. Do not log passwords, tokens, or sensitive authentication responses.

### S04. Profile Setup

**Required fields:**
- Display name, recommended 2 to 32 characters.
- Instagram username, normalized by removing an optional leading `@`, validating allowed syntax, and storing a canonical form.

**Optional fields:**
- Profile photo or generated initials avatar.
- One-line introduction, max 60 characters, optional.

**Do not include:** Interests, college, community, birthday, gender, or public address by default.

**Layout:** Profile image/avatar control; display-name field; Instagram username field with a lock icon and helper copy; optional one-line introduction; short privacy note; primary “Save and continue”.

**Rules:**
- Tell the user that the display name and any enabled photo are shown to eligible nearby users.
- The Instagram username is private until mutual acceptance.
- Do not import Instagram photos without consent or unauthorised access.
- Support incomplete-save and retry.
- Editing the username later must not bypass backend privacy checks.
- The server, not only the UI, validates ownership/connection permissions.

### S05. Discovery explanation and permissions

**Purpose:** Explain requirements before Android shows system prompts.

**Cards:**
- Bluetooth is used to discover participating AuraRadar devices nearby.
- Nearby-device permissions are required where the Android version requires them.
- Notifications are optional but required for request alerts.
- Some Android versions/devices restrict background discovery.

**Actions:** “Enable discovery”, “Not now”, “Why is this needed?”.

Request permissions just in time, with separate explanations. Never force the user to allow every permission to view their profile or settings.

### S06. Radar Home

**Primary screen and strongest visual signature.**

**Top area:** AuraRadar wordmark; status pill (`Radar on`, `Paused`, `Needs permission`, `Offline`); settings or help shortcut only if needed.

**Hero:** Radar graphic with restrained sweep. Centered status text such as:
- “Radar is on”
- “Looking for opted-in people nearby”
- “Discovery paused”
- “Bluetooth access needed”

**Power mode control:** segmented choices `Smart`, `Extended`, `Off`. Show a small “Higher battery use” note when Extended is selected. Off must stop scanning and advertising.

**Nearby section:** heading “People nearby”; count only when based on current valid results; list/cards ordered by recent confidence and freshness, not by inferred exact distance.

**Person card:** avatar/initials; display name; optional introduction; label `Very close`, `Nearby`, or `Extended range`; `Instagram hidden` status; Connect button. Use the same rank/order consistently.

**Safety footer:** “Only opted-in AuraRadar users appear here.” Tap to view privacy controls.

**Empty states:**
- No peers found: “No one nearby yet” and explanation that both people must have Radar enabled.
- Bluetooth off: recovery CTA.
- Permission missing: open system settings guidance.
- Offline: cached encounters may be shown only if still valid and clearly marked; otherwise show retry.
- Radar paused: show explicit Resume button.

**Performance:** Animate radar only while the screen is visible and discovery is active. Throttle UI updates; never recompose the entire list for every raw RSSI packet.

### S07. Nearby Person Preview

**Purpose:** Show the small amount of profile information that is allowed before a connection.

**Content:** avatar/initials; display name; optional one-line intro; range-confidence label; freshness status; `Instagram profile hidden`; Connect; Ignore; overflow menu with Block and Report.

**Do not show:** Instagram handle, precise distance, map, coordinates, live movement, previous encounters, or contact details.

**Rules:** The backend must return a safe profile DTO which has no Instagram username field. Do not fetch the hidden handle and merely obscure it in the UI.

### S08. Connection request confirmation

**Purpose:** Prevent accidental requests and give an understandable result.

A lightweight confirmation bottom sheet is enough. Show the display name and “Send a request to connect with Arun K?” Actions: “Send request” and “Cancel”.

After sending, show “Request sent” and disable the action until the server returns a state change. Idempotency must prevent duplicates from rapid taps/retries.

### S09. Requests Inbox

**Structure:** segmented `Received` and `Sent` tabs; small count badge for pending incoming requests; stable loading skeletons.

**Received row:** avatar; display name; request time if appropriate; Accept and Decline quick actions; overflow Block/Report.

**Sent row:** display name; Pending/Accepted/Declined/Expired status; Cancel request if supported.

**Empty states:** “No requests yet” / “You haven’t sent any requests”.

**Security:** Incoming notifications should not include an Instagram username. Display only permitted profile details.

### S10. Request Detail

**Content:** requester display name; permitted avatar/intro; short explanation that Instagram details stay hidden until mutual acceptance; Accept; Decline; Block; Report.

**After Accept:** call backend; update state only after confirmed response. If the other user has blocked the requester or the request expired, show a clear non-blaming message. Do not reveal the username until the backend confirms mutual acceptance.

### S11. Connections List

**Structure:** heading “Connections”; search/filter only if it is useful at realistic list size; list of mutually accepted users.

**Row:** avatar; display name; state `Connected`; optional last connection date if needed; Open profile action.

**Empty state:** “Connections appear here after you both accept.”

**Privacy:** If one user revokes the connection or blocks the other, remove access according to server rules and update cached details.

### S12. Connection Detail / Instagram Reveal

**Content:** avatar; display name; connection state; Instagram username only after server-confirmed mutual acceptance; button “Open Instagram”; button “Remove connection”; Block and Report in overflow.

**Instagram launch:** Attempt the supported app/web link using safe URI handling. Do not promise that an app always opens directly. If unavailable, show “Open in browser” and “Copy username” only when copy is permitted by product policy.

**If consent is revoked or connection is invalid:** remove the handle from UI state and cached copies as far as practical; show the current connection status.

### S13. My Profile

**Layout:** profile image/initial avatar; display name; optional introduction; a privacy explanation for the Instagram field (do not display the handle publicly on this screen if you want the privacy model to stay consistent); Edit profile action; discovery status; settings rows.

**Quick actions:** Edit profile, Discovery controls, Privacy and safety, Appearance.

Do not include follower counts or make follower accumulation the central metric. This app facilitates mutual discovery, not follower guarantees.

### S14. Edit Profile

Use the same reusable fields as Profile Setup, prefilled from the local/server state.

**Fields:** display name, Instagram username, optional image/avatar, optional one-line introduction.

**Actions:** Save changes, Cancel.

**States:** pristine, dirty, saving, saved, validation error, network error. Confirm before discarding unsaved changes. Preserve inputs on failure.

### S15. Discovery Location and Zone Options

Do not ask users to type a home address. Do not treat a manually selected location as proof that the user is there.

**Sections:**
- Use nearby discovery (requires permitted device discovery signal).
- Optional zone filter for an event/campus if the product supports vetted zones.
- Explain that selected zones are filters, not a way to spoof presence.
- Location permission status and link to Android settings, only if location is actually required by the implemented method.

If using BLE-only proximity, do not request location permission without a documented platform/API reason. Keep the permission set minimal and aligned with the exact Android versions and BLE APIs used.

**Privacy copy:** “Your exact coordinates are not shown to nearby users.” If location data is sent to the backend for matching, state that clearly and provide retention and deletion details.

### S16. Radar Mode and Power Settings

**Modes:**
- Smart Discovery (default): balanced, adaptive power profile.
- Extended Range: higher battery use; use only when requested.
- Off: stops discovery.

**Show:** current mode; Bluetooth state; permission status; battery/thermal adjustment status; short explanation of changes.

**Important:** Do not promise “zero drain” or “no heating”. State that Extended Range may use more battery and the phone may reduce discovery under thermal stress or low battery.

**Actions:** choose mode; resume Radar; open Android Bluetooth settings when needed.

### S17. Privacy and Safety

**Controls:** pause/hide discovery; allow or disable incoming connection requests where supported; photo visibility; introduction visibility; block list; report help; account-data and retention explanation; delete account.

**Rules:** Visibility changes should apply immediately on the backend or clearly show a pending state. Do not let the UI claim that a user is hidden until the backend/device state confirms it.

### S18. Blocked Accounts

**Layout:** list blocked profiles with display name and avatar/initials; “Unblock” action; explanatory helper text.

**Rules:** Blocking must suppress future BLE token resolution/profile display, connection requests, and relevant notifications. Do not reveal one user's hidden profile data to the other during unblock.

### S19. Notification Settings

**Controls:** connection requests, accepted connections, nearby encounter alerts, discovery availability. Nearby alerts should be limited and respect the range tiers.

**Copy:** Explain Android notification permission separately. Turning app notifications off should not silently activate or deactivate Radar.

### S20. Appearance Settings

**Options:** `System default`, `Light`, `Dark`.

Store the choice in DataStore. Apply the theme app-wide without resetting navigation or form state. Preview changes immediately. Ensure screenshots and status/navigation bars follow the active theme.

### S21. App Settings and About

Rows: Appearance; Notifications; Radar mode; Privacy and safety; Help; Terms; Privacy policy; open-source licences; app version/build; diagnostic export if designed with privacy safeguards.

Do not expose internal tokens, BLE identifiers, or raw API logs in a user-visible diagnostics screen.

### S22. Help and Policy

Provide concise explanations for:
- How nearby discovery works.
- Why a person may not appear.
- Why range labels are estimates.
- How mutual acceptance protects Instagram details.
- How to disable discovery.
- How to report or block a user.
- Battery and background-operation limitations.
- Privacy policy, terms, support channel.

### S23. Report User

**Fields:** required report reason (harassment, impersonation, spam, unsafe behaviour, other); optional description with sensible maximum length; submit; cancel.

**After submission:** confirmation with request reference if supported. Offer to block immediately. Do not send a report target's hidden Instagram username unless it is essential and properly authorised for moderation.

### S24. Account Deletion

**Flow:** explain what is deleted and what may be retained for safety/legal obligations; ask for confirmation; require reauthentication if appropriate; show progress; finish with a clear result.

Backend deletion must cover profile data, active location/presence, tokens, connection data as required by policy, and user-linked cached data. Do not claim immediate full deletion if a queued process is used; show the actual status.

### S25. Permission Recovery

Shared screen/bottom sheet for Bluetooth disabled, nearby-devices permission denied, notifications denied, background restrictions, or OS-level feature unavailability.

Show:
- What is unavailable.
- Why the permission/capability is required for that feature.
- A direct, user-triggered action such as “Open settings”.
- “Continue without this feature” where feasible.

Never launch repeated permission prompts or settings pages without user action.

### S26. Offline and Backend Error States

Use inline banners where possible instead of full-screen blocking errors.

Examples: “You’re offline. Nearby profile refresh is paused.” “Couldn’t send request. Try again.” “Your request may already have been sent. Refreshing status…”

Do not show stale peer data as currently nearby if its expiry/last-seen time has passed.

### S27. Empty States

Create a reusable `AuraEmptyState` with icon/illustration, concise heading, one supporting sentence, and one optional action.

Variants: no nearby people, no received requests, no sent requests, no connections, no blocked users, no notifications.

### S28. Loading / Reconnecting

Use stable skeleton rows, not a full-screen spinner for every request. Prevent layout jumps. Set timeouts and retry options. Cancel obsolete work when navigating away.

### S29. Instagram Open Fallback

Use a bottom sheet with “Open Instagram”, “Open in browser”, and Cancel as appropriate. The username must already be authorised for the connected user. Do not expose a private handle through analytics, crash reports, notification payloads, or share previews.

### S30. Shared confirmation dialogs

Create reusable components for:
- Ignore encounter.
- Decline request.
- Cancel sent request.
- Remove connection.
- Block user.
- Unblock user.
- Sign out.
- Discard profile changes.
- Delete account.

Use clear action verbs and destructive styling only for destructive actions. Do not over-confirm reversible, low-risk actions.

---

## 5. Layout and component library

Build a small design-system layer before screen implementation.

### Core composables

- `AuraRadarApp`
- `AuraTheme`
- `AuraScaffold`
- `AuraTopBar`
- `AuraBottomBar`
- `AuraPrimaryButton`
- `AuraSecondaryButton`
- `AuraTextButton`
- `AuraTextField`
- `AuraPasswordField` if auth needs it
- `AuraProfileAvatar`
- `AuraInitialsAvatar`
- `AuraPersonCard`
- `AuraRangeBadge`
- `AuraDiscoveryStatusPill`
- `AuraPowerModeSelector`
- `AuraPermissionCard`
- `AuraPrivacyNotice`
- `AuraSectionHeader`
- `AuraSettingsRow`
- `AuraEmptyState`
- `AuraErrorBanner`
- `AuraLoadingSkeleton`
- `AuraConfirmDialog`
- `AuraBottomSheet`
- `AuraInlineStatus`

Every component should read colours, typography, shapes, and spacing from design tokens. Do not set screen-specific hex values inline.

### Button variants

- Primary: accent filled, one prominent per main screen.
- Secondary: outlined or neutral surface.
- Text: low emphasis.
- Destructive: danger colour; use for Block/Delete only.
- Icon button: minimum 48dp hit target, content description.

### Profile card anatomy

1. Avatar/initials.
2. Display name.
3. Optional intro, one or two lines max.
4. Signal confidence badge (not an exact distance).
5. Privacy label where relevant.
6. Connect action.

Do not put more than one prominent action in every card. Keep Ignore and Block in secondary menus or detail screens to reduce accidental taps.

---

## 6. Motion and tactile interaction

Keep motion subtle and functional.

- Screen transition: 180–240ms fade/slide where appropriate.
- Button press: 100–140ms tonal/scale feedback; avoid bounce.
- Bottom sheet: platform-consistent spring, no excessive overshoot.
- Radar sweep: restrained and visible only while Radar is on and screen is foregrounded.
- Connection success: one short opacity/scale animation, optional haptic.
- List updates: animate insertion/removal only for meaningful peer state changes, not every RSSI packet.
- Theme switch: no full activity restart and no loss of screen state.
- Respect system reduced-motion settings where available.

Do not keep animations running in a foreground service or background.

---

## 7. Responsive design and accessibility

- Support compact, medium, and expanded widths. On tablets/foldables, keep form content within a readable max width and use side-by-side panels only where it improves the flow.
- Support system font scaling without truncating button labels or profile names.
- Provide at least 48dp touch targets.
- Ensure all fields have labels, not placeholders alone.
- Preserve keyboard navigation and visible focus.
- Test TalkBack order, semantic labels, and status announcements.
- Never communicate `Radar on`, `Extended range`, or `Blocked` using colour alone.
- Do not require photos. Use initial avatars gracefully.
- Test contrast in both themes and with high-contrast/large-font system settings.

---

## 8. Kotlin / Jetpack Compose architecture

Use a layered architecture with unidirectional data flow. Do not place BLE scanning, networking, or privacy rules inside composables.

### Suggested package layout

```text
app/
  src/main/java/com/auraradar/
    AuraRadarApplication.kt
    MainActivity.kt
    navigation/
      AuraNavHost.kt
      Routes.kt
      NavigationEvents.kt
    core/
      common/
      time/
      logging/
      result/
      permissions/
      design/
        AuraTheme.kt
        ColorTokens.kt
        Type.kt
        ShapeTokens.kt
        Spacing.kt
        components/
    data/
      auth/
      profile/
      discovery/
      connections/
      settings/
      remote/
      local/
    domain/
      model/
      repository/
      usecase/
    feature/
      onboarding/
      auth/
      profile/
      radar/
      nearby/
      requests/
      connections/
      privacy/
      settings/
      help/
    ble/
      BleDiscoveryManager.kt
      AdvertisingStrategy.kt
      ScanStrategy.kt
      BleCapabilities.kt
      ProximityEvaluator.kt
      ThermalAndBatteryController.kt
      DiscoverySessionService.kt
    workers/
      PresenceRefreshWorker.kt
      ExpiredDataCleanupWorker.kt
```

Package names can be adapted to the existing project. Keep the responsibilities and dependency direction.

### Layer responsibilities

**UI / feature layer**
- Compose screens and reusable components.
- ViewModels expose `StateFlow<UiState>`.
- User events call use cases.
- No direct calls to BLE or Retrofit from composables.

**Domain layer**
- Models such as `PublicProfile`, `PrivateProfile`, `NearbyEncounter`, `ConnectionRequest`, `Connection`, `DiscoveryMode`, `PermissionState`.
- Use cases: `SaveProfile`, `ObserveNearbyPeers`, `SendConnectionRequest`, `AcceptConnectionRequest`, `DeclineConnectionRequest`, `ObserveConnections`, `SetDiscoveryMode`, `PauseDiscovery`, `BlockUser`, `ReportUser`, `DeleteAccount`.
- Privacy/consent rules are expressed in domain-level outcomes, but the server remains authoritative for access control.

**Data layer**
- Repository interfaces.
- API implementation.
- Local persistence and DataStore settings.
- Profile cache with expiry and invalidation rules.
- No private profile reveal solely because a client cache contains a handle.

**BLE / platform layer**
- Owns advertiser/scanner lifecycle, capability checks, callbacks, retry/fallback state, observation timestamps, and Android permission errors.
- Emits minimal opaque peer-token observations, not raw identity data.
- No direct navigation or UI mutation.

### Suggested ViewModels

- `OnboardingViewModel`
- `ProfileViewModel`
- `RadarViewModel`
- `RequestsViewModel`
- `ConnectionsViewModel`
- `PrivacyViewModel`
- `SettingsViewModel`

Avoid one giant `MainViewModel` for the whole app.

### State model example

```kotlin
sealed interface RadarUiState {
    data object Loading : RadarUiState
    data class Ready(
        val mode: DiscoveryMode,
        val discoveryStatus: DiscoveryStatus,
        val peers: List<NearbyPeerUiModel>,
        val banner: RadarBanner? = null,
        val powerWarning: PowerWarning? = null
    ) : RadarUiState
    data class RecoverableError(
        val reason: RadarErrorReason,
        val canRetry: Boolean
    ) : RadarUiState
}
```

The UI should derive state from the ViewModel. Avoid manually changing a badge to “Radar on” before the BLE session is actually active.

---

## 9. Recommended app data models

Use separate types for pre-connection and post-connection data so a private username cannot accidentally leak into nearby card rendering.

```kotlin
data class NearbyPeerUiModel(
    val encounterId: String,
    val displayName: String,
    val avatarUrl: String?,
    val introduction: String?,
    val confidenceTier: ConfidenceTier,
    val lastSeenElapsedMs: Long,
    val connectionState: NearbyConnectionState
)

data class ConnectedProfile(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val instagramUsername: String,
    val connectedAt: Instant
)

enum class ConfidenceTier { VERY_CLOSE, NEARBY, EXTENDED }
enum class DiscoveryMode { OFF, SMART, EXTENDED }
enum class DiscoveryStatus { OFF, STARTING, ACTIVE, PAUSED, NEEDS_PERMISSION, BLUETOOTH_OFF, DEGRADED }
```

Do not include the Instagram username in `NearbyPeerUiModel`. Only the server-confirmed connected-profile endpoint should provide that field.

A local monotonic timestamp should not be serialised as a server timestamp. Store it with the device boot context if it needs persistence, and invalidate it after reboot.

---

## 10. Backend and privacy contract

The backend must enforce the privacy model; client-side hiding is not enough.

### Endpoint responsibilities (illustrative names)

- `GET /me/profile` — own profile.
- `PATCH /me/profile` — update own profile.
- `POST /discovery/session/start` — enable a discovery session.
- `POST /discovery/session/stop` — stop and expire presence.
- `POST /discovery/resolve` — resolve an ephemeral discovery token into an allow-listed public profile summary.
- `GET /requests?direction=incoming|outgoing` — request lists.
- `POST /requests` — create an idempotent request.
- `POST /requests/{id}/accept` — accept; return a connection only when mutual acceptance is valid.
- `POST /requests/{id}/decline` — decline.
- `GET /connections` — mutually accepted connections only.
- `GET /connections/{id}/profile` — reveal authorised Instagram username only after server verification.
- `POST /users/{id}/block` — block, revoke discovery and invalidate relationship as defined.
- `POST /reports` — submit report.
- `DELETE /me` — begin or complete account deletion according to actual backend process.

Adapt routes to the existing backend rather than creating duplicate APIs unnecessarily.

### Server rules

- Authenticate every private API request.
- Use random, short-lived discovery identifiers. Do not put a permanent user ID or Instagram handle in BLE advertisements.
- Return allow-listed public profile DTOs for nearby requests.
- Enforce mutual acceptance transactionally on the server.
- Apply block lists before returning a peer profile or delivering notifications.
- Use idempotency for connection creation and notification processing.
- Apply cooldown and rate limits server-side.
- Expire stale presence and invalidate tokens.
- Keep notification payloads free of Instagram handles before mutual acceptance.
- Protect logs from storing tokens, exact coordinates, or private usernames unnecessarily.
- Define data retention and deletion behaviour clearly.

---

## 11. BLE state and power integration

UI must reflect the BLE engine's actual state. Keep the BLE session state machine separate from the Compose navigation state.

### Discovery state progression

`OFF → STARTING → ACTIVE → DEGRADED / PAUSED → STOPPING → OFF`

Reasons for `DEGRADED` or `PAUSED` can include Bluetooth disabled, permission missing, advertising/scanning unsupported, thermal downthrottle, low battery, radio start failure, or temporary OS restrictions.

### Power modes

- **Off:** no scanning or advertising, cancel foreground session if no longer required, release resources.
- **Smart:** default balanced profile. Use low-power or balanced radio settings after real-device calibration. Keep network lookup throttled and cache eligible profile summaries safely.
- **Extended:** explicit user choice, capability-check long-range PHY and use higher transmit power only where supported. Show “Uses more battery”.

Thermal controller and battery controller must emit observable state updates. Never repeatedly oscillate between Smart and Extended modes; use hysteresis and a recovery delay. The user should be told when the app has automatically reduced range.

Do not claim zero battery impact. Measure it on real devices.

---

## 12. Local persistence and cache invalidation

Use DataStore for settings such as theme, selected discovery mode, and notification preferences. Use Room only for durable local data that is genuinely needed offline.

Cache rules:
- A cache hit can avoid a profile HTTP request, but it does not guarantee zero cellular radio activity from the rest of the operating system.
- Cache only fields authorised for the current relationship state.
- Do not cache the hidden Instagram username in the nearby-profile cache.
- Expire peer data according to encounter freshness; never show an expired peer as currently nearby.
- Invalidate relationship-sensitive fields on connection revocation, block, sign-out, account deletion, or token expiry.
- Do not persist raw BLE identifiers longer than required.

---

## 13. Navigation, deep links, and app state

- Use Navigation Compose with typed routes if available in the project's dependency set.
- Preserve bottom navigation back stacks where practical.
- Handle notification intents by opening the relevant Request Detail or Connection Detail only after authentication and server-side authorisation.
- A notification tap must not expose a hidden Instagram username in a deep-link URI.
- Handle expired requests with an honest message and safe return route.
- Deep links should use internal opaque IDs, not private handles.
- Back navigation from Profile Setup should preserve entered details.

---

## 14. Analytics, logging, and diagnostics

Only collect analytics required to understand product performance, with appropriate disclosure and consent.

Safe useful metrics include:
- Discovery session started/stopped.
- Time to first eligible peer, aggregated.
- Successful request/acceptance counts.
- Detection success by anonymous test device and mode during opt-in testing.
- BLE start failure category.
- App crash and ANR rates.
- Battery/thermal test measurements in explicit QA sessions.

Avoid logging:
- Instagram handles in ordinary logs.
- Exact coordinates.
- Long-lived device identifiers.
- Raw discovery tokens.
- Personal profile text.
- Authentication secrets.

---

## 15. Testing and acceptance checklist

### UI consistency
- [ ] Every screen uses `AuraTheme` tokens.
- [ ] Light mode and dark mode are tested across every screen.
- [ ] No hardcoded per-screen hex colours remain.
- [ ] Icons use one family and consistent sizing.
- [ ] Typography scales correctly with Android accessibility settings.
- [ ] No clipped layouts on narrow devices or large fonts.
- [ ] Loading, empty, offline, error, and permission states exist.

### Profile and privacy
- [ ] Initial profile setup can be resumed if interrupted.
- [ ] Edit Profile prepopulates saved data.
- [ ] Instagram handle is absent from nearby DTOs and notification payloads.
- [ ] Mutual acceptance is verified on the server before handle reveal.
- [ ] Blocking prevents future profile resolution and requests.
- [ ] Account deletion clears/queues all required data as disclosed.

### BLE and power
- [ ] Radar Off stops scanning and advertising.
- [ ] Smart is the default mode.
- [ ] Extended mode shows a battery warning.
- [ ] Unsupported PHY and permission failures produce recoverable states.
- [ ] Repeated callback failures do not create duplicate scan/advertising sessions.
- [ ] Thermal downthrottling has hysteresis and recovery delay.
- [ ] Battery threshold logic measures actual percentage; `ACTION_BATTERY_LOW` is not treated as an exact 15% signal.
- [ ] No exact distance is inferred solely from RSSI.
- [ ] Presence expiry works when the screen is off and the process is recreated.
- [ ] Multi-device tests record detection rate, false-positive rate, detection latency, battery use, and thermal status.

### Backend and security
- [ ] Concurrent duplicate requests create only one logical request.
- [ ] Expired token cannot resolve a profile.
- [ ] Block is checked before profile response and push dispatch.
- [ ] Connection reveal endpoint fails closed if mutual acceptance is absent.
- [ ] Stale peers are not shown as present.
- [ ] Notification retries are idempotent.

---

## 16. Implementation milestones

### Milestone A: Design foundation
1. Create design tokens, type scale, icon wrapper, shapes, theme preference.
2. Implement all shared components.
3. Build static previews for light and dark mode.

### Milestone B: Onboarding and profile
1. Splash/session routing.
2. Welcome and auth.
3. Profile setup and editing.
4. Permission education and recovery states.

### Milestone C: Core social UI using mock data
1. Radar Home and nearby cards.
2. Requests inbox and request details.
3. Connections and connected profile reveal.
4. Profile, safety, settings, help, and delete-account flows.

### Milestone D: Backend integration
1. Connect repositories to actual backend endpoints.
2. Ensure nearby response DTO has no private Instagram username.
3. Implement server-confirmed mutual acceptance.
4. Test blocks, request races, expiration, and notification redelivery.

### Milestone E: BLE and power integration
1. Connect Radar ViewModel to the BLE session manager.
2. Surface true engine states in the UI.
3. Add Smart/Extended/Off controls.
4. Add thermal and low-battery policies.
5. Run multi-device field tests.

### Milestone F: Release quality
1. Accessibility test.
2. Light/dark screenshot regression.
3. Privacy/security review.
4. Battery and thermal benchmarking.
5. Play distribution policy review for any foreground service or background discovery feature.

---

## 17. Final implementation rule set

1. Use Kotlin + Jetpack Compose + Material 3.
2. Use Claude-inspired warm minimalism, but maintain AuraRadar's own brand identity.
3. Use Inter for UI and Source Serif 4 for selected editorial headings unless a properly licensed font is provided.
4. Use one professional icon family, preferably Material Symbols Rounded.
5. Build for light, dark, and system theme selection from the start.
6. Make Profile Setup and Edit Profile share a reusable form implementation.
7. Never show an exact distance inferred from RSSI.
8. Never reveal an Instagram username before server-confirmed mutual acceptance.
9. Keep BLE logic out of composables and keep UI state out of the radio manager.
10. Do not claim battery, range, privacy, or verification guarantees until they are tested.
11. Build all error, permission, offline, loading, and empty states as part of the feature, not as later polish.
12. Keep the interface calm and uncluttered. Every screen should have one clear primary action.

**Definition of done:** Every listed screen and state is navigable, responsive, accessible, works in light and dark themes, reads real ViewModel state, and enforces the privacy contract at the backend. BLE/range/battery claims are supported by physical-device test results.

---

## 18. Technical references

- Jetpack Compose theming and Material 3 typography: https://codelabs.developers.google.com/jetpack-compose-theming
- Jetpack Compose custom/downloadable font guidance: https://developer.android.com/develop/ui/compose/text/fonts
- Android BLE advertiser API: https://developer.android.com/reference/android/bluetooth/le/BluetoothLeAdvertiser
- Android advertising parameters: https://developer.android.com/reference/android/bluetooth/le/AdvertisingSetParameters
- Android BLE scanning API: https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner
- Android SystemClock: https://developer.android.com/reference/android/os/SystemClock
- Android thermal status API: https://developer.android.com/reference/android/os/PowerManager
