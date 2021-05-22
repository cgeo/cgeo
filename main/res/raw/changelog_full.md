# Full changelog
This changelog contains all changes which are not intermediate developing steps. It is more detailed than the changelogs we publish in our releases.

<!-- --------------------------------------------------------------------------------- --->

## Next release (branch `release`)

### Map
- Fix: Theming error in downloader (light theme only)
- Fix: Selected track/route file did not get persisted

### Other

<!-- --------------------------------------------------------------------------------- --->

## Current development (branch `master`)

### Advanced filtering system (experimental)
- Introducing a new filtering system to c:geo, supporting flexible, combinable and storable filters
- Available in both cache lists and map view
- This is work in progress, stay tuned!
- The old filter systems are still available (for the time being), but will be superseeded by the new system at some point in time.

### Map
- New: On creating a user-defined cache while displaying a map from a list: Offer user to store new cache in current list (instead of default list for user-defined caches)

### Cache details
- New: Make use of google translate in-app translation popup
- New: Allow changing the assigned icon in cache details popup via long click (stored caches only)

### Downloader
- Change: Files downloaded successfully will automatically overwrite existing files having the same name
- Change: If a map requires a certain theme which is not installed yet, c:geo will automatically download and install that theme as well

### Other
- New: Download bookmark lists from geocaching.com - see "Lists / pocket queries" in main menu
- New: Ignore capability for geocaching.su
- New: Support exporting/importig assigned emoji in GPX
- Change: Removed no longer maintained RMAPS navigation app

<!-- --------------------------------------------------------------------------------- --->

## 2021.05.20

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
