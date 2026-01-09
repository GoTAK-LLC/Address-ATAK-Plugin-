<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher.png" alt="Address Plugin Logo" width="128">
</p>

# Address ATAK Plugin

<p align="center">
  <a href="https://getgotak.com/activate">
    <img src="https://img.shields.io/badge/Download-Get%20Plugin-blue?style=for-the-badge" alt="Download">
  </a>
  <a href="https://github.com/GoTAK-LLC/Address-ATAK-Plugin/releases/tag/databases">
    <img src="https://img.shields.io/badge/Offline_Data-Download-green?style=for-the-badge" alt="Offline Data">
  </a>
</p>

The Address Plugin extends ATAK with powerful geocoding and POI search capabilities for both online and offline operations. Search addresses, find nearby points of interest, drop tactical markers, and navigate—all with or without internet connectivity.

## Key Features

### Address Search
- **Fuzzy text search** - Find addresses even with typos or partial matches
- **Online search** via Nominatim/Photon APIs
- **Offline search** with downloadable regional databases (FTS5 full-text search)
- Search history with quick access to recent locations
- Location type icons (city, building, road, etc.)

### Nearby POI Search
Find points of interest near your location or any map position:

| Category | POI Types |
|----------|-----------|
| **Medical** | Hospitals, Pharmacies, Clinics, Dentists |
| **Emergency** | Fire Stations, Police Stations |
| **Transport** | Airports, Heliports, Railway Stations, Ferries |
| **Services** | Gas Stations, Banks, ATMs, Post Offices |
| **Government** | Embassies, Government Buildings, Prisons |
| **Infrastructure** | Comm Towers, Cell Towers, Power Stations |
| **Food & Lodging** | Restaurants, Hotels, Cafes, Bars |
| **Shopping** | Supermarkets, Convenience Stores, Malls |

- **Radius search** - 1km to 50km range
- **Works offline** with R*Tree spatial indexing
- **Distance display** to each POI

### Marker Placement & CoT Dispatch
- Drop markers directly from search results
- **Markers broadcast as CoT** to all connected TAK devices
- **MIL-STD-2525 affiliation selection**:
  - **Friendly** (Blue)
  - **Hostile** (Red)
  - **Neutral** (Green)
  - **Unknown** (Yellow)
- Address included in marker remarks

### Bloodhound Navigation
- Navigate to any search result or POI
- Automatic marker placement at destination
- Supports VNS (Vehicle Navigation System) integration
- Works with ATAK's turn-by-turn navigation

### Self-Location Widget
- Shows your current street address on the map
- Updates automatically as you move (50ft threshold)
- Caches addresses across app restarts
- Single-tap to refresh, double-tap for settings

### Map Center Geocoding
- Shows address for map center crosshairs
- Active when ATAK's "Designate Map Centre" is enabled
- Yellow text display in bottom-left corner

### Marker Selection Address
- Tap any marker to see its geocoded address
- Displays in top-right corner in white text
- Shows street address, city, region, and country code
- Auto-hides after 10 seconds

## Offline Databases

Download pre-built databases for offline operation—no internet required after download.

### Available Regions

- **All 50 US States**
- **European Countries** - Germany, France, Spain, UK, Italy, etc.
- **Asian Countries** - Japan, South Korea, Taiwan, Philippines, etc.
- **Americas** - Canada, Mexico, Brazil, Argentina, etc.
- **Africa & Middle East** - Egypt, South Africa, Israel, Iraq, etc.
- **Oceania** - Australia, New Zealand

### Download Pre-Built Databases

Ready-to-use databases are available at:
**[Download Databases](https://github.com/GoTAK-LLC/Address-ATAK-Plugin/releases/tag/databases)**

Or download directly from within the plugin:
1. Open Address Plugin → **Offline Data**
2. Select your region
3. Tap **Download**

### Request a New Region

Anyone can request a database build via GitHub Issues:

1. Go to [Create New Issue](https://github.com/GoTAK-LLC/Address-ATAK-Plugin/issues/new/choose)
2. Select **"Request Database Build"**
3. Set the title to `Build: your-region` (e.g., `Build: europe/france`)
4. Submit—the build starts automatically!
5. You'll be notified when it's ready to download

**Example titles:**
- `Build: virginia`
- `Build: europe/spain`
- `Build: asia/japan`
- `Build: africa/egypt`

## Usage

### Address Search
1. Tap the search widget icon on the map
2. Type an address, city, or place name
3. Results appear with location type icons
4. Tap a result to pan/zoom to that location

### Nearby POI Search
1. Open the Address Plugin
2. Switch to the **Nearby** tab
3. Select POI categories (hospitals, gas stations, etc.)
4. Set search radius (1-50 km)
5. Tap **Search** to find nearby POIs

### Dropping Markers
1. Tap the pin icon on any search result
2. Select marker affiliation (Friendly/Hostile/Neutral/Unknown)
3. Marker is placed and **broadcast to all connected TAK devices**

### Navigation
1. Tap the navigate icon on any result
2. A marker is dropped at the destination
3. Bloodhound activates for navigation

## Settings

Access via: **ATAK Settings → Tool Preferences → Address Settings**

| Setting | Description |
|---------|-------------|
| **Show My Address** | Toggle self-location address widget |
| **Refresh Period** | How often to update address (seconds) |
| **Enable Photon API Fallback** | Use external API when ATAK geocoder fails |
| **Show Crosshairs Address** | Display address for map center |
| **Show Marker Address** | Display geocoded address when tapping markers |
| **Save Search History** | Remember recent searches |

## Building Databases Locally

For advanced users who want to build custom databases:

### Prerequisites

```bash
# Windows
pip install osmium requests

# Linux
sudo apt install python3-pip libosmium2-dev
pip3 install osmium requests

# macOS
brew install libosmium
pip3 install osmium requests
```

### Build Commands

```bash
cd tools/

# US State
python build_state_db.py virginia
python build_state_db.py california

# International Region
python build_state_db.py --region europe/germany
python build_state_db.py --region asia/japan

# City (via Overpass API)
python build_state_db.py --city "Tokyo" --bbox 139.5,35.5,140.0,36.0

# List available regions
python build_state_db.py --list-regions
```

### What's Included

Each database contains:
- **Places** - Addresses, cities, POIs for text search (FTS5 indexed)
- **POIs** - Categorized points of interest for spatial search (R*Tree indexed)
- **Metadata** - Region info, place counts, creation date

## Requirements

- ATAK 4.5.0 or later
- Internet connectivity for online search
- Downloaded databases for offline operation

## Downloads

TPC-signed releases are available at **[getgotak.com/activate](https://getgotak.com/activate)**. All production builds are digitally signed and verified for use with ATAK.

## Privacy

- **Self-location geocoding** - Uses ATAK's built-in geocoder by default
- **Photon API fallback** - Optional, sends coordinates to photon.komoot.io
- **Offline mode** - No data transmitted when using downloaded databases
- **Nearby search** - Uses Overpass API online, or local database offline

## License

Copyright © GoTAK LLC. All rights reserved.

---

<p align="center">
  <b>Made for operators who need reliable location intelligence, online or off.</b>
</p>
