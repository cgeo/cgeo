# Full changelog
This changelog contains all changes which are not intermediate developing steps. It is sometimes more detailed than the changelogs we publish in our releases.

<!-- --------------------------------------------------------------------------------- --->

## 2025.04.02 Bugfix Release

- Fix: Wherigo links without host name not detected
- Fix: Non-legible error message when posting a log
- Reverted: Local .map file detection (due to unwanted side-effects)

<!-- --------------------------------------------------------------------------------- --->

## 2025.03.06 Bugfix Release

- Fix: Wherigo: Handle zones with invalid/too-close coordinates
- Fix: c:geo tries to import local ".map" files as GPX
- Fix: Edited user notes from popup sometimes get lost
- Fix: Editing log shows "report problem" option
- Fix: Live Filter 'Does not contain' fails to return any caches
- Fix: Crashes on pausing a UnifiedMap online map
- Fix: Outdated app "GPS Bluethooth" in useful apps list
- Fix: Variables and/or values are deleted erroneously when editing personal note
- Fix: Wherigo: Wrong altitude info
- Fix: Wherigo: All button texts are capitalized
- Fix: Crash in settings
- Fix: c:geo not listed in Play Store for GPS-less devices
- Fix: c:geo crashing silently on folder selection in certain configurations
- Change: Make extremcaching.com an offline connector (their service has ceased)
- Fix: Update status info in map on zoom level changes

<!-- --------------------------------------------------------------------------------- --->

## 2025.01.18 Feature Release

