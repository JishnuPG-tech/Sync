import re

with open("sync-app.html", "r", encoding="utf-8") as f:
    html = f.read()

# 1. Update dimensions to 16.74 cm (6.6-inch modern display)
html = html.replace(
    """      /* Universal Device Dimensions (Standard Android 412x892 & iPhone 393x852) */
      --phone-max-w: 412px;
      --phone-h: min(880px, 94vh);""",
    """      /* Universal Physical Device Dimensions (16.74 cm / 6.6-inch modern display, 19.5:9 aspect) */
      --phone-w: 395px;
      --phone-max-w: 395px;
      --phone-h: min(855px, 92vh);"""
)

# Also update companion toolbar title to reflect 16.74 cm
html = html.replace(
    """      <div class="toolbar-brand">
        <span class="toolbar-logo">Sync</span>
        <span class="toolbar-badge">PROTOTYPE</span>
      </div>""",
    """      <div class="toolbar-brand">
        <span class="toolbar-logo">Sync</span>
        <span class="toolbar-badge">16.74 cm (6.6") Mobile Display</span>
      </div>"""
)

# 2. Fix the layout bug so Bottom Nav is permanently docked and visible!
html = html.replace(
    """    /* SCROLLABLE MAIN BODY */
    .tab-content-container {
      flex: 1;
      overflow-y: auto;
      -webkit-overflow-scrolling: touch;
      padding-bottom: 70px; /* Clearance for bottom navigation bar */
    }

    /* BOTTOM NAVIGATION BAR (3 Core Tabs) */
    .app-bottom-nav {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 58px;
      background: var(--surface);
      border-top: 1px solid var(--line);
      display: flex;
      align-items: center;
      justify-content: space-around;
      padding: 0 8px;
      z-index: 50;
      box-shadow: 0 -3px 12px rgba(0,0,0,0.03);
    }""",
    """    /* SCREEN MAIN: Lock viewport so only tab content scrolls */
    #screen-main {
      overflow: hidden !important;
    }

    .app-main-layout {
      display: flex;
      flex-direction: column;
      height: 100%;
      width: 100%;
      position: relative;
      overflow: hidden;
    }

    /* SCROLLABLE MAIN BODY */
    .tab-content-container {
      flex: 1 1 0;
      min-height: 0;
      overflow-y: auto;
      -webkit-overflow-scrolling: touch;
      padding-bottom: 20px;
    }

    /* BOTTOM NAVIGATION BAR (Permanently Docked, Zero Shift) */
    .app-bottom-nav {
      flex: 0 0 60px;
      height: 60px;
      position: relative;
      bottom: auto;
      left: auto;
      right: auto;
      background: var(--surface);
      border-top: 1px solid var(--line);
      display: flex;
      align-items: center;
      justify-content: space-around;
      padding: 0 8px 4px 8px;
      z-index: 50;
      box-shadow: 0 -3px 12px rgba(0,0,0,0.04);
    }"""
)

# 3. Remove "Radar active" text from top bar and replace with minimal pulse beacon
html = html.replace(
    """              <div class="radar-status-pill">
                <span class="pulse-dot"></span>
                <span id="topRadarLabel">Radar active</span>
              </div>""",
    """              <div class="radar-beacon-pill" title="Proximity Radar Active">
                <span class="pulse-dot"></span>
              </div>"""
)

# Replace radar-status-pill in CSS with radar-beacon-pill
html = html.replace(
    """    .radar-status-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: var(--green-subtle);
      border: 1px solid rgba(107, 140, 114, 0.35);
      padding: 4px 9px;
      border-radius: var(--radius-full);
      font-family: var(--mono);
      font-size: 9.5px;
      font-weight: 500;
      color: var(--green);
    }""",
    """    .radar-beacon-pill {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      background: var(--green-subtle);
      border: 1px solid rgba(107, 140, 114, 0.35);
      width: 26px;
      height: 26px;
      border-radius: var(--radius-full);
    }"""
)

