# Full changelog
This changelog contains all changes which are not intermediate developing steps. It is more detailed than the changelogs we publish in our releases.

<!-- --------------------------------------------------------------------------------- --->

## Next release (branch `release`)

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
