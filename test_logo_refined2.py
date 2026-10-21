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
    width: 124px;
    height: 124px;
    border-radius: 28px;
    background: radial-gradient(circle at 50% 20%, #201714 0%, #111215 65%, #080809 100%);
    box-shadow: 0 18px 40px rgba(0,0,0,0.7), 0 0 0 1px rgba(255,255,255,0.08), inset 0 1px 1.5px rgba(255,255,255,0.22);
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;
  }
  .light-app-icon {
    width: 124px;
    height: 124px;
    border-radius: 28px;
    background: radial-gradient(circle at 50% 15%, #ffffff 0%, #fbfaf6 60%, #eeeae1 100%);
    box-shadow: 0 18px 40px rgba(21,21,19,0.12), 0 0 0 1px #d9d5cc, inset 0 1px 1.5px #fff;
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

  <!-- DESIGN 1: THE SYNCHRONOUS HELIX (S-MONOGRAM WITH DUAL HARMONIC APEXES) -->
  <div class="container">
    <div class="app-icon">
      <svg width="78" height="78" viewBox="0 0 80 80" fill="none">
        <defs>
          <linearGradient id="helixGrad" x1="16" y1="12" x2="64" y2="68" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#ff9a7f" />
            <stop offset="35%" stop-color="#cf7052" />
            <stop offset="70%" stop-color="#e18462" />
            <stop offset="100%" stop-color="#91331c" />
          </linearGradient>
          <filter id="hGlow" x="-20%" y="-20%" width="140%" height="140%">
            <feDropShadow dx="0" dy="4" stdDeviation="5" flood-color="#cf7052" flood-opacity="0.5"/>
          </filter>
        </defs>
        <g filter="url(#hGlow)">
          <!-- Top Orbit Curve -->
          <path d="M 52 24 C 52 16.27 45.73 10 38 10 C 27.5 10 19 18.5 19 29 C 19 36.5 23.5 43 30.5 45.5 L 49.5 34.5 C 56.5 37 61 43.5 61 51 C 61 61.5 52.5 70 42 70 C 34.27 70 28 63.73 28 56" 
                stroke="url(#helixGrad)" stroke-width="7.5" stroke-linecap="round"/>
          <!-- Luminous Center Nexus -->
          <circle cx="40" cy="40" r="4.5" fill="#ffffff" />
          <circle cx="40" cy="40" r="2.2" fill="#cf7052" />
        </g>
      </svg>
    </div>
    <h4>THE SYNCHRONOUS HELIX</h4>
  </div>

  <!-- DESIGN 2: THE DUAL INTERSECTING CHIRAL ORBIT (MOST AWARD-WINNING TECH LOGO FORM) -->
  <div class="container">
    <div class="app-icon">
      <svg width="80" height="80" viewBox="0 0 80 80" fill="none">
        <defs>
          <linearGradient id="chiral1" x1="18" y1="14" x2="62" y2="58" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#ffa58d"/>
            <stop offset="50%" stop-color="#cf7052"/>
            <stop offset="100%" stop-color="#943820"/>
          </linearGradient>
          <linearGradient id="chiral2" x1="62" y1="66" x2="18" y2="22" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#e27c5e"/>
            <stop offset="50%" stop-color="#cf7052"/>
            <stop offset="100%" stop-color="#732715"/>
          </linearGradient>
          <filter id="cGlow" x="-20%" y="-20%" width="140%" height="140%">
            <feDropShadow dx="0" dy="4" stdDeviation="4.5" flood-color="#cf7052" flood-opacity="0.45"/>
          </filter>
        </defs>
        <g filter="url(#cGlow)">
          <!-- Primary Loop: Starts at center, expands outward-up into circle, sweeps back -->
          <path d="M 40 40 C 35 32 30 22 30 18 C 30 12.48 34.48 8 40 8 C 45.52 8 50 12.48 50 18 C 50 26 44 34 32 44 C 24 50 18 56 18 62 C 18 67.52 22.48 72 28 72 C 33.52 72 38 67.52 38 62 C 38 58 35 52 32 48" 
                stroke="url(#chiral1)" stroke-width="7" stroke-linecap="round"/>
          <circle cx="40" cy="40" r="4.2" fill="#ffffff" />
        </g>
      </svg>
    </div>
    <h4>CHIRAL ORBIT</h4>
  </div>

  <!-- DESIGN 3: LIGHT MODE PRESENTATION -->
  <div class="container">
    <div class="light-app-icon">
      <svg width="78" height="78" viewBox="0 0 80 80" fill="none">
        <defs>
          <linearGradient id="lightHelix" x1="16" y1="12" x2="64" y2="68" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#e18462" />
            <stop offset="100%" stop-color="#943820" />
          </linearGradient>
          <filter id="lhGlow" x="-20%" y="-20%" width="140%" height="140%">
            <feDropShadow dx="0" dy="3" stdDeviation="4" flood-color="#cf7052" flood-opacity="0.3"/>
          </filter>
        </defs>
        <g filter="url(#lhGlow)">
          <path d="M 52 24 C 52 16.27 45.73 10 38 10 C 27.5 10 19 18.5 19 29 C 19 36.5 23.5 43 30.5 45.5 L 49.5 34.5 C 56.5 37 61 43.5 61 51 C 61 61.5 52.5 70 42 70 C 34.27 70 28 63.73 28 56" 
                stroke="url(#lightHelix)" stroke-width="7.5" stroke-linecap="round"/>
          <circle cx="40" cy="40" r="4.2" fill="#cf7052" />
          <circle cx="40" cy="40" r="2" fill="#ffffff" />
        </g>
      </svg>
    </div>
    <h4>WARM PAPER HELIX</h4>
  </div>

</body>
</html>
"""

with open("test_logo2.html", "w", encoding="utf-8") as f:
    f.write(html)

with sync_playwright() as p:
    try:
        browser = p.chromium.launch(headless=True, channel="chrome")
    except Exception:
        browser = p.chromium.launch(headless=True, channel="msedge")
    page = browser.new_page(viewport={"width": 800, "height": 340})
    page.goto(f"file:///{os.path.abspath('test_logo2.html').replace(os.sep, '/')}")
    page.wait_for_timeout(400)
    page.screenshot(path=os.path.join(output_dir, "logo_refined2_preview.png"))
    browser.close()
    print("Generated logo_refined2_preview.png successfully!")
