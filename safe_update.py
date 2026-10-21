import re

with open("sync-app.html", "r", encoding="utf-8") as f:
    lines = f.readlines()

new_lines = []
in_splash_logo = False
in_topbar_brand = False
in_welcome_mini = False
in_cel_badge = False

HELIX_LOGO_LG = """              <svg class="sync-rings-icon" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
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
                  <path d="M 52 24 C 52 16.27 45.73 10 38 10 C 27.5 10 19 18.5 19 29 C 19 36.5 23.5 43 30.5 45.5 L 49.5 34.5 C 56.5 37 61 43.5 61 51 C 61 61.5 52.5 70 42 70 C 34.27 70 28 63.73 28 56" 
                        stroke="url(#helixGradLg)" stroke-width="7.5" stroke-linecap="round"/>
                  <circle cx="40" cy="40" r="4.5" fill="#ffffff" />
                  <circle cx="40" cy="40" r="2.2" fill="#cf7052" />
                </g>
              </svg>
"""

HELIX_LOGO_SM = """                <svg width="26" height="26" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
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
                </svg>
"""

HELIX_LOGO_BADGE = """                <svg width="22" height="22" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M 52 24 C 52 16.27 45.73 10 38 10 C 27.5 10 19 18.5 19 29 C 19 36.5 23.5 43 30.5 45.5 L 49.5 34.5 C 56.5 37 61 43.5 61 51 C 61 61.5 52.5 70 42 70 C 34.27 70 28 63.73 28 56" 
                        stroke="#ffffff" stroke-width="8" stroke-linecap="round"/>
                  <circle cx="40" cy="40" r="4.8" fill="#ffffff" />
                </svg>
"""

skip_until_tag = None

i = 0
while i < len(lines):
    line = lines[i]

    # 1. Dimensions update
    if "--phone-max-w:" in line:
        new_lines.append("      /* Exact Virtual Device Screen Size (1080 x 2412 px / 412 x 920 dp, 20.1:9 Aspect Ratio) */\n")
        new_lines.append("      --phone-w: 412px;\n")
        new_lines.append("      --phone-max-w: 412px;\n")
        new_lines.append("      --phone-h: min(920px, 94vh);\n")
        # skip old --phone-h if on next line
        if i + 1 < len(lines) and "--phone-h:" in lines[i+1]:
            i += 1
        i += 1
        continue

    # 2. Toolbar badge
    if 'class="toolbar-badge"' in line:
        new_lines.append('        <span class="toolbar-badge">Virtual Device 1080 × 2412</span>\n')
        i += 1
        continue

    # 3. Remove "proximity radar active symbol" from top bar
    if 'class="radar-beacon-pill"' in line or 'radar-status-pill' in line:
        # skip this and closing tag
        while i < len(lines) and '</div>' not in lines[i]:
            i += 1
        i += 1 # skip the </div>
        continue

    # 4. Replace Splash Logo
    if '<div class="splash-logo-wrap">' in line:
        new_lines.append(line)
        new_lines.append(HELIX_LOGO_LG)
        # skip until </div>
        i += 1
        while i < len(lines) and '</div>' not in lines[i]:
            i += 1
        new_lines.append(lines[i]) # the </div>
        i += 1
        continue

    # 5. Replace Topbar brand logo
    if '<div class="topbar-brand">' in line:
        new_lines.append(line)
        new_lines.append(HELIX_LOGO_SM)
        new_lines.append('                <span class="brand-wordmark">Sync</span>\n')
        # skip until </div>
        i += 1
        while i < len(lines) and '</div>' not in lines[i]:
            i += 1
        new_lines.append(lines[i]) # the </div>
        i += 1
        continue

    # 6. Replace Welcome mini logo
    if '<div class="mini-logo-lockup">' in line:
        new_lines.append(line)
        new_lines.append(HELIX_LOGO_SM)
        new_lines.append('              <span class="mini-brand-name">Sync</span>\n')
        i += 1
        while i < len(lines) and '</div>' not in lines[i]:
            i += 1
        new_lines.append(lines[i]) # the </div>
        i += 1
        continue

    # 7. Replace Match Badge
    if 'class="cel-sync-badge"' in line:
        new_lines.append(line)
        new_lines.append(HELIX_LOGO_BADGE)
        i += 1
        while i < len(lines) and '</div>' not in lines[i]:
            i += 1
        new_lines.append(lines[i]) # the </div>
        i += 1
        continue

    new_lines.append(line)
    i += 1

with open("sync-app.html", "w", encoding="utf-8") as f:
    f.writelines(new_lines)

print("Safe update completed successfully!")
