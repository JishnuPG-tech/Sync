with open("sync-app.html", "r", encoding="utf-8") as f:
    html = f.read()

# 1. Add CSS for:
# - .radar-beacon-pill
# - .radar-empty-scan-state
# - .location-input-wrap, .btn-pick-map-inline, .location-preset-chips, .loc-chip, .location-verified-pill
# - #locationMapModal and interactive map canvas styles
CSS_ADDITIONS = """
    /* RADAR EMPTY SCANNING STATE (When no devices are nearby) */
    .radar-empty-scan-state {
      background: var(--surface);
      border: 1px dashed var(--line);
      border-radius: var(--radius-md);
      padding: 24px 18px;
      text-align: center;
      margin: 10px 0 16px 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      animation: fadeIn 0.3s ease;
    }

    .empty-radar-beacon {
      width: 56px;
      height: 56px;
      border-radius: 50%;
      background: var(--accent-subtle);
      color: var(--accent);
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      margin-bottom: 4px;
    }

    .beacon-wave {
      position: absolute;
      inset: -4px;
      border-radius: 50%;
      border: 1.5px solid var(--accent);
      opacity: 0.6;
      animation: beaconRipple 2.2s cubic-bezier(0.2, 0.8, 0.4, 1) infinite;
    }

    .beacon-wave.w2 {
      animation-delay: 1.1s;
    }

    @keyframes beaconRipple {
      0% { transform: scale(0.9); opacity: 0.8; }
      100% { transform: scale(1.6); opacity: 0; }
    }

    .radar-empty-scan-state h4 {
      font-family: var(--serif);
      font-size: 17px;
      font-weight: 500;
      color: var(--text);
    }

    .radar-empty-scan-state p {
      font-family: var(--sans);
      font-size: 12px;
      color: var(--muted);
      line-height: 1.45;
      max-width: 290px;
    }

    .btn-simulate-detect {
      background: var(--accent);
      color: #fff;
      border: none;
      padding: 9px 16px;
      border-radius: var(--radius-full);
      font-family: var(--sans);
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      margin-top: 6px;
      transition: all 0.2s ease;
    }

    .btn-simulate-detect:hover {
      background: var(--accent2);
      transform: translateY(-1px);
    }

    /* LOCATION INPUT WITH MAP INTEGRATION */
    .location-input-wrap {
      display: flex;
      align-items: center;
      position: relative;
    }

    .location-input-wrap .app-input {
      padding-right: 90px;
    }

    .btn-pick-map-inline {
      position: absolute;
      right: 6px;
      height: 34px;
      padding: 0 12px;
      background: var(--surface2);
      border: 1px solid var(--line);
      border-radius: var(--radius-sm);
      color: var(--accent);
      font-family: var(--sans);
      font-size: 11.5px;
      font-weight: 600;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      transition: all 0.2s ease;
    }

    .btn-pick-map-inline:hover {
      background: var(--accent-subtle);
      border-color: var(--accent);
    }

    .location-preset-chips {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
      margin-top: 8px;
    }

    .loc-chip {
      background: var(--surface);
      border: 1px solid var(--line);
      color: var(--muted);
      padding: 4px 10px;
      border-radius: var(--radius-full);
      font-family: var(--sans);
      font-size: 11px;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .loc-chip:hover {
      border-color: var(--accent);
      color: var(--text);
      background: var(--surface2);
    }

    .location-verified-pill {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      margin-top: 8px;
      padding: 4px 10px;
      background: var(--green-subtle);
      border: 1px solid rgba(107, 140, 114, 0.35);
      border-radius: var(--radius-full);
      color: var(--green);
      font-family: var(--mono);
      font-size: 10px;
      font-weight: 500;
    }

    /* INTERACTIVE MAP MODAL */
    .map-modal-sheet {
      width: 100%;
      background: var(--surface);
      border-radius: var(--radius-lg) var(--radius-lg) 0 0;
      padding: 14px 16px 28px 16px;
      border-top: 1px solid var(--line);
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      gap: 12px;
      max-height: 92%;
    }

    .map-header-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .map-modal-title {
      font-family: var(--serif);
      font-size: 20px;
      font-weight: 600;
      color: var(--text);
    }

    .map-canvas-container {
      width: 100%;
      height: 250px;
      background: #e9ecef;
      border-radius: var(--radius-md);
      position: relative;
      overflow: hidden;
      border: 1px solid var(--line);
      cursor: crosshair;
    }

    [data-theme="dark"] .map-canvas-container {
      background: #191c1e;
    }

    .map-svg-grid {
      width: 100%;
      height: 100%;
      display: block;
    }

    .map-pin-target {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -100%);
      pointer-events: none;
      transition: top 0.25s cubic-bezier(0.2, 0.9, 0.3, 1), left 0.25s cubic-bezier(0.2, 0.9, 0.3, 1);
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .map-pin-badge {
      background: var(--accent);
      color: #fff;
      font-family: var(--mono);
      font-size: 9.5px;
      font-weight: 600;
      padding: 2px 8px;
      border-radius: var(--radius-full);
      box-shadow: 0 2px 6px rgba(0,0,0,0.25);
      white-space: nowrap;
      margin-bottom: 2px;
    }

    .map-pin-icon-wrap {
      color: var(--accent);
      filter: drop-shadow(0 3px 6px rgba(0,0,0,0.3));
    }

    .map-pin-pulse-ring {
      width: 16px;
      height: 8px;
      border-radius: 50%;
      background: rgba(207, 112, 82, 0.3);
      border: 1px solid var(--accent);
      margin-top: -4px;
    }

    .map-info-card {
      background: var(--surface2);
      border: 1px solid var(--line);
      border-radius: var(--radius-md);
      padding: 10px 14px;
      display: flex;
      flex-direction: column;
      gap: 3px;
    }

    .map-info-title {
      font-family: var(--sans);
      font-size: 13px;
      font-weight: 600;
      color: var(--text);
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .map-info-coords {
      font-family: var(--mono);
      font-size: 10.5px;
      color: var(--muted);
    }

    .map-actions-row {
      display: flex;
      gap: 8px;
    }
"""

