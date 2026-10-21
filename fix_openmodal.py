with open("sync-app.html", "r", encoding="utf-8") as f:
    html = f.read()

target = "    function closeModal(id) {"
replacement = """    function openModal(id) {
      const el = document.getElementById(id);
      if (el) el.classList.add('active');
    }

    function closeModal(id) {"""

if target in html:
    html = html.replace(target, replacement, 1)
    with open("sync-app.html", "w", encoding="utf-8") as f:
        f.write(html)
    print("Successfully added openModal!")
else:
    print("Target not found")