New: Integrated Wherigo player (beta) - see menu entry on home screen.<br>
(You may want to [configure a quick launch item](cgeo-setting://quicklaunchitems_sorted) or [customize bottom navigation](cgeo-setting://custombnitem) for easier access, need to enable extended settings first.)

### Map
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

### Cache details
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

### General
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
- Fix: Some sliders in settings having problems with uninitialized values

<!-- --------------------------------------------------------------------------------- --->

## 2024.12.01 Bugfix Release

- Fix: Fix: More time zone issues for events starting around midnight or noon

<!-- --------------------------------------------------------------------------------- --->

## 2024.11.22 Bugfix Release

- Fix: Time zone issues with new event time detection
- Fix: "Manual login" button shown even without "captcha required" login error

<!-- --------------------------------------------------------------------------------- --->

## 2024.11.09 Bugfix Release

- Fix: Some issues with new event time detection
- Fix: Crash when editing variable field
- Fix: Crash on importing GPX file (workaround)

<!-- --------------------------------------------------------------------------------- --->

## 2024.10.27 Bugfix Release

- Fix: Missing event times on cache download (website change)

<!-- --------------------------------------------------------------------------------- --->

## 2024.10.14 Bugfix Release

- Fix: Map constantly reloading caches in offline-only mode (UnifiedMap)
- Fix: Wrong elevation chart for tracks with multiple segments
- Fix: Some crashes in Mapsforge map
- New: Show warning for activated gc connector without credentials

<!-- --------------------------------------------------------------------------------- --->

## 2024.09.18 Bugfix Release

- Fix: Missing parametrization for user-defined tileprovider
- Fix: Edited waypoints or caches do not get updated on map (UnifiedMap)
- Fix: Found cache not marked as found on map (UnifiedMap)
- Fix: Elevation being scaled to kilometers/miles on home screen
- Fix: Attended events being shown as "found"
- Fix: Bumping white space in settings - system

<!-- --------------------------------------------------------------------------------- --->

## 2024.09.09 Bugfix Release

- Fix: "Manual login" cannot be activated
- Fix: "Manual login" shown even without stored credentials
- Fix: Map crash on opening navigation menu
- Fix: Crash on editing personal note without cache
- Fix: Certain spoiler images not detected
- Fix: Current elevation on map shown with fraction

<!-- --------------------------------------------------------------------------------- --->

## 2024.09.01 Feature Release

This is basically the same as release 2024.08.19, but with different foreground service type configuration. This build is available on Play Store again.

The following features are included in addition to release 2024.08.19:

- Fix: Missing "copy to clipboard" option on some devices (see settings => system)
- New: Show warnings when HTTP error 429 occurs (Too many requests) (debug mode only)
- New: Offer manual login on Captcha error

<!-- --------------------------------------------------------------------------------- --->

## 2024.08.19 Feature Release

### Notice

This release is NOT available at Play Store currently.

Reason for this are recent changes in Play Store policies which requires us to declare all service types c:geo offers (e. g.: downloading a map uses a so-called "foreground service"). There is quite a list of conditions which type of foreground service has to be declared for what type of service, but so far we seemingly have not found the right combination for the services c:geo offers. Thus Play Store has rejected c:geo's 2024.08.19 release (although it had accepted the beta versions for it, that used exactly the same declarations). We are working on it.

### Map
- Change: Search for coordinates: Show direction and distance to target and not to current position
- Change: Removed map theme legends
- Fix: Waypoints not filtered on mapping a single cache (UnifiedMap)
- Fix: Refresh map data after opening / closing settings dialog (UnifiedMap)
- Fix: Compass rose hidden when filterbar is visible (UnifiedMap)
- Fix: Multiple navigation selection popups on long-press
- New: "Edit Personal Note" from cache info sheet
- New: Support user-defined tile providers
- New: Toggle display of buildings 2D/3D (UnifiedMap OSM maps)
- New: Cache store/refresh from popup moved into background
- New: Graphical D/T indicator in cache info sheet

### Cache details
- Change: Simplify long-tap action in cache details and trackable details
- Change: Change "edit lists" icon from pencil to list + pencil
- Change: Allow larger integer ranges in formulas (disallow usage of negation)
- Fix: vanity function failing on long strings
- Fix: Wrong parsing priority in formula backup
- Fix: Spoiler images no longer being loaded (website change)
- New: Allow user-stored cache images on creating/editing log
- New: Show images linked in "personal note" in Images tab
- New: Smoother scaling of log images

### General
- Change: Use OSM Nominatum as fallback geocoder, replacing MapQuest geocoder (which is no longer working for us)
- Change: Updated integrated BRouter to v1.7.5
- Change: Updated targetSDK to 34 to comply with upcoming Play Store requirements
- Change: Improve description for "maintenance" function (remove orphaned data)
- Fix: Search results for a location no longer sorted by distance to target location
- Fix: Flickering on cache list refresh
- Fix: Search for geokretys no longer working when using trackingcodes
- New: Switch to set found state of Lab Adventures either manually or automatically
- New: List selection dialog: Auto-group cache lists having a ":" in their name
- New: Read elevation info from track on import
- New: API to Locus now supporting cache size "virtual"
- New: "Corrected coordinates" filter
- New: Added "none"-entry to selection of routing profiles
- New: Allow display of passwords in connector configuration

<!-- --------------------------------------------------------------------------------- --->

## 2024.07.07 Bugfix Release

- Fix: Log length check counting some characters twice
- Fix: Editing cache logs does not take care of existing favorite points
- Fix: Adapt to hylly website change
- Fix: Compass rose hidden behind distance views (Google Maps v2)
- Fix: "Save offline" not working after failing to edit a found log
- New: Enhance logging in case of GC login errors
- New: Additional theming options for Google Maps
- New: Option to limit search radius for address search
- New: Show notification for missing location permission

<!-- --------------------------------------------------------------------------------- --->

## 2024.06.02 Bugfix Release

- Fix: Caches not loading after enabling live map (UnifiedMap)
- Fix: Missing 'use current list' option on creating user-defined cache (UnifiedMap)
- Fix: Compass rose hidden behind distance views (UnifiedMap)
- Fix: Cache details scroll to page header after editing personal note
- New: Show event date to cache selector
- Fix: Login to OC platform not recognized by installation wizard
- Fix: Routing not working by default after fresh installation
- Fix: Info sheet toolbar hidden in landscape mode even on large devices
- Fix: "follow my location" still active after zoom with pan (UnifiedMap)
- Fix: Individual routes exported as track cannot be read by Garmin devices
- Fix: Loading trackables from internal database fails under certain conditions
- Fix: Route to navigation target not recalculated on routing mode change
- Fix: Error while reading available trackable log types

<!-- --------------------------------------------------------------------------------- --->

## 2024.04.25 Bugfix Release

- Fix: Trackable logging not working again (website changes)
- Fix: Username not detected during login when containing certain special characters
- Fix: Elevation info is rotating with position marker
- Fix: Trackable links with TB parameter not working
- New: Add hint to disabled keyword search for basic members

<!-- --------------------------------------------------------------------------------- --->

## 2024.04.18 Bugfix Release

- Fix: Logging caches or trackables no longer working (website changes)
- Fix: Deleting own logs not working
- Fix: Show/hide waypoints not working correctly if crossing waypoint limits (UnifiedMap)
 
<!-- --------------------------------------------------------------------------------- --->

## 2024.04.13 Bugfix Release

- Fix: Found counter not detected in certain situations due to website changes
- Fix: Trackable logs cannot be loaded due to website changes
- Fix: Crash on opening map with empty track file names
- Fix: Map auto rotation still active after reset using compass rose (UnifiedMap)
- Fix: Missing compass rose in autorotation modes on Google Maps (UnifiedMap)
- Change: Combine elevation + coordinate info in map long-tap menu into single "selected position" + show distance to current position

<!-- --------------------------------------------------------------------------------- --->

## 2024.04.05 Bugfix Release

- Fix: Login failure due to website change on geocaching.com
- NOTE: There are more issues due to recent website changes on geocaching.com, which have not been fixed yet. We are working on it. See our [status page](https://github.com/cgeo/cgeo/issues/15555) for current progress.
<br />

- New: Delete offline logs using context menu
- Fix: Deleting offline log not working under certain condition
- Fix: Filter name lost on filter quickchange
- Change: Sort trackfiles by name
- Change: Save trackable action also for offline logs
- Fix: Map switching to 0,0 coordinates on map type change (UnifiedMap)
- Fix: Waypoint target switching back to cache as target (UnifiedMap)
- Fix: "Storing" a cache without selecting a list
- Change: Show elevation info below position marker (if activated)

<!-- --------------------------------------------------------------------------------- --->

## 2024.03.19 Feature Release

### Map
- New: [Unified Map](https://github.com/cgeo/cgeo/wiki/UnifiedMap) (beta), see [Settings => Map Sources => Unified Map](cgeo-setting://featureSwitch_useUnifiedMap)
- New: Highlight existing downloads in download manager
- New: Show cache's found state on waypoint icons
- New: Long-tap option to connect a cache with its waypoints by lines
- Change: Show cache/waypoint details in a non-blocking way
- New: Optionally keep temporary OAM files (map downloader, useful when using POI files with other apps)
- Fix: Hylly theme download error 404
- Fix: Elevation info does not respect "use imperial units" setting

### Cache details
- Change: Make "Toggle talking" an actual toggle
- Change: Increased maximum log length for geocaching.com
- Fix: Cannot upload longer personal notes on opencaching sites
- New: Edit/delete own logs
- New: Waypoint projection with variables
- Change: Restrict image selection to types jpg, png, gif
- New: New formula CHARS (short form: CH) to select multiple single chars
- Fix: Wrong result for TRUNC function with negative numbers
- Fix: Formulas starting with variable similar to a hemisphere marker cannot be calculated
- Fix: Email links in cache listings do not open if custom webview is enabled

### General
- New: Tapping on downloader notification opens "pending downloads" view
- Change: Using wallpaper as background no longer requires READ_EXTERNAL_STORAGE permission
- New: Two column-layout for settings in landscape mode
- Fix: Restore of backups without valid date/time info
- New: Include active trackfiles in backup
- New: Integrated c:geo contacts addon functionality (external addon no longer required)
- Fix: Trackable log type defaults reworked
- Fix: Trackable spotted cache info (website changes)
- Fix: Missing star symbol for changed stored filters
- Fix: Keyword search field displays "GC" after performing a search
- Fix: Internal crash in routing calculation
- Fix: Downloading bookmark lists returns empty list (website change)

<!-- --------------------------------------------------------------------------------- --->

## 2024.01.06 Bugfix Release

- Fix: Trackable logtype default selection
- Fix: Unneded READ_EXTERNAL_STORAGE permission requested on newer Android versions
- Fix: Crash on displaying notification dot
- Fix: Crash on opening map downloader under certain conditions
- Fix: Crash on OSM-based maps under certain conditions

<!-- --------------------------------------------------------------------------------- --->

## 2023.12.21 Bugfix Release

- Fix: Trackables missing in cache inventory (website change)

<!-- --------------------------------------------------------------------------------- --->

## 2023.12.11 Bugfix Release

- Fix: Wrong default when manually logging a trackable
- Fix: coord.info trackable links cannot be opened any more
- Fix: Hint hidden behind compass when using large fonts
- Fix: Cache icon on map not updated after logging from popup
- Fix: Crash on displaying logging options
- Fix: Crash on retrieving message center status

<!-- --------------------------------------------------------------------------------- --->

## 2023.11.13 Bugfix Release

- Change: Removed separate BetterCacher box, tap on BetterCacher info line instead
- Change: Reduce timeout for calls to bettercacher.org
- Fix: Personal note from server contained incorrect chars

<!-- --------------------------------------------------------------------------------- --->

## 2023.11.01 Bugfix Release

- Fix: Issues with distance views
- Fix: Not all launcher icon variants are adaptive
- Fix: Outdated error message if not network connection is available
- Fix: Crashes when creating waypoint markers
- Fix: Timezone issues when logging

<!-- --------------------------------------------------------------------------------- --->

## 2023.10.25 Feature Release

### Map
- Updated Mapsforge and VTM libs to v0.20
- Updated Google Maps renderer
- New: Show cache type icon as marker if custom cache icon has been set (emoji) or big icons are enabled
- New: Show custom cache icon as marker for related waypoints
- New: Allow scaling of cache/waypoint icons (see Settings => Appearance)
- New: Linear labs use numbers as waypoint markers
- New: Waypoints of a cache show cache type icon as marker
- New: Show elevation info for current position on map (see Settings => Map Content & Behavior)
- New: User-defined routing profiles (internal routing only)
- Fix: Proximity notification title too long, actual notification gets cut off on small screens

### Cache details
- New: Edit default value for log image caption prefix
- New: Ensure minimum image size for display
- Change: Redesigned edit options for own log images
- New: Bettercacher integration
- New: Sync visited state on synchronizing waypoints to personal note
- New: Retrieve found date for Opencaching-based caches
- New: Append all waypoints to individual route

### General
- New: Increased target SDK to 33
- Change: Handling of database access to avoid "database not available" crashes
- Change: Only basic settings will be displayed per default. Go to Settings => Extended Settings to enable extended settings
- New: Allow filtering in multi selection lists
- New: Filter to include other named (stored) filters
- Change: Removed AndroidBeam which is no longer supported by Android platform
- Change: Configuration wizard: Integrated "restore" functionality in "configure services" screen and removed advanced configuration screen.
- Fix: Hide outdated connection error message
- Fix: Cannot create shortcuts for c:geo widgets on newer Android systems

<!-- --------------------------------------------------------------------------------- --->

## 2023.09.26 Bugfix Release

- New: Increased target SDK to 33 (due to Play Store requirements)
- New: (Android 13 only) Added "Notifications" permission - you may need to run the configuration wizard from home screen or grant notification permission manually to see download notifications
- Fix: GPX import with multiple tracks in same file
- Fix: Favorite ratio > 100% under certain conditions
- Fix: Navigation line lost on screen rotation with Google Maps
- Fix: Individual route not deleted from Google Maps on deletion
- Fix: Last log text discarded when adding image first on new log
- Fix: "Set as favorite" cannot be unchecked if no favorite point left, logging fails with error

<!-- --------------------------------------------------------------------------------- --->

## 2023.08.24 Bugfix Release

- Fix: Individual route actions not working when using Google Maps
- Fix: Crash on adding to favorites (under certain conditions)

<!-- --------------------------------------------------------------------------------- --->

## 2023.08.16 Feature Release

### Map
- Change: Always show popup menu on cache/waypoint long-tap (configurable)
- New: Add 'Set as target' to map's long-tap popup
- Fix: Partial files remaining on aborted copying of downloaded files
- New: Integrity check for downloaded routing tiles
- New: Support for user-selected audio for proximity notifications
- New: Toggle proximity notifications from map quick settings
- New: Extended GeoJSON support for Google Maps
- New: Display a search center indicator when performing address search => tap on map symbol
- New: OpenTopoMap map provider

### Cache details
- New: 'Save and upload' button when editing personal note
- Change: Replace most update progress dialogs by background actions
- Change: Use only non-empty logs for 'repeat last log'
- New: Display logging errors
- New: Allow selecting, copying etc. of image description (EXIF info)

### General
- New: Add more number emojis (separate category)
- Change: Use different source for preview of pocket queries
- New: Make startscreen selectable
- New: Support target selector for additional navi apps
- New: Display question mark in difficulty/terrain symbol, if both difficulty and terrain are unknown
- New: Option to vote on opencaching (OCPL) caches during logging
- New: Event Date filter for upcoming events
- Change: Make 'relative' the default for date filters
- New: Display info on home screen for unread messages (optional)
- New: Quick launch item for message center
- New: Make last bottom navigation item customizable
- New: Updated integrated BRouter
- New: Add interface to OrganicMaps for navigation
- Fix: HTML-encoded chars in 'trackable last spotted" name
- Change: Removed outdated Twitter support

<!-- --------------------------------------------------------------------------------- --->

## 2023.06.18 Bugfix Release

### General
- Fix: Crash in Google Maps on certain map source changes
- Fix: Store caches - "refresh and keep list assignments" option does not store new caches
- Fix: Some seekbar issues
- Fix: SVG geochecker images not being displayed
- New: Display geocaching.com login error messages on home screen

<!-- --------------------------------------------------------------------------------- --->

## 2023.05.15 Feature Release

### Map
- Change: Use last known map position as fallback (when no GPS available and "follow my location" inactive)
- New: Refresh caches in route
- New: Individual coloring of tracks
- New: Update map list on receiving map file
- Change: Do not install downloaded map/theme automatically
- New: Individual route optimization (experimental)
- New: Support for "Google: Terrain" maps
- New: Setting line-width per track
- New: Hide map's action bar on demand (tap on empty space on map)
- Change: Move quick settings buttons to the left in landscape mode to gain more vertical space
- New: Visualize coords-only-points of individual route
- Fix: Preserve target geocode on mapsource change
- Change: Long tap on cache/waypoint, which is part of individual route, will open context menu
- New: Add online attribute filtering for opencaching services
- Fix: Waypoints of Adventure Labs sometimes not being displayed

### Cache details
- Change: Force redownload of cache when importing via send2cgeo
- Change: Differentiate between size "not chosen" and "other" for geocaching.com caches (might give different filtering for some caches older than 2013)
- Fix: Preserve scroll position when using "render complete description" button

### General
- Fix: Preview button displayed only with filter setting "show all" in PQ list
- New: Add Cruiser as navigation app (requires Cruiser 3.0.9 or newer)
- New: Import geocaching.com bookmark lists from links
- New: Allow opening recently viewed caches as list
- New: Make quicklaunch buttons sortable
- New: Workaround for trackable namespace conflict (works for disabled GeoKrety connector)
- New: Make cache list info items configurable
- New: Clear recently viewed caches
- New: Add 'recently viewed caches' to quicklaunch options
- New: Add last logs smiley row to configurable info items
- New: Added "add to individual route" to cache list menu
- New: Updated integrated BRouter to v1.7.0
- Fix: Alignment of compass status view in landscape mode

<!-- --------------------------------------------------------------------------------- --->

## 2023.04.11 Bugfix Release

### General
- Fix: Error on downloading bookmark lists
- Fix: Error on adding caches to / removing caches from bookmark lists
- Fix: Google Maps: cache/waypoint popup opens when tapping zoom control/compass rose with cache/waypoint beneath it

<!-- --------------------------------------------------------------------------------- --->

## 2023.03.25 Bugfix Release

### General
- Fix: Multiple navigation lines on Google Maps map
- Fix: User-created waypoints not being displayed on OSM map
- Fix: Widen log description column in landscape mode
- Fix: Make image format error message clearer

<!-- --------------------------------------------------------------------------------- --->

## 2023.02.25 Bugfix Release

### General
- Fix: Skipping some user-created waypoints on GPX import
- Fix: List of extended filters not translated when language is set to non-default within c:geo
- Fix: Downloading/updating routing tiles or maps no longer possible on older devices
- Fix: Location name shown in compass instead of coordinates
- Fix: c:geo hangs on adding waypoint when certain formulas have been copied to clipboard
- Fix: Wrong image orientation for own images in portrait mode
- Fix: Listing images having urls without protocol not being handled correctly
- Fix: Crash on drawing position marker
- Fix: Loading GPX tracks/routes may lead to unconnected segments

<!-- --------------------------------------------------------------------------------- --->

## 2023.02.12 Feature Release

### Map
- New: OSM map source osm.paws.cz
- New: Enable reading flopp.net GPX files as tracks
- Fix: Missing routing symbol after 'append to route'
- Fix: Missing route calculation for prepended points
- New: Add support for 'Voluntary MF5' OpenAndroMaps theme
- New: Add support for GeoJSON data

### Cache details
- New: New more sophisticated image gallery
- Fix: Restore position in waypoint list after updating or deleting waypoint
- Fix: Move to bottom when creating new waypoint
- New: Recognize variables entered in waypoint user notes
- New: Display lab adventure button in mystery cache details if link to lab adventure detected
- Fix: Removal of waypoint description not synced for server-side waypoints
- Fix: Waypoint list not updated after scan

### General
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
- Fix: Several crashes

<!-- --------------------------------------------------------------------------------- --->

## 2022.12.21 Bugfix Release

### General
- Fix: New TB set to visit mode automatically 
- Fix: gc.com basic member settings not displayed
- Change: mapy.cz map provider removed (due to change in licensing)
- Fix: Cache search field emptied after selecting search hit
- Fix: Crash for degree formulas with multiple dots

<!-- --------------------------------------------------------------------------------- --->

## 2022.11.25 Bugfix Release

### General
- Fix: Use updated URL for mapquest geocoding
- Fix: Prevent crash under certain conditions when trying to attach photo to log
- Change: Better error message about how to resolve a Captcha requirement on login to geocaching.com

### Caches
- Fix: Prevent spoiler images from being doubled in gallery when refreshing a cache 

### Variable calculator
- New: TRUNC function to truncate decimal values 
- New: Allow usage of square brackets for calculations. For variable ranges please use e.g. \[:1-5\] instead.

### Map
- Fix: Show correct DT marker for D4.0 and D4.5

<!-- --------------------------------------------------------------------------------- --->

## 2022.11.13 Feature Release

### Map
- New: Allow change of track's display name

### Cache details
- Fix: Log image labelled "Image 1" even if only a single image added
- New: GEOCODE log template placeholder
- New: Basic HTML formatting support for definition lists (dl/dt/dd)
- New: Open zoomable image view when tapping on listing images
- Fix: Open links in listings in integrated web view (if enabled)
- Change: Render cache description in background and limit length to 50,000 characters by default
- Change: GCVote service connection disabled due to severe performance problems - You can manually re-enable it using Settings - Services - GCVote
- New: Log caches: Preserve last trackable action per trackable

### General
- API level change (targetSDK 31, compileSDK 31)
- New: View for pending downloads
- New: Append cache name / list name to file name on GPX export
- Change: Removed "Identify as Android browser" setting
- New: Check pending downloads (maps / routing data) on startup
- New: Allow selection of files to download
- New: Status filter for DNF
- New: Display elevation on home screen (if available)
- New: Allow manual input of values in filters using sliders
- New: Enable upload of modified coordinates for caches imported from GPX file, when cache has waypoint of type "ORIGINAL"
- Change: Improve filter status line text
- Change: User a better readable color for archived cache names in titles and remove coloring from cache details page

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
