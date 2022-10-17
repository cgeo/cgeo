# Full changelog
This changelog contains all changes which are not intermediate developing steps. It is more detailed than the changelogs we publish in our releases.

<!-- --------------------------------------------------------------------------------- --->

## Next release (branch `release`)

<!-- --------------------------------------------------------------------------------- --->

## 2022.10.17 Feature Release

### Map
- New: Add support for hillshading on OSM
- Fix: Manually copied waypoints of a cache not all being displayed

### Cache details
- Change: Show loading indicator in log activity while retrieving required data is ongoing
- Fix: nbsp; not considered as space in formula parsing
- Fix: Extra waypoints created on personal note formula parsing
- Fix: Variables not created on copying formula to different cache
- Fix: (Experimental gallery) Use image cache for log/spoiler images
- New: Improvements to coordinate scan in texts

### General
- New: Support generic downloads for "mf-v4-map"- and "mf-theme"-prefixed links (no automatic updates supported)
- New: Maintenance function reindexes database
- New: Automatic performance optimization of database every 90 days (reindex)
- Fix: Handle missing fine location permission
- Change: Website language switch removed
- Fix: Deleted user-defined caches are not deleted on c:geo restart
- Fix: Filter bar not readable in light mode

<!-- --------------------------------------------------------------------------------- --->

## 2022.09.04 Bugfix Release

- Fix: Crash on logging "needs maintenance" => use "report problem" options instead
- Fix: Crashes under certain conditions in map and cache list
- Fix: Crash on 'Add to bookmark' in debug mode
- Fix: Clear icon being shown on name edit for system waypoints
- Fix: Decimal values separated by = being parsed as coords

<!-- --------------------------------------------------------------------------------- --->

## 2022.08.21 Feature Release

### Map
- Fix: Do not show distance circles for waypoints of archived caches
- New: Tap on free map space to create waypoint or user-defined cache, append or prepend to individual route, display coordinates or navigate (depending on context)
- New: Option to delete offline maps
- Fix: Lab adventure items in individual routes not recognized as such after edit

### Cache details
- Change: Remove size-limit check while storing waypoints in personal notes
- Fix: Wrong checksum formula results on large numbers

### General
- New: Download / update caches in background
- New: Copy current coordinates when long-tapping location on home screen
- Fix: Missing trackable count in search results
- New: Navigation method "Other external apps (coords only)" to invoke external navigation app with just coordinates (helps with Here WeGo in offline mode)
- New: Posibility to build nested filters
- New: Display Lab adventure mode (random / linear)
- New: Support configuring settings from app info window
- Fix: Support selecting the same value for d/t filter range

<!-- --------------------------------------------------------------------------------- --->

## 2022.07.31 Bugfix Release

- Fix: Filter settings lost on screen rotation when mapping an address search result
- Fix: Double tap required to edit offline log
- Fix: Exceptions in CheckerUtils, internal routing service and WhereYouGo connector
- New: Added last geocaching.com login status to status page
- Changed: Misleading maps.me dialog

<!-- --------------------------------------------------------------------------------- --->

## 2022.06.06 Feature Release

### Cache details

- New: Redesigned coordinates calculator (supporting functions)
- Change: Variables for waypoint calculation are now cache-global
- New: Variables tab in cache details
- New: Generating waypoints using formulas and variables with ranges
- New: Log templates for offline logs
- New: Add \[location\] to log template menu
- New: Allow selecting log texts
- Fix: GC checker link leading to loop in certain conditions on Android 12
- New: Added geochecker button at end of description text (when appropriate)
- New: Added 'log in browser' option to cache menu

### Cache list

- New: Added option for "has user defined waypoints" to advanced status filter
- New: Allow inclusion of caches without D/T in filter
- Fix: Resort cache list on every location change on distance sort order

### Map

