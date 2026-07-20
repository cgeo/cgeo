##
Time to update! If you are still using Android 7 or older, this might be the last c:geo update for you! With our next feature release of c:geo we will drop support for Android 5-7 to reduce our maintenance load and to be able to update some external components used by c:geo which we are currently still holding back. We will still be supporting Android 8 up to Android 16 then (and newer versions when they will be published), a span of more than eight years of Android history.

- Fix: Cache/waypoint popup opening delayed on some devices
- Fix: Edit cache description does not support copy & paste
- Fix: Some crashes and "app not responding"
- Fix: Deleting of trackable log fails (website change)

##
- Fix: Deleting of log images broken (website change)
- Change: Unify track and individual route loading buttons
- Fix: Cache attributes not detected correctly under certain conditions
- Fix: Logging caches (website change)
- Fix: Logging trackables (website change)

##
- Fix: Pocket query import broken (website change)

##
- Fix: Crash when accessing routes
- Fix: Crash on waypoint page
- Change: Search for "own caches" starts with fresh filters
- Fix: Unsaved lab adventure stages losing "visited" info on refresh
- Fix: Recurring prompt for tile source updates
- Fix: Random location on mapping a list (Google Maps)

##
- Fix: Crash in cache infosheet
- Fix: Wherigo cartridges cannot be downloaded anymore (website change)

##
 - Change: Wherigo files cannot be downloaded currently, display mitigation instructions
 - Fix: Log delete reason does not enforce lengh limit
 - New: Extended logging for crashes in download manager
 - Fix: Waypoint infosheet can become too long, buttons unreachable
 - Fix: Some location info gets truncated
 - Fix: Internal routing no longer working, only straight line shown
 - Fix: Some folder creation issues

Note: If you are using internal routing, you will need to execute the following step once after installing this release: Go to c:geo home screen, open "Manage offline data" - "Update routing data", and let c:geo install the updated files. (Reason: BRouter routing data file structure has changed and all routing data files must comply to the same version.)

##
- Fix: Parsing cache location string fails for certain website languages
- Fix: Opening trackable from watchlist fails
- Fix: Keyboard may be blocking list selection
- Fix: User-defined tileprovider not supporting additional URL parameters
- Fix: Inventory / Trackables of a cache not loaded anymore
- Change: Updated internal user-agent to address some download issues
- Fix: Viewing trackable details removes it from cache inventory

##
- Oprava: Dialógové okno na stiahnutie offline prekladu sa zobrazovalo v inštaláciách bez podpory offline prekladu
- Oprava: Zmena formátu súradníc v informačnom hárku kešky/bodu trasy
- Fix: Log date cut off in list of logs (depending on date format and font size)
- Fix: Event times not detected in certain conditions
- Fix: Link in listing not clickable under certain conditions
- Fix: Logging actions for trackables get mixed up sometimes

##
- Change: Maximum number of GC trackables visiting per cache log reduced to 100 (as per request from geocaching.com to reduce their server load caused by extreme trackable lovers)
- Fix: Some possible security exceptions when user has not granted certain rights (eg.: notifications)
- Fix: Cache circles incomplete on low zoom levels (VTM only)
- Fix: Crash on reloading waypoints in certain load conditions
- Fix: Event date filter not working under certain conditions
- Fix: Max log line limit not working reliably in "unlimited" setting
- Fix: Crash on opening map under certain conditions
- Fix: No map shown if wherigo has no visible zones
- Fix: Crash on cache details' image tab under certain conditions
- Fix: Map searches with invalid coordinates
- Fix: Some translations do not respect c:geo-internal language setting

##
- Change: UnifiedMap set as default map for anyone (as part of our roadmap to UnifiedMap) You can switch back in "settings" - "map sources" for the time being. Removal of legacy maps is planned for spring 2026 in our regular releases.
- Fix: Favorite checkbox gets reset on reentering offline log screen
- Fix: Geofence radius input box shows decimal number
- Fix: Syncing of personal notes not working
- Change: New icon for GPX track/route import in map track/route quick settings

##
- Fix: Negative values in elevation chart not scaled
- Fix: Coordinates near 0 broken in GPX exports
- Fix: Some crashes
- Try to fix: ANR on startup
- Try to fix: Missing geocache data on live map

##
- Fix: Crash in keyword search
- Fix: Crash in map
- Fix: Hint text no longer selectable
- Fix: Several Wherigo issues

##
- Fix: Encrypting/decrypting a hint needs an extra tap initially
- Fix: Wherigo crash on reading old saved games
- Fix: Logging from within c:geo not remembered sometimes
- Fix: Missing live data update for found & archived caches
- Fix: Waypoints in offline map are not shown sometimes

##
- Fix: Unencrypted cache hints (website change)
- Fix: Lab Adventures not loading in app (website change, you will need to update stored lab adventures to be able to call them from c:geo again)
- Fix: UnifiedMap VTM: Toggling 3D buildings doesn't work for combined maps
- Fix: Offline translation: Listing language sometimes detected as --

##
- Fix: Crash in translation module
- Fix: Login detection fails (website change)
- Fix: Crash on retrieving Wherigo cartridge
- Fix: "Load more" does not respect offline filters

##
- Fix: Trackable inventory not loaded while logging a cache

##
- Fix: Migration of user-defined caches during c:geo startup fails => removed it for the time being
- Fix: Finished Wherigo tasks not marked as finished or failed































