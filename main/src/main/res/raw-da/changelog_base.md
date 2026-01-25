Edge to Edge: Due to Play Store policies we have updated the Android API level this version of c:geo targets + we have changed some of the screen layout routines. This may come with some unwanted side effects, especially on newer Android versions. If you experience any problems with this version of c:geo, please report either on [GitHub](https://github.com/cgeo/cgeo) or via email to [support@cgeo.org](mailto:support@cgeo.org)

Legacy Maps: As announced with 2025.07.17 and 2025.12.01 releases, we have finally removed the legacy implementations for our maps. You will be switched to our new UnifiedMap automatically and should notice no differences except a couple of new features, some of which are
- Map rotation for OpenStreetMap based maps (online + offline)
- Cluster popup for Google Maps
- Hide map sources you don't need
- Elevation chart for routes and tracks
- Switch between lists directly from map
- "Driving mode" for OpenStreetMap based maps
- Long-tap on track / individual route for further options

### Kort
- New: Route optimization caches calculated data
- New: Enabling live mode keeps waypoints of currently set target visible
- New: Long-tap on navigation line opens elevation chart (UnifiedMap)
- New: Show generated waypoints on map
- New: Download caches ordered by distance
- Fix: Doubling of individual route items
- New: Support for Motorider theme (VTM only)
- New: NoMap tile provider (don't show map, just caches etc.)
- Change: Max distance to connect points on history track lowered to 500m (configurable)

### Cachedetaljer
- New: Detect additional characters in formulas: –, ⋅, ×
- New: Preserve timestamp of own logs on refreshing a cache
- New: Optional compass mini view (see settings => cache details => Show direction in cache detail view)
- New: Show owners' logs on "friends/own" tab
- Change: "Friends/own" tab shows log counts for that tab instead of global counters
- Change: Improved header in variable and waypoint tabs
- Fix: Two "delete log" items shown
- Fix: c:geo crashing in cache details when rotating screen
- Change: More compact layout for "adding new waypoint"
- New: Option to load images for geocaching.com caches in "unchanged" size
- New: Variables view can be filtered
- New: Visualize calculated coordinates overflow in waypoint list
- New: Menu entry in waypoint list to mark certain waypoint types as visited
- New: Placeholders for trackable logging (geocache name, geocache code, user)
- Change: Removed the link to outdated WhereYouGo player. Integrated Wherigo player is now default for Wherigos.
- Fix: Missing quick toggle in guided mode of waypoint calculator

### Wherigo player
- New: Offline translation for Wherigos
- New: Improved button handling
- New: Status auto-save
- New: Option to create shortcout to Wherigo player on your mobile's home screen

### Generelt
- New: Share option after logging a cache
- Change: Do not show "needs maintenance" or "needs archived" options for own caches
- Fix: Restoring a backup may duplicate track files in internal storage and subsequent backups
- Change: Removed references to Twitter
- New: Delete orphaned trackfiles on clean up and restore backup
- New: Warning on trying to add too many caches to a bookmark list
- New: Watch/unwatch list functions
- New: Offer offline translation with Google Translate or DeepL apps (if installed)
- New: Delete items from search history
- Change: Remove GCVote (service discontinued)
- New: Colored toolbar on cache details pages
- New: Select multiple bookmark lists / pocket queries to download
- New: Preview bookmark lists
- Change: Increase minimum required Android version to Android 8
- New: Default quick buttons for new installations
- Fix: Titles in range input dialogs cut off
- Fix: Notification for nightly update points to regular APK even for FOSS variant
- New: "Ignore year" option for date filters
- New: Make remote URI clickable in pending downloads
- Change: Use system-settings as default theme for new installations
- New: GPX export: Write GSAK Lat/LonBeforeCorrect annotations when exporting original waypoints
- New: Show undo bar when deleting caches from list from map