- New: Map theming for Google Maps
- New: Map scaling options for OpenStreetMap (see theme options)
- Change: Settings => Map => Long tap on map will now enable/disable long tap in cache map as well (relevant for creating new waypoints for current cache)
- Change: Don't show distance-circle for archived caches
- Fix: Crash in OpenStreetMap maps under certain conditions
- Fix: Routing becoming unresponsive when many routing tiles are installed

### General

- New: Automatically perform backups (optional)
- Fix: Resume importing finished downloads
- New: Added configurable quick launch buttons to home screen, see Settings => Appearance
- New: Updated internal routing to BRouter v1.6.3
- New: Limit the need of repetitive back key usage by starting a new activity stack when changing to another part of the app
- New: Add setting to decrypt the cache hint by default (instead of only when tapping on it)
- New: Support setting caches from unknown source as found locally
- Removed: Geolutin trackable service as it was discontinued

<!-- --------------------------------------------------------------------------------- --->

## 2022.03.10 Bugfix Release

- Fix: Crash on opening map when active track/route files are missing

<!-- --------------------------------------------------------------------------------- --->

## 2022.03.09 Bugfix Release

- Fix: Keep position on switching from Google map to OpenStreetMap map
- Fix: Rare crash in cache list attribute overview
- Fix: Re-enable "Restore a different backup" function
- Change: Remove notifications on loading tracks
- Fix: Rare crash when sorting by difficulty if adventure labs are on the list
- Change: Use different icon for "edit individual route" in route/track quick settings
- Fix: Map for a waypoint now centered correctly with Google Maps
- Fix: Show better file name in c:geo after selecting a GPX track file
- Fix: Remember "show/hide" setting for routes / tracks

<!-- --------------------------------------------------------------------------------- --->

## 2021.02.16 Bugfix release

- Fix: Rare crash on startup of cgeo

<!-- --------------------------------------------------------------------------------- --->

## 2021.02.13 Feature release

### General
- Change: Introducing bottom navigation for direct access to c:geo's most-used screens, replacing the old mainscreen

### Map
- Fix: On loading GPX files containing multiple tracks display them as separate, unconnected tracks
- Change: Automatically enable track display on loading a GPX track file
- New: Allow displaying several tracks at once
- New: D/T symbols for cache icons (optional)
- New: Option to check for missing routing data for current viewport
- New: Theme legend for Elevate, Elements and Freizeitkarte themes
- Fix: Reenable routing with external BRouter app in version 1.6.3
- Fix: Avoid map duplication by map downloader in certain conditions

### Cache list
- New: Option to select next 20 caches
- New: Attributes overview (see Manage Caches => Attributes overview)
- New: Add import from bookmark lists (GC premium only)
- New: Invert sort-order on long click on sort bar
- Change: Also perform automatic sorting by distance for lists containing cache series with more than 50 caches (up to 500)
- Fix: Use a shorter timeout for fast scrolling mechanism for less interference with other layout elements

### Cache details
- New: Pass current cache coordinates to geochecker (if supported by geochecker)
- New: Colored attribute icons (following attribute groups)
- Fix: Problem opening pictures from gallery tab in external apps on some Samsung devices
- Fix: Missing log count (website change)

### Other
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
- Fix: Crash on long-tap on trackable code in trackable details
- Fix: Fieldnotes upload (website change)
- Refactored settings to meet current Android specifications
- Updated MapsWithMe API

<!-- --------------------------------------------------------------------------------- --->

## 2021.12.24 Bugfix release

- Fix: Enable upgrading from OpenAndroMaps v4 to v5
- Fix: Enable checks for map theme updates
- Fix: Skip Mapsforge cache cleanup due to problems under certain conditions

<!-- --------------------------------------------------------------------------------- --->

## 2021.12.13 Bugfix release

- Fix: Mapsforge's cache files: Cleanup and new location (separate folder)
- Fix: Freizeitkarte map files: Use 'latest' folder in downloader as workaround for temporary server errors
- Fix: Avoid null pointer exception in about pages
- Fix: Enable wrapping for stars in cache popup
- Fix: Display error message if a cache could not be found while trying to refresh it
- Fix: Show system default browser in app selection when using 'Open in browser' for a cache
- Fix: Adapt downloader to use new theme page and new v5 maps for OpenAndroMaps
- Fix: On changing a path setting don't ask user for copy or move if old path has no files

