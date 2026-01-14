##
- Time to update! If you are still using Android 7 or older, this might be the last c:geo update for you! With our next feature release of c:geo we will drop support for Android 5-7 to reduce our maintenance load and to be able to update some external components used by c:geo which we are currently still holding back. We will still be supporting Android 8 up to Android 16 then (and newer versions when they will be published), a span of more than eight years of Android history.
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
























