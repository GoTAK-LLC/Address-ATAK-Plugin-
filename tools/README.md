# Address Database Build Tools

This directory contains tools for building offline address and POI databases from OpenStreetMap data.

**Supports:**
- 🇺🇸 All US States
- 🌍 Worldwide countries/regions (via Geofabrik)
- 🏙️ Any city in the world (via Overpass API)
- 📁 Custom OSM/PBF files

## Prerequisites

### Windows

1. Install Python 3.8+ from https://www.python.org/downloads/
2. Install the required packages:

```powershell
pip install osmium requests
```

Note: On Windows, you may need to install Visual C++ Build Tools for osmium to compile properly.
If you have issues, try using the pre-built wheel:

```powershell
pip install osmium --only-binary :all:
```

### Linux / macOS

```bash
# Ubuntu/Debian
sudo apt install python3-pip libosmium2-dev

# macOS
brew install libosmium

# Then install Python packages
pip3 install osmium requests
```

## Quick Start

```bash
# US State
python build_state_db.py virginia

# Worldwide country
python build_state_db.py --region europe/germany

# Specific city
python build_state_db.py --city "Tokyo" --bbox 139.5,35.5,140.0,36.0
```

## Usage Examples

### US States

```bash
# Single state
python build_state_db.py virginia
python build_state_db.py california
python build_state_db.py new-york

# All US states
python build_state_db.py --all-us
```

### Worldwide Regions (Geofabrik)

Countries and large regions from Geofabrik's OSM extracts:

```bash
# Europe
python build_state_db.py --region europe/germany
python build_state_db.py --region europe/france
python build_state_db.py --region europe/great-britain
python build_state_db.py --region europe/italy
python build_state_db.py --region europe/ukraine

# Asia
python build_state_db.py --region asia/japan
python build_state_db.py --region asia/south-korea
python build_state_db.py --region asia/israel-and-palestine
python build_state_db.py --region asia/afghanistan
python build_state_db.py --region asia/iraq

# Middle East / Africa
python build_state_db.py --region africa/egypt
python build_state_db.py --region africa/morocco

# South America
python build_state_db.py --region south-america/brazil
python build_state_db.py --region south-america/colombia

# Oceania
python build_state_db.py --region australia-oceania/australia
python build_state_db.py --region australia-oceania/new-zealand

# List all available regions
python build_state_db.py --list-regions
```

### Cities (Overpass API)

For specific cities or small areas, use Overpass API with a bounding box:

```bash
# Format: --city "Name" --bbox west,south,east,north

# European cities
python build_state_db.py --city "London" --bbox -0.5,51.3,0.3,51.7
python build_state_db.py --city "Paris" --bbox 2.2,48.8,2.5,48.95
python build_state_db.py --city "Berlin" --bbox 13.1,52.3,13.7,52.7
python build_state_db.py --city "Rome" --bbox 12.35,41.8,12.6,42.0

# Asian cities
python build_state_db.py --city "Tokyo" --bbox 139.5,35.5,140.0,36.0
python build_state_db.py --city "Seoul" --bbox 126.8,37.4,127.2,37.7
python build_state_db.py --city "Dubai" --bbox 55.0,25.0,55.5,25.4

# Middle East
python build_state_db.py --city "Tel Aviv" --bbox 34.7,32.0,34.9,32.15
python build_state_db.py --city "Baghdad" --bbox 44.2,33.2,44.5,33.5

# Finding bounding boxes:
# 1. Go to https://www.openstreetmap.org
# 2. Navigate to your city
# 3. Click "Export" in the top menu
# 4. The bbox is shown: left (west), bottom (south), right (east), top (north)
```

**Note:** Overpass API has rate limits. For areas larger than ~100x100 km, use Geofabrik regions instead.

### Custom PBF/OSM Files

If you have your own OSM data file:

```bash
python build_state_db.py --file my-region.osm.pbf --name "My Region"
python build_state_db.py --file custom-extract.osm --name "Custom Area"
```

## Output

```
output/
├── virginia.db           # US state database
├── europe-germany.db     # Geofabrik region database  
├── city-london.db        # City database
├── manifest.json         # Metadata for all databases
```

```
cache/
├── us-virginia.osm.pbf   # Cached downloads (reused)
├── europe-germany.osm.pbf
└── city-london.osm       # Overpass downloads
```

## Database Schema (v2)

```sql
-- Main places table (for address text search)
CREATE TABLE places (
    id INTEGER PRIMARY KEY,
    osm_id INTEGER,
    osm_type TEXT,
    lat REAL,
    lon REAL,
    name TEXT,
    display_name TEXT,
    type TEXT,
    street TEXT,
    housenumber TEXT,
    city TEXT,
    postcode TEXT,
    state TEXT,
    country TEXT
);

-- FTS5 virtual table for fast text search
CREATE VIRTUAL TABLE places_fts USING fts5(
    name, display_name, street, city, postcode,
    content='places', content_rowid='id'
);

-- POI table (for spatial category search)
CREATE TABLE pois (
    id INTEGER PRIMARY KEY,
    osm_id INTEGER,
    osm_type TEXT,
    lat REAL,
    lon REAL,
    name TEXT,
    category TEXT,       -- e.g., 'HOSPITAL', 'GAS_STATION'
    address TEXT,
    phone TEXT,
    website TEXT,
    opening_hours TEXT
);

-- R*Tree spatial index for fast radius queries
CREATE VIRTUAL TABLE pois_rtree USING rtree(
    id,
    min_lat, max_lat,
    min_lon, max_lon
);
```

