from PIL import Image
import os
from playwright.sync_api import sync_playwright

output_dir = r"C:\Users\JISHNU PG\.gemini\antigravity\brain\2fe2aca2-50c2-4441-b718-21efc2285c0e"

# We'll create a small HTML file with 4 logo concepts, render them in high-res, and view them.
logo_preview_html = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  body {
    background: #0f1012;
    color: #fff;
    font-family: -apple-system, sans-serif;
    display: flex;
    flex-wrap: wrap;
    gap: 30px;
    padding: 40px;
    justify-content: center;
  }
  .card {
    background: #191b1f;
    border: 1px solid #282c34;
    border-radius: 16px;
    padding: 24px;
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 220px;
    text-align: center;
  }
  .card h3 {
    margin: 14px 0 4px 0;
    font-size: 15px;
    font-weight: 600;
  }
  .card p {
    font-size: 11px;
    color: #8b92a0;
    margin: 0;
  }
  .icon-wrap {
    width: 90px;
    height: 90px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #121316;
    border-radius: 20px;
    border: 1px solid #22252c;
  }
</style>
</head>
<body>

  <!-- CONCEPT 1: Geometric Infinity S-Monogram (Ribbon Wave) -->
  <div class="card">
    <div class="icon-wrap">
      <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
        <defs>
          <linearGradient id="g1" x1="12" y1="12" x2="52" y2="52" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#f5a48d"/>
            <stop offset="50%" stop-color="#cf7052"/>
            <stop offset="100%" stop-color="#994129"/>
          </linearGradient>
          <linearGradient id="g2" x1="52" y1="52" x2="12" y2="12" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#e27c5f"/>
            <stop offset="50%" stop-color="#cf7052"/>
            <stop offset="100%" stop-color="#692515"/>
          </linearGradient>
          <filter id="glow1" x="-20%" y="-20%" width="140%" height="140%">
            <feDropShadow dx="0" dy="3" stdDeviation="3" flood-color="#cf7052" flood-opacity="0.4"/>
          </filter>
        </defs>
        <!-- Interlocking Diagonal Infinity S -->
        <path d="M 42 16 C 47.5 21.5 47.5 30.5 42 36 L 28 50 C 22.5 55.5 13.5 55.5 8 50 C 2.5 44.5 2.5 35.5 8 30 L 36 2 C 41.5 -3.5 50.5 -3.5 56 2" 
              fill="none" stroke="url(#g1)" stroke-width="7" stroke-linecap="round" />
      </svg>
    </div>
    <h3>Concept 1</h3>
    <p>Diagonal Infinity Flow</p>
  </div>

  <!-- CONCEPT 2: The S-Vortex Dual Complementary Waves (Dribbble Favorite) -->
  <div class="card">
    <div class="icon-wrap">
      <svg width="68" height="68" viewBox="0 0 64 64" fill="none">
        <defs>
          <linearGradient id="v1" x1="16" y1="10" x2="48" y2="34" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#ff9e85"/>
            <stop offset="100%" stop-color="#cf7052"/>
          </linearGradient>
          <linearGradient id="v2" x1="48" y1="54" x2="16" y2="30" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#cf7052"/>
            <stop offset="100%" stop-color="#913822"/>
          </linearGradient>
          <filter id="vGlow" x="-20%" y="-20%" width="140%" height="140%">
            <feDropShadow dx="0" dy="3" stdDeviation="3.5" flood-color="#cf7052" flood-opacity="0.35"/>
          </filter>
        </defs>
        <g filter="url(#vGlow)">
          <!-- Upper S-Wave: swoops from top right, loops left, dives into center -->
          <path d="M 44 14 C 44 14 36 10 26 10 C 17.16 10 10 17.16 10 26 C 10 32.8 14.3 38.6 20.4 40.8 L 36 24 C 38.2 21.6 39.5 18.5 39.5 15" 
                stroke="url(#v1)" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
          <!-- Lower S-Wave: swoops from bottom left, loops right, dives into center -->
          <path d="M 20 50 C 20 50 28 54 38 54 C 46.84 54 54 46.84 54 38 C 54 31.2 49.7 25.4 43.6 23.2 L 28 40 C 25.8 42.4 24.5 45.5 24.5 49" 
                stroke="url(#v2)" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
          <!-- Center Synchronous Pulse Node -->
          <circle cx="32" cy="32" r="3.5" fill="#ffffff" />
        </g>
      </svg>
    </div>
    <h3>Concept 2</h3>
    <p>S-Vortex Resonant Loops</p>
  </div>

  <!-- CONCEPT 3: The Interlocking Torus S-Monogram (Linear / Raycast Style) -->
  <div class="card">
    <div class="icon-wrap">
      <svg width="66" height="66" viewBox="0 0 64 64" fill="none">
        <defs>
          <linearGradient id="t1" x1="10" y1="10" x2="44" y2="44" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#ffaa91"/>
            <stop offset="50%" stop-color="#cf7052"/>
            <stop offset="100%" stop-color="#8a301b"/>
          </linearGradient>
          <linearGradient id="t2" x1="20" y1="20" x2="54" y2="54" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#e27c5e"/>
            <stop offset="100%" stop-color="#591c0e"/>
          </linearGradient>
        </defs>
        <!-- Two Interlocked Precision Torus Rings forming S Monogram -->
        <!-- Top-Left Loop -->
        <path d="M 32 10 C 20.95 10 12 18.95 12 30 C 12 37.5 16.1 44.1 22.2 47.5 L 34.5 35.2 C 32.9 33.8 32 31.8 32 29.5 C 32 25.9 34.9 23 38.5 23 C 40.8 23 42.8 23.9 44.2 25.5 L 50.8 18.9 C 46.2 13.4 39.5 10 32 10 Z" 
              fill="url(#t1)"/>
        <!-- Bottom-Right Complementary Loop -->
        <path d="M 32 54 C 43.05 54 52 45.05 52 34 C 52 26.5 47.9 19.9 41.8 16.5 L 29.5 28.8 C 31.1 30.2 32 32.2 32 34.5 C 32 38.1 29.1 41 25.5 41 C 23.2 41 21.2 40.1 19.8 38.5 L 13.2 45.1 C 17.8 50.6 24.5 54 32 54 Z" 
              fill="url(#t2)" opacity="0.95"/>
      </svg>
    </div>
    <h3>Concept 3</h3>
    <p>Interlocking Torus S</p>
  </div>

  <!-- CONCEPT 4: Precision Modern Glyph (Harmonic Proximity Crest) -->
  <div class="card">
    <div class="icon-wrap">
      <svg width="66" height="66" viewBox="0 0 64 64" fill="none">
        <defs>
          <linearGradient id="pGrad1" x1="14" y1="12" x2="50" y2="48" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#ffab94"/>
            <stop offset="50%" stop-color="#cf7052"/>
            <stop offset="100%" stop-color="#943922"/>
          </linearGradient>
          <filter id="pDrop" x="-20%" y="-20%" width="140%" height="140%">
            <feDropShadow dx="0" dy="4" stdDeviation="4" flood-color="#cf7052" flood-opacity="0.38"/>
          </filter>
        </defs>
        <g filter="url(#pDrop)">
          <!-- Continuous Chiral S-Ribbon -->
          <path d="M 43 14 C 47.4 14 51 17.6 51 22 C 51 28.5 44.5 31.5 38 32 C 28.5 32.7 20 35.5 20 44 C 20 48.4 23.6 52 28 52 C 34.5 52 41 47.5 45 42" 
                stroke="url(#pGrad1)" stroke-width="6.5" stroke-linecap="round"/>
          <path d="M 21 50 C 16.6 50 13 46.4 13 42 C 13 35.5 19.5 32.5 26 32 C 35.5 31.3 44 28.5 44 20 C 44 15.6 40.4 12 36 12 C 29.5 12 23 16.5 19 22" 
                stroke="url(#pGrad1)" stroke-width="6.5" stroke-linecap="round"/>
          <circle cx="32" cy="32" r="3.2" fill="#ffffff" />
        </g>
      </svg>
    </div>
    <h3>Concept 4</h3>
    <p>Chiral S-Ribbon Nexus</p>
  </div>

</body>
</html>
"""

with open("logo_preview.html", "w", encoding="utf-8") as f:
    f.write(logo_preview_html)

with sync_playwright() as p:
    try:
        browser = p.chromium.launch(headless=True, channel="chrome")
    except Exception:
        browser = p.chromium.launch(headless=True, channel="msedge")
    page = browser.new_page(viewport={"width": 1100, "height": 450})
    page.goto(f"file:///{os.path.abspath('logo_preview.html').replace(os.sep, '/')}")
    page.wait_for_timeout(400)
    page.screenshot(path=os.path.join(output_dir, "logo_concepts_preview.png"))
    browser.close()
    print("Generated logo_concepts_preview.png successfully!")
