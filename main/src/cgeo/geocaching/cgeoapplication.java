package cgeo.geocaching;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class cgeoapplication extends Application {

    private cgData storage = null;
    private String action = null;
    private Geopoint lastCoords = null;
    private cgGeo geo = null;
    private boolean geoInUse = false;
    private cgDirection dir = null;
    private boolean dirInUse = false;
    public boolean firstRun = true; // c:geo is just launched
    public boolean showLoginToast = true; //login toast shown just once.
    private boolean databaseCleaned = false; // database was cleaned
    private static cgeoapplication instance = null;

    public cgeoapplication() {
        instance = this;
        storage = new cgData(this);
    }

    public static cgeoapplication getInstance() {
        return instance;
    }

    @Override
    public void onLowMemory() {
        Log.i(Settings.tag, "Cleaning applications cache.");

        storage.removeAllFromCache();
    }

    @Override
    public void onTerminate() {
        Log.d(Settings.tag, "Terminating c:geo...");

        cleanGeo();
        cleanDir();

        if (storage != null) {
            storage.clean();
            storage.closeDb();
            storage = null;
            storage = new cgData(this);
        }

        super.onTerminate();
    }

    public String backupDatabase() {
        return storage.backupDatabase();
    }

    public static File isRestoreFile() {
        return cgData.isRestoreFile();
    }

    /**
     * restore the database in a new thread, showing a progress window
     *
     * @param fromActivity
     *            calling activity
     */
    public void restoreDatabase(final Activity fromActivity) {
        final Resources res = this.getResources();
        final ProgressDialog dialog = ProgressDialog.show(fromActivity, res.getString(R.string.init_backup_restore), res.getString(R.string.init_restore_running), true, false);
        final AtomicBoolean atomic = new AtomicBoolean(false);
        Thread restoreThread = new Thread() {
            final Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    dialog.dismiss();
                    boolean restored = atomic.get();
                    String message = restored ? res.getString(R.string.init_restore_success) : res.getString(R.string.init_restore_failed);
                    ActivityMixin.helpDialog(fromActivity, res.getString(R.string.init_backup_restore), message);
                }
            };

            @Override
            public void run() {
                atomic.set(storage.restoreDatabase());
                handler.sendMessage(handler.obtainMessage());
            }
        };
        restoreThread.start();
    }

    public void cleanGeo() {
        if (geo != null) {
            geo.closeGeo();
            geo = null;
        }
    }

    public void cleanDir() {
        if (dir != null) {
            dir.closeDir();
            dir = null;
        }
    }

    public boolean storageStatus() {
        return storage.status();
    }

    public cgGeo startGeo(UpdateLocationCallback geoUpdate) {
        if (geo == null) {
            geo = new cgGeo();
            Log.i(Settings.tag, "Location service started");
        }

        geo.replaceUpdate(geoUpdate);
        geoInUse = true;

        return geo;
    }

    public cgGeo removeGeo() {
        if (geo != null) {
            geo.replaceUpdate(null);
        }
        geoInUse = false;

        (new removeGeoThread()).start();

        return null;
    }

    private class removeGeoThread extends Thread {

        @Override
        public void run() {
            try {
                sleep(2500);
            } catch (Exception e) {
                // nothing
            }

            if (!geoInUse && geo != null) {
                cleanGeo();
                Log.i(Settings.tag, "Location service stopped");
            }
        }
    }

    public cgDirection startDir(Context context, UpdateDirectionCallback dirUpdate) {
        if (dir == null) {
            dir = new cgDirection(context, dirUpdate);

            Log.i(Settings.tag, "Direction service started");
        }

        dir.replaceUpdate(dirUpdate);
        dirInUse = true;

        return dir;
    }

    public cgDirection removeDir() {
        if (dir != null) {
            dir.replaceUpdate(null);
        }
        dirInUse = false;

        (new removeDirThread()).start();

        return null;
    }

    private class removeDirThread extends Thread {

        @Override
        public void run() {
            try {
                sleep(2500);
            } catch (Exception e) {
                // nothing
            }

            if (!dirInUse && dir != null) {
                cleanDir();
                Log.i(Settings.tag, "Direction service stopped");
            }
        }
    }

    public void cleanDatabase(boolean more) {
        if (databaseCleaned) {
            return;
        }

        storage.clean(more);
        databaseCleaned = true;
    }

    public boolean isThere(String geocode, String guid, boolean detailed, boolean checkTime) {
        return storage.isThere(geocode, guid, detailed, checkTime);
    }

    public boolean isOffline(String geocode, String guid) {
        return storage.isOffline(geocode, guid);
    }

    public String getGeocode(String guid) {
        return storage.getGeocodeForGuid(guid);
    }

    public String getCacheid(String geocode) {
        return storage.getCacheidForGeocode(geocode);
    }

    public boolean hasUnsavedCaches(final SearchResult search) {
        if (search == null) {
            return false;
        }

        for (final String geocode : search.getGeocodes()) {
            if (!isOffline(geocode, null)) {
                return true;
            }
        }
        return false;
    }

    public cgTrackable getTrackableByGeocode(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        cgTrackable trackable = null;
        trackable = storage.loadTrackable(geocode);

        return trackable;
    }

    public String[] geocodesInCache() {
        return storage.allDetailedThere();
    }

    public List<Number> getBounds(String geocode) {
        if (geocode == null) {
            return null;
        }

        Set<String> geocodeList = new HashSet<String>();
        geocodeList.add(geocode);

        return getBounds(geocodeList);
    }

    public List<Number> getBounds(final SearchResult search) {
        if (search == null) {
            return null;
        }

        return getBounds(search.getGeocodes());
    }

    public List<Number> getBounds(final Set<String> geocodes) {
        return storage.getBounds(geocodes);
    }

    /** Retrieve all stored caches for a specified list */
    public SearchResult getBatchOfStoredCaches(final boolean detailedOnly, final Geopoint coords, final CacheType cacheType, final int listId) {
        final Set<String> geocodes = storage.loadBatchOfStoredGeocodes(detailedOnly, coords, cacheType, listId);
        final SearchResult search = new SearchResult(geocodes);
        search.totalCnt = getAllStoredCachesCount(true, cacheType, listId);
        return search;
    }

    public List<cgDestination> getHistoryOfSearchedLocations() {
        return storage.loadHistoryOfSearchedLocations();
    }

    public SearchResult getHistoryOfCaches(final boolean detailedOnly, final CacheType cacheType) {
        final Set<String> geocodes = storage.loadBatchOfHistoricGeocodes(detailedOnly, cacheType);
        final SearchResult search = new SearchResult(geocodes);

        search.totalCnt = getAllHistoricCachesCount();
        return search;
    }

    /** Retrieve all stored caches from DB */
    public SearchResult getCachedInViewport(final Long centerLat, final Long centerLon, final Long spanLat, final Long spanLon, final CacheType cacheType) {
        final Set<String> geocodes = storage.loadCachedInViewport(centerLat, centerLon, spanLat, spanLon, cacheType);
        return new SearchResult(geocodes);
    }

    /** Retrieve stored caches from DB with listId >= 1 */
    public SearchResult getStoredInViewport(final Long centerLat, final Long centerLon, final Long spanLat, final Long spanLon, final CacheType cacheType) {
        final Set<String> geocodes = storage.loadStoredInViewport(centerLat, centerLon, spanLat, spanLon, cacheType);
        return new SearchResult(geocodes);
    }

    public int getAllStoredCachesCount(final boolean detailedOnly, final CacheType cacheType, final Integer list) {
        return storage.getAllStoredCachesCount(detailedOnly, cacheType, list);
    }

    public int getAllHistoricCachesCount() {
        return storage.getAllHistoricCachesCount();
    }

    public void markStored(String geocode, int listId) {
        storage.moveToList(geocode, listId);
    }

    public void markDropped(String geocode) {
        storage.moveToList(geocode, StoredList.TEMPORARY_LIST_ID);
    }

    public boolean markFound(String geocode) {
        return storage.markFound(geocode);
    }

    public boolean clearSearchedDestinations() {
        return storage.clearSearchedDestinations();
    }

    public boolean saveSearchedDestination(cgDestination destination) {
        return storage.saveSearchedDestination(destination);
    }

    public boolean saveWaypoints(String geocode, List<cgWaypoint> waypoints, boolean drop) {
        return storage.saveWaypoints(geocode, waypoints, drop);
    }

    public boolean saveOwnWaypoint(int id, String geocode, cgWaypoint waypoint) {
        if (storage.saveOwnWaypoint(id, geocode, waypoint)) {
            this.removeCache(geocode, RemoveFlag.REMOVECACHEONLY);
            return true;
        }
        return false;
    }

    public boolean deleteWaypoint(int id) {
        return storage.deleteWaypoint(id);
    }

    public boolean saveTrackable(cgTrackable trackable) {
        final List<cgTrackable> list = new ArrayList<cgTrackable>();
        list.add(trackable);

        return storage.saveInventory("---", list);
    }

    /** @see cgData.dropList **/
    public void dropList(int listId) {
        storage.dropList(listId);
    }

    public List<cgTrackable> loadInventory(String geocode) {
        return storage.loadInventory(geocode);
    }

    public Map<LogType, Integer> loadLogCounts(String geocode) {
        return storage.loadLogCounts(geocode);
    }

    public List<cgImage> loadSpoilers(String geocode) {
        return storage.loadSpoilers(geocode);
    }

    public cgWaypoint loadWaypoint(int id) {
        return storage.loadWaypoint(id);
    }

    /**
     * set the current action to be reported to Go4Cache (if enabled in settings)<br>
     * this might be either
     * <ul>
     * <li>geocode</li>
     * <li>name of a cache</li>
     * <li>action like twittering</li>
     * </ul>
     *
     * @param action
     */
    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return StringUtils.defaultString(action);
    }

    public boolean addLog(String geocode, cgLog log) {
        if (StringUtils.isBlank(geocode)) {
            return false;
        }
        if (log == null) {
            return false;
        }

        List<cgLog> list = new ArrayList<cgLog>();
        list.add(log);

        return storage.saveLogs(geocode, list, false);
    }

    public void setLastCoords(final Geopoint coords) {
        lastCoords = coords;
    }

    public Geopoint getLastCoords() {
        return lastCoords;
    }

    public boolean saveLogOffline(String geocode, Date date, LogType logtype, String log) {
        return storage.saveLogOffline(geocode, date, logtype, log);
    }

    public cgLog loadLogOffline(String geocode) {
        return storage.loadLogOffline(geocode);
    }

    public void clearLogOffline(String geocode) {
        storage.clearLogOffline(geocode);
    }

    public void saveVisitDate(String geocode) {
        storage.setVisitDate(geocode, System.currentTimeMillis());
    }

    public void clearVisitDate(String geocode) {
        storage.setVisitDate(geocode, 0);
    }

    public List<StoredList> getLists() {
        return storage.getLists(getResources());
    }

    public StoredList getList(int id) {
        return storage.getList(id, getResources());
    }

    public int createList(String title) {
        return storage.createList(title);
    }

    public int renameList(final int listId, final String title) {
        return storage.renameList(listId, title);
    }

    public boolean removeList(int id) {
        return storage.removeList(id);
    }

    public boolean removeSearchedDestinations(cgDestination destination) {
        return storage.removeSearchedDestination(destination);
    }

    public void moveToList(String geocode, int listId) {
        storage.moveToList(geocode, listId);
    }

    public String getCacheDescription(String geocode) {
        return storage.getCacheDescription(geocode);
    }


    public cgCache loadCache(final String geocode, final EnumSet<LoadFlag> loadFlags) {
        return storage.loadCache(geocode, loadFlags);
    }

    public Set<cgCache> loadCaches(final Set<String> geocodes, final EnumSet<LoadFlag> loadFlags) {
        return storage.loadCaches(geocodes, loadFlags);
    }

    public Set<cgCache> loadCaches(Long centerLat, Long centerLon, Long spanLat, Long spanLon, final EnumSet<LoadFlag> loadFlags) {
        return storage.loadCaches(null, centerLat, centerLon, spanLat, spanLon, loadFlags);
    }

    public boolean saveCache(cgCache cache, LoadFlags.SaveFlag saveFlag) {
        return storage.saveCache(cache, saveFlag);
    }

    public void removeCache(String geocode, LoadFlags.RemoveFlag removeFlag) {
        storage.removeCache(geocode, removeFlag);
    }

    public void removeCaches(final Set<String> geocodes, LoadFlags.RemoveFlag removeFlag) {
        storage.removeCaches(geocodes, removeFlag);
    }

}
