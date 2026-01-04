<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher.png" alt="Address Plugin Logo" width="128">
</p>

# Address ATAK Plugin

<p align="center">
  <a href="https://getgotak.com/activate">
    <img src="https://img.shields.io/badge/Download-Get%20Plugin-blue?style=for-the-badge" alt="Download">
  </a>
</p>

The Address Plugin extends ATAK with real-time geocoding capabilities, providing operators with immediate situational awareness of their current location in human-readable address format. The plugin displays the user's street address directly on the map interface, updating automatically as position changes while intelligently caching results to minimize network requests and conserve bandwidth in field conditions.

A dedicated search function allows users to query addresses and points of interest through the Nominatim geocoding service. Search results display location type indicators and can be navigated to directly from the results panel. The plugin maintains a search history for quick access to previously queried locations, improving operational efficiency during repeated missions in familiar areas.

The self-location widget utilizes ATAK's built-in GeocodeManager as the primary geocoding source, with automatic fallback to the Photon API when the primary service is unavailable. Address updates are triggered only when the user moves more than 50 feet from the last geocoded position, reducing unnecessary API calls while ensuring the displayed address remains accurate. All geocoded addresses are cached locally and persist across application restarts.

This plugin requires internet connectivity for geocoding operations. It integrates seamlessly with ATAK's existing map interface and does not require additional configuration beyond installation.

## Downloads

TPC-signed releases of the Address Plugin are available at https://getgotak.com/activate. All production builds distributed through this portal are digitally signed and verified for use with ATAK. The plugin will also be available on the Google Play Store in future releases.