<!-- --------------------------------------------------------------------------------- --->

## 2021.11.21 Feature release

### Map
- New: Complete rework of all cache icons
- New: Show icons for last logs in cache popup
- Fix: Show distance circles for user-defined caches (if circles are enabled)
- Fix: Make cache title more visible in cache / waypoint popup
- Fix: Show warning in live map when live mode is disabled
- Fix: Allow HTML in navigation targets' description
- Change: Adapted zoom controls for OSM maps for better consistency across maps
- Change: Moved most individual route-related menu options to a separate quick access button at the bottom for faster access. (Only being displayed when an individual route is loaded / created.)

### Cache lists
- New: Complete rework of all cache icons

### Cache details
- New: Show icons for last logs
- New: Show icons for log entries in log tab
- New: Option to hide visited waypoints on waypoints tab
- Fix: Allow HTML in navigation targets' description
- Fix: Filter short description when already contained in long description
- Fix: Refactored "coordinates input" dialog to allow for better display usage in specific conditions
- New: Added variable / calculator tab (experimental, activate debug mode to see it)

### Other
- Change: Separated bookmark lists and pocket queries (geocaching.com PM only)
- New: Force-delete caches marked as "to-be-deleted" when using maintenance function
- Change: Always use hardware acceleration (removed setting and old phone model check)

<!-- --------------------------------------------------------------------------------- --->

## 2021.10.08 Bugfix release

### Fixes
- Fix: Fix some time zone issues when changing log date
- Fix: Start secondary navigation on long tap of compass rose in popup

<!-- --------------------------------------------------------------------------------- --->

## 2021.09.27 Feature release

### Map
- New: Added Mapy.cz as online map source
- New: Individual route: Allow setting a new start and reversing the route
- New: Multiple offline map downloads can now be triggered at once
- New: Add "Has offline found log" to status filter  & change quick settings mapping

### Cache details
- Fix: Detect if current user is owner for lab adventures
- New: Added some more emoijis for use as individual cache icon
- New: Support adding caches to bookmark lists (PM only)
- New: When tapping on owner name, opening in message center will prefill the text with cache's name (geocaching.com only)

### Other
- New: Allow upload of caches to bookmark list (only available for GC premium members)
- New: Automatically prefill 'search by geocode' if clipboard content can be parsed as valid geocode
- New: Added delay option to update check
- New: Added more emojis to emoji selector
- Fix: Some fixes for UI and for filtering
- Fix: Some fixes for bookmark lists (downloading & current status)
- Change: Hide sensitive data in "view settings"
- Change: Cache prefix for adventure labs is now "AL" instead of "LC" (which is already in use for Extremcaching)
- Change: AL connector returns 100 results at a time (instead of 200)

<!-- --------------------------------------------------------------------------------- --->

## 2021.09.08 Bugfix release

### Fixes
- "Add to watchlist" / "Remove from watchlist" failing (Website change)
- "Add to favorite" / "Remove from favorite" buttons not shown after "found" log
- Date in logbook cut off on larger fonts
- Filtering in live map for more rare cache types returning only few results

<!-- --------------------------------------------------------------------------------- --->

## 2021.08.28 Bugfix release

### Design
- Increase font size for text input fields
- Increase font size for some compass elements
- Use font color with higher contrast in waypoint tab
- Make quick offline log check mark visible again
- Increase font size for coordinate input fields
- Respect system font size settings also on older Android versions (5,6 and 7)

### Cache details
- Fix missing cache title if cache opened via geocode or link (website change)
- Fix missing cache description on some caches

### Other
- Show premium caches again in search results of basic members
- Fix further creation of user defined caches if some user defines caches have been loaded via GPX
- Use more common English abbreviation for traditional cache in cache type filter

