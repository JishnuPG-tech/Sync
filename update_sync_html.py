import re

with open("sync-app.html", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Update avatar section
pattern_avatar = re.compile(r'<!-- Avatar Picker -->\s*<div class="avatar-picker-section">.*?</div>\s*</div>', re.DOTALL)
new_avatar = '''<!-- Avatar Picker -->
          <div class="avatar-picker-section">
            <div class="avatar-preview-wrap" onclick="triggerPhotoUpload()" style="cursor: pointer;">
              <img src="" id="currentAvatarImg" alt="Profile" class="avatar-preview-img" style="display:none;">
              <div id="avatarPlaceholderIcon" style="display:flex;align-items:center;justify-content:center;width:100%;height:100%;background:var(--surface-subtle);border-radius:50%;">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--terracotta)" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
              </div>
              <div class="avatar-badge-edit">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
              </div>
            </div>
            <input type="file" id="photoFileInput" accept="image/*" style="display:none;" onchange="handlePhotoUpload(event)">
            <button type="button" class="upload-photo-btn" onclick="triggerPhotoUpload()" style="margin-top:8px;background:none;border:none;color:var(--terracotta);font-family:var(--font-sans);font-weight:600;font-size:12.5px;cursor:pointer;">
              Upload profile photo
            </button>
          </div>'''

content = pattern_avatar.sub(new_avatar, content, count=1)

# 2. Reset input values to empty placeholders
content = content.replace('placeholder="e.g. Jishnu PG" value="Jishnu PG"', 'placeholder="e.g. your full name" value=""')
content = content.replace('placeholder="e.g. jishnu_pg" value="jishnu_pg"', 'placeholder="e.g. sync_handle" value=""')
content = content.replace('placeholder="e.g. jishnu.tech" value="jishnu.tech"', 'placeholder="e.g. your_instagram_id" value=""')
content = content.replace('value="Coffee, clean UI/UX & acoustic music"', 'value=""')
content = content.replace('value="Kochi Tech Campus"', 'value=""')
content = content.replace('<div class="char-counter" id="bioCounter">36 / 60</div>', '<div class="char-counter" id="bioCounter">0 / 60</div>')

# 3. Add JS functions for file upload & backend integration
js_additions = '''
function triggerPhotoUpload() {
  document.getElementById('photoFileInput').click();
}

function handlePhotoUpload(e) {
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = function(evt) {
    const dataUrl = evt.target.result;
    const img = document.getElementById('currentAvatarImg');
    const placeholder = document.getElementById('avatarPlaceholderIcon');
    img.src = dataUrl;
    img.style.display = 'block';
    if (placeholder) placeholder.style.display = 'none';
    currentAvatar = dataUrl;
    
    // Upload to local FastAPI backend
    fetch('http://localhost:8000/api/v1/users/upload-photo', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ image_base64: dataUrl, filename: file.name })
    }).then(r => r.json()).then(data => {
      if (data.photo_url) {
        currentAvatar = 'http://localhost:8000' + data.photo_url;
      }
    }).catch(err => console.log('Local fallback avatar'));
  };
  reader.readAsDataURL(file);
}
'''

if 'function triggerPhotoUpload()' not in content:
    content = content.replace('</script>', js_additions + '\n</script>')

with open("sync-app.html", "w", encoding="utf-8") as f:
    f.write(content)

print("sync-app.html updated successfully!")