# Insert CSS additions right before </style>
html = html.replace("</style>", CSS_ADDITIONS + "\n</style>")

# 2. Update Location Input in Screen 3 (Setup)
OLD_LOCATION_BLOCK = """            <!-- Location to Add -->
            <div class="form-group">
              <label class="form-label" for="inputLocation">
                Location / Campus
                <span class="form-label-opt">Optional</span>
              </label>
              <div class="input-wrapper">
                <span class="input-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
                </span>
                <input type="text" id="inputLocation" class="app-input" placeholder="e.g. Kochi, Kerala" value="Kochi Tech Campus">
              </div>
            </div>"""

NEW_LOCATION_BLOCK = """            <!-- Location Input (Type or Select on Map) -->
            <div class="form-group">
              <label class="form-label" for="inputLocation">
                <span>Location / Campus</span>
                <span class="form-label-opt">Optional</span>
              </label>
              <div class="location-input-wrap">
                <span class="input-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
                </span>
                <input type="text" id="inputLocation" class="app-input" placeholder="Type location or select from map..." value="Kochi Tech Campus">
                <button type="button" class="btn-pick-map-inline" onclick="openLocationMapModal()" title="Select precise location on map">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"/><line x1="8" y1="2" x2="8" y2="18"/><line x1="16" y1="6" x2="16" y2="22"/></svg>
                  Map Pin
                </button>
              </div>

              <!-- Quick Location Preset Chips -->
              <div class="location-preset-chips">
                <button type="button" class="loc-chip" onclick="setLocationPreset('Infopark Campus, Kochi')">📍 Infopark</button>
                <button type="button" class="loc-chip" onclick="setLocationPreset('Maker Village, Kalamassery')">📍 Maker Village</button>
                <button type="button" class="loc-chip" onclick="setLocationPreset('Central Library Hub')">📍 Library</button>
                <button type="button" class="loc-chip" onclick="openLocationMapModal()">🗺️ Pick on Map</button>
              </div>

              <div id="locationVerifiedBadge" class="location-verified-pill" style="display: none;">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                <span id="locVerifiedText">Precision map coordinates locked</span>
              </div>
            </div>"""

