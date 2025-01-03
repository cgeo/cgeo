New: Integrated Wherigo player (beta) - see menu entry on home screen.<br> (You may want to [configure a quick launch item](cgeo-setting://quicklaunchitems_sorted) or [customize bottom navigation](cgeo-setting://custombnitem) for easier access, need to enable extended settings first.)

### Kort
- New: Store map theme per tile provider (UnifiedMap)
- New: Highlight selected cache/waypoint (UnifiedMap)
- New: Add separator between offline and online map sources
- New: Support Mapsforge as alternative to VTM in UnifiedMap, see [Settings => Map Sources => Unified Map](cgeo-setting://useMapsforgeInUnifiedMap)
- Change: 'Show elevation chart' moved to long tap menu (UnifiedMap)
- Change: Use new hillshading algorithm for Mapsforge offline maps
- New: Hillshading support for UnifiedMap Mapsforge offline maps
- New: Hillshading support for UnifiedMap VTM maps (requires online connection)
- Fix: Address search not considering live mode (UnifiedMap)
- Change: "follow my location" moved to the map, giving more space for "live mode" button
- Change: Make long-press pin more c:geo-like
- Change: Offline data management functions (download maps, check for missing routing / hillshading data) moved to map selection menu => "Manage offline data"
- Fix: Map not updating changed caches

### Cachedetaljer
- New: Not yet existing variables used in projection get created in variable list
- New: Allow large integers in formulas
- New: Support more constellations for variables in formulas
- Fix: Multiple images in personal note not added to images tab
- Fix: Handling of projections in waypoints and personal notes
- New: Long tap on date in logging retrieves previous log date
- Fix: Resetting cache to original coordinates does not remove "changed coordinates" flag
- New: Confirm overwriting log on quick offline log
- New: Update cache status on sending a log
- New: Colored HTML source view of cache details
- Fix: checksum(0) returning wrong value
- Fix: Editing logs removes "friends" status

### Generelt
- Change: Use elevation above mean sea level (if available, Android 14+ only)
- New: Allow multiple hierarchy levels in cache lists
- New: Dedicated icons for geocaching.com blockparty and HQ event types
- New: Set preferred image size for images loaded from geocaching.com caches and trackables
- Fix: "Open in browser" not working for trackable logs
- New: Option to manage downloaded files (maps, themes, routing and hillshading data)
- New: Option to remove a cache from all lists (= mark it as to be deleted)
- Fix: Reset coordinates not detected by c:geo for unsaved caches
- New: Allow clearing filter if no named filter is stored
- Fix: "Empty list" confirmation popping up when starting a pocket query download in newly created list
- Change: Owned caches with offline logs show offline log marker
- New: Configurable date format (eg.: cache logs), see [Settings => Appearance => Date format](cgeo-settings://short_date_format)
- New: Point connector info on home screen to connector-specific preference screen
- New: Additional emojis for cache icons
- Change: Cache type filter "Specials" includes events of types mega, giga, community celebration, HQ celebration, block party and maze
- Change: Cache type filter "Other" includes GCHQ, APE and unknown types
- Fix: History length and proximity settings sharing slider values
- Fix: Trackable log page showing time/coordinate input fields for trackables not supporting this
- Fix: Some crashes
