## Beta Version 2021.05.11-RC

### Geocaching Services
- New: Added connector for Adventure Lab Caches - Shows starting coords of Adventure Labs with basic info on map and searches (PM-only). Use the link on the cache details page to start the Adventure Lab app to play the Adventure.

### Cache details
- New: Long click on waypoint coordinates to copy coordinates
- New: Export and import user defined caches with empty coordinates
- New: Support changing found state for user-defined caches and Lab Adventures
- New: Parse formula for waypoints in personal cache note
- New: Added indicator for calculated coordinates in waypoint list


### Map
- New: Automatic check for updates of downloaded map and theme files (optional)
- New: BRouter: Show info message on missing routing data
- New: Export individual route as track (in addition to "Export as route")

### Integrated routing engine
- New: Integrated BRouter routing engine - you can now use either external BRouter app or the integrated routing engine
- New: Integrated routing engine supports automatic download of missing routing tiles
- New: Integrated routing engine supports automatic updates of downloaded routing tiles
- New: Integrated routing engine supports selecting different routing profiles


### Other
- Change: "Sort individual route" automatically closes on saving and checks for unsaved changes on using the back arrow
- Fix: A couple of theming issues, esp. aligned theming of Google Maps and settings to "rest of app"
- Fix: Optimize global search: If no trackable with matching tracking code is found, execute online cache name search afterwards
- Fix: Avoid avatar images being displayed too wide and pushing the "Update / remove authorization" functionalty aside
- Fix: Fix conversion error in some distance settings for imperial units
- New: Debug view for pending downloads
- Fix: Directory selected by user not taken over in wizard on older devices
- Fix: Scan for map themes now run as background task on startup
- Fix: Changing map source via settings being recognized after full restart only
- Fix: Crash in "View settings" under certain conditions
- Fix: Back arrow in map downloader returning to main screen
- Fix: Avoid strange popup messages when attaching image to log
- Fix: Possible crash on map