html = html.replace(OLD_LOCATION_BLOCK, NEW_LOCATION_BLOCK)

# 3. Update the Radar Viewport in HTML to make pins dynamic:
# Replace hardcoded pins with dynamic container
OLD_RADAR_PINS = """                    <!-- Orbiting Peer Pins -->
                    <div class="radar-peer-pin pin-1" title="Arun K" onclick="openPersonPreview(0)">
                      <img src="assets/portraits/portrait_02.jpg" alt="Arun K">
                    </div>
                    <div class="radar-peer-pin pin-2" title="Priya M" onclick="openPersonPreview(1)">
                      <img src="assets/portraits/portrait_03.jpg" alt="Priya M">
                    </div>
                    <div class="radar-peer-pin pin-3" title="Jamie M" onclick="openPersonPreview(2)">
                      <img src="assets/portraits/portrait_04.jpg" alt="Jamie M">
                    </div>
                    <div class="radar-peer-pin pin-4" title="Sara R" onclick="openPersonPreview(3)">
                      <img src="assets/portraits/portrait_01.jpg" alt="Sara R">
                    </div>"""

NEW_RADAR_PINS = """                    <!-- Orbiting Peer Pins (Only displayed when devices are detected nearby) -->
                    <div id="radarDynamicPins"></div>"""

html = html.replace(OLD_RADAR_PINS, NEW_RADAR_PINS)

