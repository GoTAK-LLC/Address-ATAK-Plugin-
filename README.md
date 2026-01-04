# Address Plugin for ATAK

The Address Plugin provides geocoding functionality for the Android Team Awareness Kit (ATAK). Users can search for addresses, cities, and points of interest worldwide using the integrated search panel, which leverages the Photon and Nominatim geocoding APIs for accurate and typo-tolerant results. Search results are displayed with contextual icons based on location type, and selecting a result navigates the map to that location. A search history feature allows users to revisit previous searches, navigate to saved locations, or launch external navigation apps like Google Maps for turn-by-turn directions.

The plugin also includes a local geocoding feature that displays the user's current address above their callsign on the map. This reverse geocoding functionality updates periodically based on configurable preferences, providing real-time location context without manual input. Both the self-location display and search history features can be enabled or disabled through the plugin's settings panel.

## Building

This plugin requires the ATAK SDK and follows the standard ATAK plugin build process. Run `./install-plugin.sh` from the project root to build and deploy to a connected device. The plugin supports both local development builds and the TAK third-party build pipeline for official distribution.
