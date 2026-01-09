<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher.png" alt="Address Plugin Logo" width="128">
</p>

# Address ATAK Plugin

<p align="center">
  <a href="https://getgotak.com/activate">
    <img src="https://img.shields.io/badge/Download-Get%20Plugin-blue?style=for-the-badge" alt="Download">
  </a>
</p>

The Address Plugin extends ATAK with powerful geocoding capabilities for both online and offline operations. It provides real-time location awareness, address search, marker placement, and navigation integration.

## Features

### 🏠 Self-Location Address Display
- Shows your current street address above your callsign on the map
- Updates automatically as you move (50ft threshold to conserve bandwidth)
- Caches addresses across app restarts
- Single-tap to refresh, double-tap to open settings

### 🔍 Address Search
- Search for addresses, POIs, and locations using Nominatim/Photon APIs
- **Offline search** with downloadable state databases
- Search history with quick access to recent locations
- Location type icons (city, building, road, etc.)

### 📍 Marker Placement
- Drop markers directly from search results
- **MIL-STD-2525 marker type selection**:
  - 🔵 **Friendly** (Blue Rectangle)
  - 🔴 **Hostile** (Red Diamond)
  - 🟢 **Neutral** (Green Square)
  - 🟡 **Unknown** (Yellow Quatrefoil)
- Markers include address in remarks field

### 🧭 Bloodhound Navigation
- Navigate to any search result using ATAK's Bloodhound tool
- Automatic marker placement at destination
- Supports VNS (Vehicle Navigation System) integration
- Works with turn-by-turn navigation

### 🗺️ Map Center Crosshairs Geocoding
- Shows geocoded address for map center crosshairs
- Only active when ATAK's "Designate Map Centre" is enabled
- Displays in bottom-left corner (yellow text)

### 📴 Offline Geocoding
- Download state/region databases for offline operation
- Full-text search with fuzzy matching
- No internet required after database download
- Automatic fallback between online and offline sources

## Usage

### Opening the Search Panel
Tap the search widget icon on the map to open the address search panel.

### Searching for Locations
1. Type an address, city, or place name in the search box
2. Results appear below with location type icons
3. Tap a result to pan/zoom to that location

### Dropping a Marker
1. Tap the 📍 pin icon on any search result
2. Select the marker type (Friendly/Hostile/Neutral/Unknown)
3. Marker is placed with the address as the title

### Navigating with Bloodhound
1. Tap the ➡️ navigate icon on any search result
2. A marker is dropped at the destination
3. Bloodhound activates automatically for navigation

### Using Offline Mode
1. Tap "📥 Offline Data" button in the search panel
2. Select your state/region to download
3. Once downloaded, searches work without internet

## Settings

Access plugin settings via: **ATAK Settings → Tool Preferences → Address Settings**

| Setting | Description |
|---------|-------------|
| **Show My Address** | Toggle self-location address widget |
| **Refresh Period** | How often to update address (seconds) |
| **Enable Photon API Fallback** | Use external API when ATAK geocoder fails |
| **Show Crosshairs Address** | Display address for map center |
| **Save Search History** | Remember recent searches |

## Offline Database Generation

For disconnected operations, you can generate offline address databases from OpenStreetMap data.

### Quick Start

```bash
cd tools/
pip install osmium requests
python build_state_db.py virginia
```

### Prerequisites

**Windows:**
```powershell
pip install osmium requests
```

**Linux/macOS:**
```bash
# Ubuntu/Debian
sudo apt install python3-pip libosmium2-dev
pip3 install osmium requests

# macOS
brew install libosmium
pip3 install osmium requests
```

### Building Databases

**Single State:**
```bash
python build_state_db.py virginia
```

**All States:**
```bash
python build_state_db.py --all
```

### Output Files

| File | Description |
|------|-------------|
| `output/virginia.db` | SQLite database with FTS5 search |
| `output/manifest.json` | Metadata for available regions |
| `cache/*.osm.pbf` | Cached downloads for rebuilds |

### Database Sizes (Approximate)

| State | Download | Database |
|-------|----------|----------|
| Virginia | 180 MB | 30-50 MB |
| California | 900 MB | 100-150 MB |
| Texas | 500 MB | 80-120 MB |
| Wyoming | 30 MB | 5-10 MB |

### Hosting Your Own Databases

1. Build databases using the tools
2. Create a GitHub repository (e.g., `atak-address-data`)
3. Create a release and upload `.db` files + `manifest.json`
4. Plugin downloads from: `https://github.com/USERNAME/atak-address-data/releases/latest/download/`

See `tools/README.md` for detailed database schema and query examples.

## Requirements

- ATAK 4.5.0 or later
- Internet connectivity for online geocoding
- Downloaded databases for offline operation

## Downloads

TPC-signed releases are available at https://getgotak.com/activate. All production builds are digitally signed and verified for use with ATAK.

## Privacy

- **Self-location geocoding**: Uses ATAK's built-in geocoder by default
- **Photon API fallback**: Optional, sends coordinates to photon.komoot.io
- **Offline mode**: No data transmitted when using downloaded databases
