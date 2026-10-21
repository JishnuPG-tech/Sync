with open("sync-app.html", "r", encoding="utf-8") as f:
    html = f.read()

# 1. Update phone dimensions to match Virtual Device (1080 x 2412, aspect ratio 1 : 2.233, 412 x 920 dp)
OLD_DIMENSIONS = """      /* Universal Physical Device Dimensions (16.74 cm / 6.6-inch modern display, 19.5:9 aspect) */
      --phone-w: 395px;
      --phone-max-w: 395px;
      --phone-h: min(855px, 92vh);"""

NEW_DIMENSIONS = """      /* Exact Virtual Device Screen Size (1080 x 2412 px / 412 x 920 dp, 20.1:9 Aspect Ratio) */
      --phone-w: 412px;
      --phone-max-w: 412px;
      --phone-h: min(920px, 94vh);"""

html = html.replace(OLD_DIMENSIONS, NEW_DIMENSIONS)

# Update toolbar badge to reflect Virtual Device screen size
html = html.replace(
    '<span class="toolbar-badge">16.74 cm (6.6") Mobile Display</span>',
    '<span class="toolbar-badge">Virtual Device 1080 × 2412</span>'
)

# 2. REMOVE "proximity radar active symbol" COMPLETELY from the top bar!
OLD_TOPBAR_BEACON = """              <div class="radar-beacon-pill" title="Proximity Radar Active">
                <span class="pulse-dot"></span>
              </div>"""

# Remove it completely from the HTML
html = html.replace(OLD_TOPBAR_BEACON, "")

# 3. New Unique "Synchronous Helix" SVG Logo
# Large for Splash Screen
HELIX_LOGO_LG = """<svg class="sync-rings-icon" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="helixGradLg" x1="16" y1="12" x2="64" y2="68" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#ff9a7f" />
      <stop offset="35%" stop-color="#cf7052" />
      <stop offset="70%" stop-color="#e18462" />
      <stop offset="100%" stop-color="#91331c" />
    </linearGradient>
    <filter id="helixGlowLg" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="4" stdDeviation="5" flood-color="#cf7052" flood-opacity="0.45"/>
    </filter>
  </defs>
  <g filter="url(#helixGlowLg)">
    <!-- Harmonic S-Orbit Continuum -->
    <path d="M 52 24 C 52 16.27 45.73 10 38 10 C 27.5 10 19 18.5 19 29 C 19 36.5 23.5 43 30.5 45.5 L 49.5 34.5 C 56.5 37 61 43.5 61 51 C 61 61.5 52.5 70 42 70 C 34.27 70 28 63.73 28 56" 
          stroke="url(#helixGradLg)" stroke-width="7.5" stroke-linecap="round"/>
    <!-- Luminous Synchronous Nexus -->
    <circle cx="40" cy="40" r="4.5" fill="#ffffff" />
    <circle cx="40" cy="40" r="2.2" fill="#cf7052" />
  </g>
</svg>"""

# Medium for Topbar & Welcome
HELIX_LOGO_SM = """<svg width="26" height="26" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="helixGradSm" x1="16" y1="12" x2="64" y2="68" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#ff9a7f" />
      <stop offset="50%" stop-color="#cf7052" />
      <stop offset="100%" stop-color="#91331c" />
    </linearGradient>
  </defs>
  <path d="M 52 24 C 52 16.27 45.73 10 38 10 C 27.5 10 19 18.5 19 29 C 19 36.5 23.5 43 30.5 45.5 L 49.5 34.5 C 56.5 37 61 43.5 61 51 C 61 61.5 52.5 70 42 70 C 34.27 70 28 63.73 28 56" 
        stroke="url(#helixGradSm)" stroke-width="8" stroke-linecap="round"/>
  <circle cx="40" cy="40" r="4.8" fill="#cf7052" />
  <circle cx="40" cy="40" r="2.2" fill="#ffffff" />
</svg>"""

# Match badge icon (White stroke for terracotta circle)
HELIX_LOGO_BADGE = """<svg width="22" height="22" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M 52 24 C 52 16.27 45.73 10 38 10 C 27.5 10 19 18.5 19 29 C 19 36.5 23.5 43 30.5 45.5 L 49.5 34.5 C 56.5 37 61 43.5 61 51 C 61 61.5 52.5 70 42 70 C 34.27 70 28 63.73 28 56" 
        stroke="#ffffff" stroke-width="8" stroke-linecap="round"/>
  <circle cx="40" cy="40" r="4.8" fill="#ffffff" />
</svg>"""

# Replace splash logo
old_splash = html[html.find('<div class="splash-logo-wrap">'):html.find('</div>\n            <h1 class="splash-title">')]
new_splash = f"""<div class="splash-logo-wrap">
              {HELIX_LOGO_LG}
            """
html = html.replace(old_splash, new_splash)

# Replace topbar logo
old_topbar_svg = html[html.find('<div class="topbar-brand">'):html.find('<span class="brand-wordmark">Sync</span>')]
new_topbar_svg = f"""<div class="topbar-brand">
                {HELIX_LOGO_SM}
                """
html = html.replace(old_topbar_svg, new_topbar_svg)

# Replace welcome mini logo
old_welcome_mini = html[html.find('<div class="mini-logo-lockup">'):html.find('<span class="mini-brand-name">Sync</span>')]
new_welcome_mini = f"""<div class="mini-logo-lockup">
              {HELIX_LOGO_SM}
              """
html = html.replace(old_welcome_mini, new_welcome_mini)

# Replace match badge
old_badge = html[html.find('<div class="cel-sync-badge" title="Sync Connection">'):html.find('</div>\n              <img src="assets/portraits/portrait_02.jpg" id="celPeerAvatar"')]
new_badge = f"""<div class="cel-sync-badge" title="Sync Connection">
                {HELIX_LOGO_BADGE}
              """
html = html.replace(old_badge, new_badge)

with open("sync-app.html", "w", encoding="utf-8") as f:
    f.write(html)

print("Applied final polishes successfully!")