# 4. Add the Interactive Map Picker Modal sheet to the modals section
MAP_MODAL_HTML = """
      <!-- ==============================================
           MODAL 3: PRECISE MAP LOCATION PICKER
           ============================================== -->
      <div class="modal-overlay" id="locationMapModal" onclick="closeModalOnBackdrop(event, 'locationMapModal')">
        <div class="map-modal-sheet">
          <div class="sheet-drag-handle"></div>
          
          <div class="map-header-row">
            <h3 class="map-modal-title">Select Precise Location</h3>
            <button class="icon-btn" onclick="closeModal('locationMapModal')" title="Close">✕</button>
          </div>

          <!-- Stylized Interactive Vector Map -->
          <div class="map-canvas-container" id="mapCanvasContainer" onclick="handleMapClick(event)">
            <svg class="map-svg-grid" viewBox="0 0 400 250" preserveAspectRatio="none">
              <!-- Water body / Canal -->
              <path d="M 0 180 Q 120 160 220 190 T 400 170 L 400 250 L 0 250 Z" fill="rgba(107, 140, 114, 0.18)" />
              <path d="M 0 185 Q 120 165 220 195 T 400 175" stroke="#6b8c72" stroke-width="2" fill="none" opacity="0.4"/>
              
              <!-- Green Park Zones -->
              <rect x="20" y="25" width="75" height="55" rx="10" fill="rgba(107, 140, 114, 0.12)" stroke="rgba(107, 140, 114, 0.25)" />
              <rect x="290" y="30" width="85" height="60" rx="10" fill="rgba(107, 140, 114, 0.12)" stroke="rgba(107, 140, 114, 0.25)" />
              
              <!-- Main Road Network Grid -->
              <line x1="0" y1="110" x2="400" y2="110" stroke="#d9d5cc" stroke-width="12" />
              <line x1="0" y1="110" x2="400" y2="110" stroke="#fbfaf6" stroke-width="8" />
              <line x1="160" y1="0" x2="160" y2="250" stroke="#d9d5cc" stroke-width="12" />
              <line x1="160" y1="0" x2="160" y2="250" stroke="#fbfaf6" stroke-width="8" />
              
              <line x1="60" y1="0" x2="60" y2="180" stroke="#d9d5cc" stroke-width="6" />
              <line x1="260" y1="0" x2="260" y2="190" stroke="#d9d5cc" stroke-width="6" />
              <line x1="0" y1="50" x2="400" y2="50" stroke="#d9d5cc" stroke-width="6" />

              <!-- Campus Buildings -->
              <rect x="180" y="25" width="60" height="30" rx="4" fill="#cf7052" opacity="0.15" />
              <rect x="180" y="65" width="60" height="35" rx="4" fill="#cf7052" opacity="0.25" />
              <rect x="80" y="130" width="65" height="40" rx="4" fill="#cf7052" opacity="0.18" />
              <rect x="180" y="130" width="85" height="45" rx="4" fill="#cf7052" opacity="0.28" />

              <text x="185" y="44" font-family="'DM Mono', monospace" font-size="8" fill="#cf7052">Maker Village</text>
              <text x="185" y="154" font-family="'DM Mono', monospace" font-size="8.5" fill="#9f4e37">Tech Zone 2</text>
              <text x="30" y="55" font-family="'DM Mono', monospace" font-size="8" fill="#6b8c72">Campus Lawn</text>
            </svg>

            <!-- Movable Target Pin -->
            <div class="map-pin-target" id="mapTargetPin" style="top: 52%; left: 54%;">
              <div class="map-pin-badge" id="mapPinBadge">10.054° N, 76.353° E</div>
              <div class="map-pin-icon-wrap">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="#cf7052" stroke="#ffffff" stroke-width="1.5">
                  <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"/>
                  <circle cx="12" cy="9" r="2.5" fill="#ffffff"/>
                </svg>
              </div>
              <div class="map-pin-pulse-ring"></div>
            </div>
          </div>

          <!-- Location Info Box -->
          <div class="map-info-card">
            <div class="map-info-title">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
              <span id="selectedMapAddressName">Maker Village, Technology Innovation Zone</span>
            </div>
            <div class="map-info-coords" id="selectedMapCoordsText">Kalamassery, Kochi • Lat: 10.0542° N, Long: 76.3538° E (±2m GPS)</div>
          </div>

          <div class="map-actions-row">
            <button type="button" class="btn-secondary" style="flex: 1;" onclick="useCurrentGpsLocation()">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 19 21 12 17 5 21 12 2"/></svg>
              Current GPS
            </button>
            <button type="button" class="btn-primary" style="flex: 1.6;" onclick="confirmMapLocation()">
              Confirm Location
            </button>
          </div>
        </div>
      </div>
"""

html = html.replace('<div class="modal-overlay" id="personPreviewModal"', MAP_MODAL_HTML + '\n      <div class="modal-overlay" id="personPreviewModal"')

