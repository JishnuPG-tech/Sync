import os
import time
from playwright.sync_api import sync_playwright

output_dir = r"C:\Users\JISHNU PG\.gemini\antigravity\brain\2fe2aca2-50c2-4441-b718-21efc2285c0e"
html_path = r"file:///c:/Users/JISHNU PG/Music/Diagram/sync-app.html"

def run():
    with sync_playwright() as p:
        try:
            browser = p.chromium.launch(headless=True, channel="chrome")
        except Exception:
            browser = p.chromium.launch(headless=True, channel="msedge")
            
        context = browser.new_context(viewport={"width": 1280, "height": 1050}, device_scale_factor=2)
        page = context.new_page()

        # 1. Load HTML
        page.goto(html_path)
        page.wait_for_timeout(600)

        # 1. Splash Screen with unique new Sync logo
        page.evaluate("navigateToScreen('splash')")
        page.wait_for_timeout(400)
        page.screenshot(path=os.path.join(output_dir, "web_01_splash.png"))
        print("Captured web_01_splash.png")

        # 2. Welcome Screen
        page.evaluate("navigateToScreen('welcome')")
        page.wait_for_timeout(400)
        page.screenshot(path=os.path.join(output_dir, "web_02_welcome.png"))
        print("Captured web_02_welcome.png")

        # 3. Setup Screen with Location Input
        page.evaluate("navigateToScreen('setup')")
        page.wait_for_timeout(400)
        page.screenshot(path=os.path.join(output_dir, "web_03_setup.png"))
        print("Captured web_03_setup.png")

        # 4. Location Map Picker Modal
        page.evaluate("openLocationMapModal()")
        page.wait_for_timeout(400)
        page.screenshot(path=os.path.join(output_dir, "web_08_location_map.png"))
        print("Captured web_08_location_map.png")
        page.evaluate("closeModal('locationMapModal')")
        page.wait_for_timeout(200)

        # 5. Radar Screen (Empty Scan State - no devices nearby)
        page.evaluate("peers.forEach(p => p.inRadar = false); renderPeerCards(); navigateToScreen('main', 'radar')")
        page.wait_for_timeout(500)
        page.screenshot(path=os.path.join(output_dir, "web_09_radar_empty_scan.png"))
        print("Captured web_09_radar_empty_scan.png")

        # 6. Radar Screen (Devices Detected Nearby - dynamic pins & cards)
        page.evaluate("peers[0].inRadar = true; peers[1].inRadar = true; renderPeerCards();")
        page.wait_for_timeout(500)
        page.screenshot(path=os.path.join(output_dir, "web_04_radar.png"))
        print("Captured web_04_radar.png")

        # 7. Discovered Screen (With locked Bottom Nav)
        page.evaluate("navigateToScreen('main', 'discovered')")
        page.wait_for_timeout(400)
        page.screenshot(path=os.path.join(output_dir, "web_05_discovered.png"))
        print("Captured web_05_discovered.png")

        # 8. Mutual Connection Modal (Sync rings badge, no heart, fully visible buttons)
        page.evaluate("openMutualMatchModal(peers[0])")
        page.wait_for_timeout(400)
        page.screenshot(path=os.path.join(output_dir, "web_06_match_modal.png"))
        print("Captured web_06_match_modal.png")
        page.evaluate("closeModal('mutualMatchModal')")

        # 9. Dark Mode Radar Screen
        page.evaluate("toggleAppTheme(true); navigateToScreen('main', 'radar')")
        page.wait_for_timeout(400)
        page.screenshot(path=os.path.join(output_dir, "web_07_radar_dark.png"))
        print("Captured web_07_radar_dark.png")

        browser.close()

if __name__ == "__main__":
    run()