<!-- --------------------------------------------------------------------------------- --->

## 2021.08.15 Feature release

### Advanced filtering system (experimental)
- Introducing a new filtering system to c:geo, supporting flexible, combinable and storable filters
- Available in both cache lists and map view
- This is work in progress, stay tuned!
- The old filter systems are still available (for the time being), but will be superseeded by the new system at some point in time.

### Rework of theming
- Change: We've completely reworked the internal technical aspects c:geo theming
- This has been done for several reasons:
  - Main reason is a technical cleanup, to free c:geo from the burden of some outdated and no longer supported components.
  - By this we have the base (and already introduce some) more modern UI components provided by Android.
  - And last but not least we hope to give c:geo a more modern and fresh look and a better usability ;-)
- This will probably have a couple of side-effects, some of them unintended. Please report any errors or glitches either on our [GitHub page](https://www.github.com/cgeo/cgeo/issues) or by contacting support.

### Map
- New: On creating a user-defined cache while displaying a map from a list: Offer user to store new cache in current list (instead of default list for user-defined caches)
- New: Map quick settings: Offer to activate integrated routing engine if no routing is available currently (use "i" icon)
- New: Map quick settings: Separate "own" and "found" filters, additional "has offline log" filter
- Change: Additionally show cache name in poup details

### Cache details
- New: Make use of google translate in-app translation popup
- New: Allow changing the assigned icon in cache details popup via long click (stored caches only)

### Downloader
- Change: Downloads will now completely happen in background, a notification is shown
- Change: Files downloaded successfully will automatically overwrite existing files having the same name
- Change: If a map requires a certain theme which is not installed yet, c:geo will automatically download and install that theme as well
- New: Remove partial files on failed download

### Other
 New: Support day / night mode from system (optional)
- New: Download bookmark lists from geocaching.com - see "Lists / pocket queries" in main menu
- New: Ignore capability for geocaching.su
- New: Support exporting/importig assigned emoji in GPX
- New: Read and write GSAK extension for DNF and visitedDate
- Change: Removed BRouter from wizard's advanced settings page and added activation of integrated routing engine instead
- Change: Removed no longer maintained RMAPS navigation app
- Fix: Enable editing of imported waypoints with empty coordinates

<!-- --------------------------------------------------------------------------------- --->

## 2021.05.20 Feature release

### Geocaching Services
- New: Added connector for Adventure Lab Caches - Shows starting coords of Adventure Labs with basic info on map and searches (PM-only). Use the link on the cache details page to start the Adventure Lab app to play the Adventure.
- [Tap for FAQ, known limitations and open issues for Adventure Lab Caches](https://www.cgeo.org/faq#ALC_PM)

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
- Fix: Failing login to geocaching.com caused by website changes
- Change: "Sort individual route" automatically closes on saving and checks for unsaved changes on using the back arrow
- Fix: A couple of theming issues, esp. aligned theming of Google Maps and settings to "rest of app"
- Fix: Optimize global search: If no trackable with matching tracking code is found, execute online cache name search afterwards
- Fix: Avoid avatar images being displayed too wide and pushing the "Update / remove authorization" functionalty aside
- Fix: Fix conversion error in some distance settings for imperial units
- Fix: Directory selected by user not taken over in wizard on older devices
- Fix: Scan for map themes now run as background task on startup
- Fix: Changing map source via settings being recognized after full restart only
- Fix: Crash in "View settings" under certain conditions
- Fix: Back arrow in map downloader returning to main screen
- Fix: Avoid strange popup messages when attaching image to log
- Fix: Possible crash on map
- Fix: Better support for file / folder selection on Android 5 devices
- New: Debug view for pending downloads
- Change: Removed Pocket Query Creator from useful apps (app no longer maintained and does not work anymore)

<!-- --------------------------------------------------------------------------------- --->

## Older releases

This type of changelog started with release 2021.05.20. For older changelogs see our [release history](https://github.com/cgeo/cgeo/releases)
