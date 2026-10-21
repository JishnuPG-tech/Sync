import os
from playwright.sync_api import sync_playwright

output_dir = r"C:\Users\JISHNU PG\.gemini\antigravity\brain\2fe2aca2-50c2-4441-b718-21efc2285c0e"

html = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  body {
    background: #0b0c0e;
    color: #fff;
    font-family: -apple-system, sans-serif;
    display: flex;
    gap: 40px;
    padding: 60px;
    justify-content: center;
    align-items: center;
  }
  .app-icon {
    width: 120px;
    height: 120px;
    border-radius: 28px;
    background: radial-gradient(circle at 50% 20%, #251b17 0%, #111214 70%, #0a0a0b 100%);
    box-shadow: 0 16px 38px rgba(0,0,0,0.7), 0 0 0 1px rgba(255,255,255,0.08), inset 0 1px 1px rgba(255,255,255,0.18);
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;
  }
  .light-app-icon {
    width: 120px;
    height: 120px;
    border-radius: 28px;
    background: radial-gradient(circle at 50% 20%, #ffffff 0%, #f7f4ed 70%, #ede8de 100%);
    box-shadow: 0 16px 38px rgba(21,21,19,0.12), 0 0 0 1px rgba(217,213,204,0.8), inset 0 1px 1px #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;
  }
  .container {
    text-align: center;
  }
  h4 {
    margin-top: 14px;
    font-size: 13px;
    color: #a0a6b5;
    letter-spacing: 0.05em;
  }
</style>
</head>
<body>

  <!-- CANDIDATE A: CONTINUOUS HARMONIC S-ORBIT (OBSIDIAN APP ICON) -->
  <div class="container">
    <div class="app-icon">
      <svg width="78" height="78" viewBox="0 0 80 80" fill="none">
        <defs>
          <linearGradient id="orbitGradA" x1="16" y1="12" x2="64" y2="68" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#ff997e" />
            <stop offset="48%" stop-color="#cf7052" />
            <stop offset="100%" stop-color="#943820" />
          </linearGradient>
          <filter id="iconGlowA" x="-20%" y="-20%" width="140%" height="140%">
            <feDropShadow dx="0" dy="4" stdDeviation="4" flood-color="#cf7052" flood-opacity="0.45"/>
          </filter>
        </defs>
        <!-- The Continuous Harmonic S Wave -->
        <g filter="url(#iconGlowA)">
          <!-- Top orbital loop -->
          <path d="M 54 22 C 54 14.27 47.73 8 40 8 C 28.95 8 20 16.95 20 28 C 20 34.6 23.2 40.5 28.2 44.2 L 51.8 35.8 C 56.8 39.5 60 45.4 60 52 C 60 63.05 51.05 72 40 72 C 32.27 72 26 65.73 26 58" 
                stroke="url(#orbitGradA)" stroke-width="7.5" stroke-linecap="round"/>
          <!-- Central Optical Connection Node -->
          <circle cx="40" cy="40" r="4.2" fill="#ffffff" />
          <circle cx="40" cy="40" r="2.2" fill="#cf7052" />
        </g>
      </svg>
    </div>
    <h4>DARK APP ICON</h4>
  </div>

  <!-- CANDIDATE B: PURE GEOMETRIC PRECISION S-SYNC MONOGRAM -->
  <div class="container">
    <div class="app-icon">
      <svg width="74" height="74" viewBox="0 0 80 80" fill="none">
        <defs>
          <linearGradient id="pGradA1" x1="15" y1="10" x2="65" y2="70" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#ffaa91" />
            <stop offset="40%" stop-color="#e18462" />
            <stop offset="100%" stop-color="#91331c" />
          </linearGradient>
          <filter id="pGlowB" x="-30%" y="-30%" width="160%" height="160%">
            <feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#cf7052" flood-opacity="0.5"/>
          </filter>
        </defs>
        <g filter="url(#pGlowB)">
          <!-- Dynamic S Interlocking Ribbons -->
          <!-- Upper Arc -->
          <path d="M 52 26 C 52 17.16 44.84 10 36 10 C 25.5 10 17 18.5 17 29 C 17 35.5 20.5 41.2 25.8 44.4 L 46 32 C 49.8 35 52 39.5 52 44.5" 
                stroke="url(#pGradA1)" stroke-width="7" stroke-linecap="round"/>
          <!-- Lower Arc -->
          <path d="M 28 54 C 28 62.84 35.16 70 44 70 C 54.5 70 63 61.5 63 51 C 63 44.5 59.5 38.8 54.2 35.6 L 34 48 C 30.2 45 28 40.5 28 35.5" 
                stroke="url(#pGradA1)" stroke-width="7" stroke-linecap="round"/>
          <circle cx="40" cy="40" r="3.8" fill="#ffffff" />
        </g>
      </svg>
    </div>
    <h4>GEOMETRIC S-RIBBON</h4>
  </div>

  <!-- CANDIDATE C: LIGHT MODE / PORTFOLIO WARM PAPER ICON -->
  <div class="container">
    <div class="light-app-icon">
      <svg width="74" height="74" viewBox="0 0 80 80" fill="none">
        <defs>
          <linearGradient id="lightGrad" x1="15" y1="10" x2="65" y2="70" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#cf7052" />
            <stop offset="100%" stop-color="#8a2e18" />
          </linearGradient>
        </defs>
        <g filter="drop-shadow(0 3px 6px rgba(207,112,82,0.35))">
          <path d="M 52 26 C 52 17.16 44.84 10 36 10 C 25.5 10 17 18.5 17 29 C 17 35.5 20.5 41.2 25.8 44.4 L 46 32 C 49.8 35 52 39.5 52 44.5" 
                stroke="url(#lightGrad)" stroke-width="7" stroke-linecap="round"/>
          <path d="M 28 54 C 28 62.84 35.16 70 44 70 C 54.5 70 63 61.5 63 51 C 63 44.5 59.5 38.8 54.2 35.6 L 34 48 C 30.2 45 28 40.5 28 35.5" 
                stroke="url(#lightGrad)" stroke-width="7" stroke-linecap="round"/>
          <circle cx="40" cy="40" r="3.8" fill="#cf7052" />
        </g>
      </svg>
    </div>
    <h4>WARM PAPER ICON</h4>
  </div>

</body>
</html>
"""

with open("test_logo.html", "w", encoding="utf-8") as f:
    f.write(html)

with sync_playwright() as p:
    try:
        browser = p.chromium.launch(headless=True, channel="chrome")
    except Exception:
        browser = p.chromium.launch(headless=True, channel="msedge")
    page = browser.new_page(viewport={"width": 800, "height": 340})
    page.goto(f"file:///{os.path.abspath('test_logo.html').replace(os.sep, '/')}")
    page.wait_for_timeout(400)
    page.screenshot(path=os.path.join(output_dir, "logo_refined_preview.png"))
    browser.close()
    print("Generated logo_refined_preview.png successfully!")
