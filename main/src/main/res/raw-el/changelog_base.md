### UnifiedMap
Welcome to our all-new map implementation, internally called "UnifiedMap". This is the result of nearly two years' work from the c:geo team for an all-new map implementation. The reason we started this was that our old map implementations got increasingly hard to maintain and to keep (more or less) in sync feature-wise, with some code parts being ten years old (or more).

With UnifiedMap we tried to get the same user experience across all different map types (where possible), while modernizing and unifying the internal architecture.

UnifiedMap basically offers (nearly) all the features our old map implementations have, but gives you a couple of additional features:

- Map rotation for OpenStreetMap based maps (online and offline)
- Fractional scaling for OpenStreetMap based maps
- Cluster popup for Google Maps
- Hide map sources you don't need
- Elevation chart for routes and tracks (tap on route)
- Switch between lists directly from map (or by long-tapping on map icon)

UnifiedMap has reached beta state by now, thus we decided to make it our default map for all nightly users.

Everything should work, but there still may be (and will be) some bugs. In case of need you may switch between old and new map implementations (see settings - map sources), but we would really like you to try the new one. Please report any bugs you find on support ([support@cgeo.org](mailto:support@cgeo.org)) or [c:geo on GitHub](github.com/cgeo/cgeo/issues). Every feedback is welcome!

---

More changes:

### Χάρτης
- New: Highlight existing downloads in download manager
- New: Show cache's found state on waypoint icons
- New: Long-tap option to connect a cache with its waypoints by lines
- Change: Show cache/waypoint details in a non-blocking way
- New: Optionally keep temporary OAM files (map downloader, useful when using POI files with other apps)

### Λεπτομέρειες κρύπτης
- Change: Make "Toggle talking" an actual toggle
- Change: Increased maximum log length for geocaching.com
- Fix: Cannot upload longer personal notes on opencaching sites

### Γενικά
- New: Tapping on downloader notification opens "pending downloads" view
- Change: Using wallpaper as background no longer requires READ_EXTERNAL_STORAGE permission
- New: Two column-layout for settings in landscape mode