# 4. Define the unique, best Sync Logo SVG template
UNIQUE_SYNC_LOGO_LG = """<svg class="sync-rings-icon" viewBox="0 0 54 54" fill="none" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="syncRingG1" x1="6" y1="8" x2="38" y2="46" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#f09d83" />
      <stop offset="50%" stop-color="#cf7052" />
      <stop offset="100%" stop-color="#94422c" />
    </linearGradient>
    <linearGradient id="syncRingG2" x1="16" y1="8" x2="48" y2="46" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#cf7052" />
      <stop offset="50%" stop-color="#e88a6d" />
      <stop offset="100%" stop-color="#692817" />
    </linearGradient>
    <filter id="syncGlowFilt" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="3" stdDeviation="3.5" flood-color="#cf7052" flood-opacity="0.35"/>
    </filter>
  </defs>
  <g filter="url(#syncGlowFilt)">
    <!-- Primary Left Resonance Orbit -->
    <path d="M 21 12 C 12.7 12 6 18.7 6 27 C 6 35.3 12.7 42 21 42 C 27.2 42 32.5 38.3 34.8 33 C 33.9 31.4 32.1 27 27 27 C 21.9 27 20.1 22.6 19.2 21 C 21.5 15.7 26.8 12 33 12" 
          stroke="url(#syncRingG1)" stroke-width="4.8" stroke-linecap="round"/>
    <!-- Secondary Right Resonance Orbit -->
    <path d="M 33 42 C 41.3 42 48 35.3 48 27 C 48 18.7 41.3 12 33 12 C 26.8 12 21.5 15.7 19.2 21 C 20.1 22.6 21.9 27 27 27 C 32.1 27 33.9 31.4 34.8 33 C 32.5 38.3 27.2 42 21 42" 
          stroke="url(#syncRingG2)" stroke-width="4.8" stroke-linecap="round"/>
    <!-- Synchronous Core Focal Node -->
    <circle cx="27" cy="27" r="3.6" fill="#cf7052" />
    <circle cx="27" cy="27" r="1.6" fill="#ffffff" />
  </g>
</svg>"""

UNIQUE_SYNC_LOGO_SM = """<svg width="24" height="24" viewBox="0 0 54 54" fill="none" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="syncSmG1" x1="6" y1="8" x2="38" y2="46" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#f09d83" /><stop offset="100%" stop-color="#94422c" />
    </linearGradient>
    <linearGradient id="syncSmG2" x1="16" y1="8" x2="48" y2="46" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#cf7052" /><stop offset="100%" stop-color="#692817" />
    </linearGradient>
  </defs>
  <path d="M 21 12 C 12.7 12 6 18.7 6 27 C 6 35.3 12.7 42 21 42 C 27.2 42 32.5 38.3 34.8 33 C 33.9 31.4 32.1 27 27 27 C 21.9 27 20.1 22.6 19.2 21 C 21.5 15.7 26.8 12 33 12" stroke="url(#syncSmG1)" stroke-width="5" stroke-linecap="round"/>
  <path d="M 33 42 C 41.3 42 48 35.3 48 27 C 48 18.7 41.3 12 33 12 C 26.8 12 21.5 15.7 19.2 21 C 20.1 22.6 21.9 27 27 27 C 32.1 27 33.9 31.4 34.8 33 C 32.5 38.3 27.2 42 21 42" stroke="url(#syncSmG2)" stroke-width="5" stroke-linecap="round"/>
  <circle cx="27" cy="27" r="3.8" fill="#cf7052" />
  <circle cx="27" cy="27" r="1.6" fill="#ffffff" />
</svg>"""

# Replace splash logo
old_splash_logo = """            <svg class="sync-rings-icon" viewBox="0 0 64 64" fill="none">
              <circle cx="25" cy="32" r="18" stroke="#cf7052" stroke-width="5.5" stroke-linecap="round" />
              <circle cx="39" cy="32" r="18" stroke="#9f4e37" stroke-width="5.5" stroke-linecap="round" />
              <circle cx="32" cy="32" r="3" fill="#cf7052" />
            </svg>"""
