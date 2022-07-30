### Geocache Ayrıntıları

- Yeni: Yeniden tasarlanan koordinat hesaplayıcısı (fonksiyonları destekler)
- Change: Variables for waypoint calculation are now cache-global
- New: Variables tab in cache details
- New: Generating waypoints using formulas and variables with ranges
- New: Log templates for offline logs
- New: Add \[location\] to log template menu
- New: Allow selecting log texts
- Fix: GC checker link leading to loop in certain conditions on Android 12
- New: Added geochecker button at end of description text (when appropriate)
- New: Added 'log in browser' option to cache menu

### Geocache listesi

- New: Added option for "has user defined waypoints" to advanced status filter
- New: Allow inclusion of caches without D/T in filter
- Fix: Resort cache list on every location change on distance sort order

### Harita

- New: Map theming for Google Maps
- New: Map scaling options for OpenStreetMap (see theme options)
- Change: Settings => Map => Long tap on map will now enable/disable long tap in cache map as well (relevant for creating new waypoints for current cache)
- Change: Don't show distance-circle for archived caches
- Çözüldü: Belirli koşullarda OpenStreetMap haritalarında çökme
- Fix: Routing becoming unresponsive when many routing tiles are installed

### Genel

- New: Automatically perform backups (optional)
- Fix: Resume importing finished downloads
- New: Added configurable quick launch buttons to home screen, see Settings => Appearance
- New: Updated internal routing to BRouter v1.6.3
- New: Limit the need of repetitive back key usage by starting a new activity stack when changing to another part of the app
- New: Add setting to decrypt the cache hint by default (instead of only when tapping on it)
- New: Support setting caches from unknown source as found locally
- Removed: Geolutin trackable service as it was discontinued
