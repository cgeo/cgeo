Due to Play Store policies we have updated the Android API level this version of c:geo targets + we have changed some of the screen layout routines. This may come with some unwanted side effects, especially on newer Android versions. If you experience any problems with this version of c:geo, please report either on [GitHub](https://github.com/cgeo/cgeo) or via email to [support@cgeo.org](mailto:support@cgeo.org)

### Map
- New: Route optimization caches calculated data
- New: Enabling live mode keeps waypoints of currently set target visible
- New: Long-tap on navigation line opens elevation chart (UnifiedMap)
- New: Show generated waypoints on map
- New: Download caches ordered by distance
- Fix: Doubling of individual route items
- New: Support for Motorider theme (VTM only)
- New: Support for transparent background display of offline maps (VTM only)

### Cache details
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

### Wherigo player
- New: Offline translation for Wherigos
- New: Improved button handling

### General
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
