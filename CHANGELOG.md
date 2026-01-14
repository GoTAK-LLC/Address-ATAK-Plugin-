# Changelog

## [1.1.0] - 2026-01-09

### Added
- **Natural language location search** - Search POIs and places by state:
  - `gas arkansas` - Find gas stations in Arkansas
  - `walmart texas` - Find Walmart stores in Texas  
  - `hospitals in virginia` - Find hospitals in Virginia
  - `gas norfolk virginia` - Find gas stations in/near Norfolk, Virginia
- Support for all 50 US states plus DC (names and abbreviations)
- City/location filtering within state searches
- POI-specific icons now display in search history

### Changed
- Nearby tab buttons match Address tab styling (consistent icons and layout)
- Location toggle color: orange → green (matches navigation icon)
- POI zoom level: 15 → 13 (more context when viewing results)
- Added spacing between marker address widget and widgets above it
- Release script excludes tools folder

### Fixed
- Search history displays correct POI icons instead of generic pins

---

## [1.0.x] - Previous Releases

### Major Features Added (1.0.4 → 1.1.0)

#### Core Search & Geocoding
- **Address Search** - Online search via Nominatim API
- **Offline Address Search** - Downloadable state databases with FTS5 full-text search
- **Self-Location Widget** - Shows your current street address on the map
- **Map Center Geocoding** - Shows address at crosshairs when enabled
- **Marker Selection Address** - Tap any marker to see its geocoded address
- **Search History** - Quick access to recent searches

#### Nearby POI Search
- **Nearby Tab** - Find points of interest near your location or map center
- **40+ POI Categories** - Hospitals, gas stations, restaurants, banks, airports, etc.
- **Radius Search** - Configurable 1-100km search radius
- **Offline POI Search** - R*Tree spatial indexing for fast offline queries
- **Custom POI Icons** - Category-specific icons from ATAK iconsets

#### Navigation & Markers
- **Bloodhound Navigation** - Navigate to any search result or POI
- **Marker Placement** - Drop tactical markers from search results
- **CoT Broadcast** - Markers broadcast to all connected TAK devices
- **MIL-STD-2525 Affiliations** - Friendly/Hostile/Neutral/Unknown marker types

#### Offline Data System
- **State Database Downloads** - Pre-built databases for all 50 US states
- **International Regions** - Europe, Asia, Americas, Africa, Oceania
- **GitHub Actions Builder** - Request new regions via GitHub Issues
- **Automatic Updates** - Check for and download database updates

#### Settings & Preferences
- Photon API fallback (optional)
- Custom icon toggle
- Search history toggle
- Widget visibility controls
