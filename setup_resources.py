import urllib.request
import re
import os
import shutil

font_dir = os.path.join("android", "app", "src", "main", "res", "font")
os.makedirs(font_dir, exist_ok=True)

# Copy portraits to android assets
assets_dir = os.path.join("android", "app", "src", "main", "assets", "portraits")
os.makedirs(assets_dir, exist_ok=True)
src_portraits = os.path.join("assets", "portraits")
if os.path.exists(src_portraits):
    for f in os.listdir(src_portraits):
        src = os.path.join(src_portraits, f)
        if os.path.isfile(src):
            shutil.copy2(src, os.path.join(assets_dir, f))
    print(f"Copied {len(os.listdir(assets_dir))} portraits to {assets_dir}")

# Fetch Google fonts CSS
req = urllib.request.Request(
    "https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=DM+Sans:wght@400;500;600;700&family=Playfair+Display:ital,wght@0,500;0,600;1,500;1,600&display=swap",
    headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}
)
with urllib.request.urlopen(req) as resp:
    css = resp.read().decode("utf-8")

blocks = css.split("@font-face")
for b in blocks:
    if not b.strip():
        continue
    fam_match = re.search(r"font-family:\s*['\"]([^'\"]+)['\"]", b)
    weight_match = re.search(r"font-weight:\s*(\d+)", b)
    style_match = re.search(r"font-style:\s*(\w+)", b)
    url_match = re.search(r"url\((https://[^)]+\.ttf)\)", b)
    if not (fam_match and weight_match and url_match):
        continue
    
    fam = fam_match.group(1).lower().replace(" ", "_")
    wt = weight_match.group(1)
    st = style_match.group(1) if style_match else "normal"
    url = url_match.group(1)
    
    fname = f"{fam}_{wt}"
    if st == "italic":
        fname += "_italic"
    fname += ".ttf"
    
    target_path = os.path.join(font_dir, fname)
    if not os.path.exists(target_path):
        print(f"Downloading {fname}...")
        urllib.request.urlretrieve(url, target_path)
    print(f"Ready: {fname}")

print("Resources setup successfully!")
