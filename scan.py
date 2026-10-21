with open("sync-app.html", encoding="utf-8") as f:
    text = f.read()

targets = [
    "DESIGN TOKENS",
    "PHONE STAGE",
    "HARDWARE NOTCH",
    "STATUS BAR",
    "MAIN APP SHELL",
    "BOTTOM NAVIGATION BAR",
    "MODAL 1: PERSON PREVIEW",
    "MODAL 2:",
    'id="screen-splash"',
    'id="screen-welcome"',
    'id="screen-setup"',
    'id="screen-main"',
    'id="personPreviewModal"',
    'id="mutualMatchModal"',
    'function setPowerMode',
    'function renderPeerCards',
    'let peers ='
]

for t in targets:
    idx = text.find(t)
    if idx != -1:
        print(f"{t:30} -> line {text[:idx].count(chr(10)) + 1}")