# 5. Update JavaScript functions for dynamic radar rendering and map picker
JS_MAP_AND_DYNAMIC_RADAR = """
    // MAP PICKER LOGIC
    let currentSelectedLocation = "Maker Village, Kalamassery, Kochi";
    let currentSelectedCoords = "10.0542° N, 76.3538° E";

    function openLocationMapModal() {
      openModal('locationMapModal');
    }

    function setLocationPreset(presetText) {
      document.getElementById('inputLocation').value = presetText;
      const badge = document.getElementById('locationVerifiedBadge');
      const text = document.getElementById('locVerifiedText');
      badge.style.display = 'inline-flex';
      text.innerText = `Preset selected: ${presetText.split(',')[0]}`;
      showToast(`Selected ${presetText}`);
    }

    function handleMapClick(e) {
      const container = document.getElementById('mapCanvasContainer');
      const rect = container.getBoundingClientRect();
      const x = Math.max(10, Math.min(rect.width - 10, e.clientX - rect.left));
      const y = Math.max(10, Math.min(rect.height - 10, e.clientY - rect.top));

      const pctX = ((x / rect.width) * 100).toFixed(1);
      const pctY = ((y / rect.height) * 100).toFixed(1);

      const targetPin = document.getElementById('mapTargetPin');
      targetPin.style.left = `${pctX}%`;
      targetPin.style.top = `${pctY}%`;

      // Generate realistic coordinates based on click
      const lat = (10.0400 + (y / rect.height) * 0.0300).toFixed(4);
      const lng = (76.3400 + (x / rect.width) * 0.0400).toFixed(4);
      currentSelectedCoords = `${lat}° N, ${lng}° E`;

      let spotName = "Maker Village Innovation Hub";
      if (pctX < 35 && pctY < 50) spotName = "Campus North Lawn & Quad";
      else if (pctX > 65 && pctY < 50) spotName = "East Academic Block A";
      else if (pctY > 65) spotName = "Waterfront Promenade, Kochi";
      else spotName = "Technology Innovation Zone, Kochi";

      currentSelectedLocation = `${spotName} (${currentSelectedCoords})`;
      document.getElementById('selectedMapAddressName').innerText = spotName;
      document.getElementById('selectedMapCoordsText').innerText = `Kochi • Lat: ${lat}° N, Long: ${lng}° E (±2m precision)`;
      document.getElementById('mapPinBadge').innerText = `${lat}° N, ${lng}° E`;
    }

    function useCurrentGpsLocation() {
      const targetPin = document.getElementById('mapTargetPin');
      targetPin.style.left = '50%';
      targetPin.style.top = '48%';
      currentSelectedLocation = "Kochi Tech Campus (10.0542° N, 76.3538° E)";
      document.getElementById('selectedMapAddressName').innerText = "Kochi Tech Campus";
      document.getElementById('selectedMapCoordsText').innerText = "Live device GPS fix • Accuracy: High (±1.8m)";
      document.getElementById('mapPinBadge').innerText = "GPS Live Fix";
      showToast("📍 Acquired high-precision device GPS");
    }

    function confirmMapLocation() {
      const input = document.getElementById('inputLocation');
      input.value = currentSelectedLocation;
      const badge = document.getElementById('locationVerifiedBadge');
      const text = document.getElementById('locVerifiedText');
      badge.style.display = 'inline-flex';
      text.innerText = `Precision Map Verified: ${currentSelectedLocation.split('(')[0]}`;
      closeModal('locationMapModal');
      showToast("📍 Precise location attached to profile");
    }

    // DYNAMIC RADAR PEERS RENDERING
    // The person's profile on the radar ONLY shows when a device is detected nearby!
    function renderDynamicRadarPins(activePeers) {
      const pinContainer = document.getElementById('radarDynamicPins');
      if (!pinContainer) return;

      if (activePeers.length === 0) {
        pinContainer.innerHTML = '';
        return;
      }

      // Pre-calculated orbital coordinates for detected peers
      const orbitPositions = [
        { top: '34%', left: '76%', ring: 2 }, // Pin 1
        { top: '74%', left: '32%', ring: 2 }, // Pin 2
        { top: '24%', left: '26%', ring: 3 }, // Pin 3
        { top: '72%', left: '72%', ring: 3 }  // Pin 4
      ];

      let pinsHtml = '';
      activePeers.forEach((peer, i) => {
        const pos = orbitPositions[i % orbitPositions.length];
        const peerIdx = peers.findIndex(p => p.id === peer.id);
        pinsHtml += `
          <div class="radar-peer-pin pin-dynamic" 
               style="top: ${pos.top}; left: ${pos.left}; animation: pulsePin 2.5s ease-in-out infinite;" 
               title="${peer.name} (${peer.distanceBadge})" 
               onclick="openPersonPreview(${peerIdx})">
            <img src="${peer.avatar}" alt="${peer.name}">
          </div>
        `;
      });

      pinContainer.innerHTML = pinsHtml;
    }
"""

html = html.replace('// USERNAME STRICT VALIDATION', JS_MAP_AND_DYNAMIC_RADAR + '\n    // USERNAME STRICT VALIDATION')

