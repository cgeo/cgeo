##
- Fix: `vanity` function failing on long strings
- Fix: Wrong parsing priority in formula backup
- Fix: Compass rose hidden when filterbar is visible (UnifiedMap)

##
- Fix: Log length check counting some characters twice
- Fix: Adapt to hylly website change
- New: Additional theming options for Google Maps
- Fix: Compass rose hidden behind distance views (Google Maps v2)
- New: Enhance logging in case of GC login errors
- Fix: Editing cache logs does not take care of existing favorite points
- Fix: "Save offline" not working after failing to edit a found log
- New: Option to limit search radius for address search
- New: Show notification for missing location permission

##
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

##
- Fix: Trackable links with TB parameter not working
- New: Add hint to disabled keyword search for basic members
- Fix: Trackable logging not working again (website changes)
- Fix: Elevation info is rotating with position marker
- Fix: Username not detected during login when containing certain special characters

##
- Fix: Show/hide waypoints not working correctly if crossing waypoint limits (UnifiedMap)
- Fix: Logging caches or trackables no longer working (website changes)
- Fix: Deleting own logs not working

##
- Fix: Found counter not detected in certain situations due to website changes
- Fix: Crash on opening map with empty track file names
- Fix: Map auto rotation still active after reset using compass rose (UnifiedMap)
- Fix: Missing compass rose in autorotation modes on Google Maps (UnifiedMap)
- Fix: Trackable logs cannot be loaded due to website changes
- Change: Combine elevation + coordinate info in map long-tap menu into single "selected position" + show distance to current position

##
- New: Delete offline logs using context menu
- Fix: Deleting offline log not working under certain condition
- Fix: Filter name lost on filter quickchange
- Change: Sort trackfiles by name
- Change: Save trackable action also for offline logs
- Fix: Map switching to 0,0 coordinates on map type change (UnifiedMap)
- Fix: Waypoint target switching back to cache as target (UnifiedMap)
- Fix: "Storing" a cache without selecting a list
- Fix: Login failure due to website change on geocaching.com
- Change: Show elevation info below position marker (if activated)
- NOTE: There are more issues due to recent website changes on geocaching.com, which have not been fixed yet. We are working on it. See our [status page](https://github.com/cgeo/cgeo/issues/15555) for current progress.
