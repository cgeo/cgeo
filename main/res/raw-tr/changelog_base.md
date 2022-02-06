### Genel
- Change: Introducing bottom navigation for direct access to c:geo's most-used screens, replacing the old mainscreen

### Harita
- Fix: On loading GPX files containing multiple tracks display them as separate, unconnected tracks
- Change: Automatically enable track display on loading a GPX track file
- New: Allow displaying several tracks at once
- New: D/T symbols for cache icons (optional)
- New: Option to check for missing routing data for current viewport
- New: Theme legend for Elevate, Elements and Freizeitkarte themes
- Fix: Reenable routing with external BRouter app in version 1.6.3
- Fix: Avoid map duplication by map downloader in certain conditions

### Geocache listesi
- Yeni: Sonraki 20 Geocache'i seçebilme seçeneği
- New: Attributes overview (see Manage Caches => Attributes overview)
- New: Add import from bookmark lists (GC premium only)
- New: Invert sort-order on long click on sort bar
- Change: Also perform automatic sorting by distance for lists containing cache series with more than 50 caches (up to 500)
- Fix: Use a shorter timeout for fast scrolling mechanism for less interference with other layout elements

### Geocache Ayrıntıları
- New: Pass current cache coordinates to geochecker (if supported by geochecker)
- New: Colored attribute icons (following attribute groups)
- Fix: Problem opening pictures from gallery tab in external apps on some Samsung devices
- Fix: Missing log count (website change)

### Diğer
- New: Quick-load geocodes from clipboard text in mainscreen search
- New: Added support for user-defined log templates
- New: Make Settings => View Settings filterable
- New: Enable search in preferences
- New: Added GC Wizard to useful apps list
- New: Attributes filter: Allow selecting from which connectors attributes are shown
- New: Option to limit distance in nearby search (see Settings => Services)
- Change: Removed barcode scanner from useful apps list and from mainscreen
- Change: Removed BRouter from useful apps list (you can still use both external and internal navigation)
- Fix: Avoid repeated update checks for maps/routing tiles with interval=0
- Fix: Optimize support to autofill passwords from external password store apps in settings
- Fix: Enable tooltips for systems running on Android below version 8
- Refactored settings to meet current Android specifications
- Updated MapsWithMe API