# 6. Update renderPeerCards to handle empty radar scan state vs detected devices
OLD_RENDER_PEERS = """      radarFeed.innerHTML = radarHtml || '<div style="text-align:center; padding:24px; color:var(--muted); font-size:12.5px;">No peers currently in radar range. Try switching to Extended Mode.</div>';
      discoveredFeed.innerHTML = discoveredHtml;

      document.getElementById('radarNearbyCount').innerText = `${countActive} active`;
      document.getElementById('radarSummaryCount').innerText = `${countActive} people in radar`;
      document.getElementById('countAll').innerText = peers.filter(p => !p.isRemoved).length;
      document.getElementById('countInRadar').innerText = countActive;
      document.getElementById('countOutRadar').innerText = countOut;"""

NEW_RENDER_PEERS = """      const activeRadarPeers = peers.filter(p => p.inRadar && !p.isRemoved);
      renderDynamicRadarPins(activeRadarPeers);

      if (countActive === 0) {
        radarFeed.innerHTML = `
          <div class="radar-empty-scan-state">
            <div class="empty-radar-beacon">
              <span class="beacon-wave"></span>
              <span class="beacon-wave w2"></span>
              <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/><line x1="12" y1="2" x2="12" y2="5"/><line x1="12" y1="19" x2="12" y2="22"/></svg>
            </div>
            <h4>Looking for nearby Sync peers</h4>
            <p>Profiles only appear when another Sync user enters your physical proximity. Bluetooth LE scanning active.</p>
            <button type="button" class="btn-simulate-detect" onclick="simulateNewNearbyPeer()">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
              Simulate Device Detected Nearby
            </button>
          </div>
        `;
        document.getElementById('radarSummaryCount').innerText = 'Scanning nearby frequencies...';
        const subcap = document.querySelector('.radar-subcaption');
        if (subcap) subcap.innerText = 'Profiles appear only when a device is detected nearby';
      } else {
        radarFeed.innerHTML = radarHtml;
        document.getElementById('radarSummaryCount').innerText = `${countActive} ${countActive === 1 ? 'person' : 'people'} in radar`;
        const subcap = document.querySelector('.radar-subcaption');
        if (subcap) subcap.innerText = 'Profiles persist in your feed even when out of range';
      }

      discoveredFeed.innerHTML = discoveredHtml;
      document.getElementById('radarNearbyCount').innerText = `${countActive} active`;
      document.getElementById('countAll').innerText = peers.filter(p => !p.isRemoved).length;
      document.getElementById('countInRadar').innerText = countActive;
      document.getElementById('countOutRadar').innerText = countOut;"""

html = html.replace(OLD_RENDER_PEERS, NEW_RENDER_PEERS)

# Add "Clear Radar" simulator button to toolbar
OLD_TOOLBAR = """        <button class="tool-btn" onclick="simulateIncomingRequest()">📩 Request</button>
      </div>"""
NEW_TOOLBAR = """        <button class="tool-btn" onclick="simulateIncomingRequest()">📩 Request</button>
        <button class="tool-btn" onclick="toggleRadarDetection()" id="btnToggleRadar" style="color: #6b8c72;">📡 Toggle Detection</button>
      </div>"""
html = html.replace(OLD_TOOLBAR, NEW_TOOLBAR)

# Add toggleRadarDetection function
html = html.replace("function simulateNewNearbyPeer() {", """
    function toggleRadarDetection() {
      const anyInRadar = peers.some(p => p.inRadar && !p.isRemoved);
      if (anyInRadar) {
        peers.forEach(p => p.inRadar = false);
        showToast("All devices left radar range (Testing empty scan state)");
      } else {
        peers[0].inRadar = true;
        peers[1].inRadar = true;
        showToast("⚡ 2 nearby devices detected via Bluetooth LE");
      }
      renderPeerCards();
    }

    function simulateNewNearbyPeer() {""")

with open("sync-app.html", "w", encoding="utf-8") as f:
    f.write(html)

print("Batch 2 applied successfully!")
