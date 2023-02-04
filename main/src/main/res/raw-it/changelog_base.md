### Mappa
- New: OSM map source osm.paws.cz
- New: Enable reading flopp.net GPX files as tracks
- Fix: Missing routing symbol after 'append to route'
- Fix: Missing route calculation for prepended points
- New: Add support for 'Voluntary MF5' OpenAndroMaps theme
- New: Add support for GeoJSON data
- Change: Use last known map position as fallback (when no GPS available and "follow my location" inactive)

### Dettagli del cache
- New: New more sophisticated image gallery
- Fix: Restore position in waypoint list after updating or deleting waypoint
- Fix: Move to bottom when creating new waypoint
- New: Recognize variables entered in waypoint user notes
- New: Display lab adventure button in mystery cache details if link to lab adventure detected
- Fix: Removal of waypoint description not synced for server-side waypoints
- Fix: Waypoint list not updated after scan

### Generale
- API level change (compileSDK 32)
- Update some dependant libraries
- Change: Use different Android mechanism to receive downloads (for better compatibility with Android 12+)
- New: Preset list name with GPX filename on import
- New: Allow import of GPX track files that do not provide a xmlns namespace tag
- New: Add monochrome launcher icon for Android 13
- New: Display geocaching.com member status on home screen
- Change: GPX-Import: Use name as geocode for 'unknown' connector
- Fix: Allow filtering for archived caches in owner search
- Fix: Line breaks sometimes missing in logbook view directly after posting a log
- Fix: Preview button displayed only with filter setting "show all" in PQ list