html = html.replace(old_splash_logo, UNIQUE_SYNC_LOGO_LG)

# Replace topbar brand logo
old_topbar_logo = """                <svg width="24" height="24" viewBox="0 0 48 48" fill="none">
                  <circle cx="19" cy="24" r="14" stroke="#cf7052" stroke-width="4.5" />
                  <circle cx="29" cy="24" r="14" stroke="#9f4e37" stroke-width="4.5" />
                </svg>"""
html = html.replace(old_topbar_logo, UNIQUE_SYNC_LOGO_SM)

# Replace welcome mini logo
old_welcome_logo = """            <div class="mini-logo-lockup">
              <svg width="22" height="22" viewBox="0 0 48 48" fill="none">
                <circle cx="19" cy="24" r="14" stroke="#cf7052" stroke-width="4.5" />
                <circle cx="29" cy="24" r="14" stroke="#9f4e37" stroke-width="4.5" />
              </svg>
              <span class="mini-brand-name">Sync</span>
            </div>"""
new_welcome_logo = f"""            <div class="mini-logo-lockup">
              {UNIQUE_SYNC_LOGO_SM}
              <span class="mini-brand-name">Sync</span>
            </div>"""
html = html.replace(old_welcome_logo, new_welcome_logo)

# Replace match modal logo badge
old_match_badge = """              <div class="cel-sync-badge" title="Sync Connection">
                <svg width="20" height="20" viewBox="0 0 48 48" fill="none">
                  <circle cx="19" cy="24" r="14" stroke="#ffffff" stroke-width="5" />
                  <circle cx="29" cy="24" r="14" stroke="#ffffff" stroke-width="5" />
                </svg>
              </div>"""
new_match_badge = """              <div class="cel-sync-badge" title="Sync Connection">
                <svg width="22" height="22" viewBox="0 0 54 54" fill="none">
                  <path d="M 21 12 C 12.7 12 6 18.7 6 27 C 6 35.3 12.7 42 21 42 C 27.2 42 32.5 38.3 34.8 33 C 33.9 31.4 32.1 27 27 27 C 21.9 27 20.1 22.6 19.2 21 C 21.5 15.7 26.8 12 33 12" stroke="#ffffff" stroke-width="5" stroke-linecap="round"/>
                  <path d="M 33 42 C 41.3 42 48 35.3 48 27 C 48 18.7 41.3 12 33 12 C 26.8 12 21.5 15.7 19.2 21 C 20.1 22.6 21.9 27 27 27 C 32.1 27 33.9 31.4 34.8 33 C 32.5 38.3 27.2 42 21 42" stroke="#ffffff" stroke-width="5" stroke-linecap="round"/>
                  <circle cx="27" cy="27" r="3.8" fill="#ffffff" />
                </svg>
              </div>"""
html = html.replace(old_match_badge, new_match_badge)

# Fix modal sheet padding so the button is never cut off
html = html.replace(
    """    .modal-sheet {
      width: 100%;
      background: var(--surface);
      border-radius: var(--radius-lg) var(--radius-lg) 0 0;
      padding: 18px 18px 28px 18px;
      transform: translateY(100%);
      transition: transform 0.28s cubic-bezier(0.16, 1, 0.3, 1);
      max-height: 85%;
      overflow-y: auto;
      border-top: 1px solid var(--line);
    }""",
    """    .modal-sheet {
      width: 100%;
      background: var(--surface);
      border-radius: var(--radius-lg) var(--radius-lg) 0 0;
      padding: 18px 20px 38px 20px;
      transform: translateY(100%);
      transition: transform 0.28s cubic-bezier(0.16, 1, 0.3, 1);
      max-height: 88%;
      overflow-y: auto;
      border-top: 1px solid var(--line);
      box-sizing: border-box;
    }"""
)

with open("sync-app.html", "w", encoding="utf-8") as f:
    f.write(html)

print("Batch 1 applied successfully!")
