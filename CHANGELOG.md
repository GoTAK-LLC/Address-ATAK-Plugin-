# Changelog

## [1.2.0] - 2026-01-20

### Added
- **Saved Views Tab** - New tab alongside Search and Nearby to save and restore map camera positions
  - **Save Current View** captures full camera state:
    - Latitude/longitude center point
    - Map scale (zoom level)
    - Camera tilt (pitch angle)
    - Camera rotation (heading/azimuth)
    - 2D/3D mode detection
    - Thumbnail screenshot of current view
    - Reverse-geocoded address
  - **View Management**:
    - Navigate - Click any saved view to restore exact camera position
    - Rename - Customize view names with pencil icon
    - Delete - Remove unwanted views
  - **Persistent Storage** - Views saved to `/sdcard/atak/plugins/address/saved_views.json`
  - Up to 50 saved views supported
- **Custom URI Handler** (Experimental) - Navigate to coordinates via `addressview://navigate?lat=...&lon=...&zoom=...&tilt=...&rotation=...`

### New Files
- `SavedView.java` - Data model for saved views
- `ViewsManager.java` - CRUD operations and persistence
- `ViewsAdapter.java` - RecyclerView adapter for view cards
- `ViewNavigationActivity.java` - Custom URI handler
- `views_content.xml` - Views tab layout
- `view_card_item.xml` - View card layout

---

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