## POI Categories

The following POI categories are extracted for spatial (Nearby) search:

### Medical/Emergency
| Category | OSM Tags | Use Case |
|----------|----------|----------|
| HOSPITAL | amenity=hospital | Medical facilities |
| PHARMACY | amenity=pharmacy | Pharmacies |

### Emergency Services
| Category | OSM Tags | Use Case |
|----------|----------|----------|
| FIRE_STATION | amenity=fire_station | Fire services |
| POLICE_STATION | amenity=police | Law enforcement |

### Transportation
| Category | OSM Tags | Use Case |
|----------|----------|----------|
| AIRPORT | aeroway=aerodrome | Aviation |
| HELIPORT | aeroway=helipad | Helicopter landing |
| RAILWAY_STATION | railway=station | Rail transport |
| FERRY_TERMINAL | amenity=ferry_terminal | Water transport |
| PARKING | amenity=parking | Vehicle parking |

### Fuel/Finance
| Category | OSM Tags | Use Case |
|----------|----------|----------|
| GAS_STATION | amenity=fuel | Fuel/petrol |
| BANK | amenity=bank | Banking |
| ATM | amenity=atm | Cash machines |

### Surveillance/Security
| Category | OSM Tags | Use Case |
|----------|----------|----------|
| SURVEILLANCE | man_made=surveillance | CCTV cameras |

### Government/Diplomatic
| Category | OSM Tags | Use Case |
|----------|----------|----------|
| EMBASSY | amenity=embassy | Diplomatic missions |
| GOVERNMENT | office=government | Government offices |
| PRISON | amenity=prison | Correctional facilities |

### Infrastructure
| Category | OSM Tags | Use Case |
|----------|----------|----------|
| COMM_TOWER | man_made=tower | Communication towers |
| POWER_STATION | power=plant | Power generation |
| WATER_TOWER | man_made=water_tower | Water storage |
| POST_OFFICE | amenity=post_office | Postal services |

### Community
| Category | OSM Tags | Use Case |
|----------|----------|----------|
| SCHOOL | amenity=school | Education |
| LIBRARY | amenity=library | Public libraries |
| PLACE_OF_WORSHIP | amenity=place_of_worship | Churches, mosques, etc. |
| RESTAURANT | amenity=restaurant | Food |
| HOTEL | tourism=hotel | Lodging |

## Searching the Database

### Text Search (Addresses)

```sql
-- Simple search
SELECT * FROM places_fts WHERE places_fts MATCH 'richmond' LIMIT 10;

-- Search with ranking
SELECT p.*, bm25(places_fts) as rank
FROM places_fts
JOIN places p ON places_fts.rowid = p.id
WHERE places_fts MATCH 'main street'
ORDER BY rank
LIMIT 10;

-- Fuzzy search (prefix match)
SELECT * FROM places_fts WHERE places_fts MATCH 'rich*' LIMIT 10;
```

### Spatial POI Search (Nearby)

```sql
-- Find hospitals within bounding box using R*Tree
SELECT p.* 
FROM pois p
INNER JOIN pois_rtree r ON p.id = r.id
WHERE r.min_lat >= 37.4 AND r.max_lat <= 37.6
  AND r.min_lon >= -77.6 AND r.max_lon <= -77.4
  AND p.category = 'HOSPITAL'
LIMIT 50;
```

## Hosting the Databases

After building, upload to GitHub Releases:

1. Create a new repository: `atak-address-data`
2. Create a release (e.g., `v2024.01`)
3. Upload all `.db` files and `manifest.json` as release assets

The plugin will download from:
```
https://github.com/YOUR_USERNAME/atak-address-data/releases/latest/download/virginia.db
```

## File Sizes (Approximate)

| Region | Raw PBF | Database |
|--------|---------|----------|
| Virginia (US state) | 180 MB | 30-50 MB |
| California (US state) | 900 MB | 100-150 MB |
| Germany | 4 GB | 400-600 MB |
| Japan | 2 GB | 200-350 MB |
| London (city) | 50 MB | 10-20 MB |
| Tokyo (city) | 100 MB | 20-40 MB |

## How Offline Search Works in the Plugin

The Address plugin uses offline data with the following priority:

### Address Search (Search Tab)
1. **Offline databases first** - Instant results from downloaded .db files
2. **Nominatim API** - Fallback online geocoder

### Nearby POI Search (Nearby Tab)
1. **Offline databases first** - Uses R*Tree spatial index for fast radius queries
2. **Overpass API** - Online OpenStreetMap POI query

If offline data provides sufficient results (≥10), no network request is made.
