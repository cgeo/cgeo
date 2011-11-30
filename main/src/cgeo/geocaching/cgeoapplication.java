package cgeo.geocaching;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.collections.CollectionUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class cgeoapplication extends Application {

    private cgData storage = null;
    private String action = null;
    private Geopoint lastCoords = null;
    private cgGeo geo = null;
    private boolean geoInUse = false;
    private cgDirection dir = null;
    private boolean dirInUse = false;
    final private Map<String, cgCache> cachesCache = new HashMap<String, cgCache>(); // caching caches into memory
    public boolean firstRun = true; // c:geo is just launched
    public boolean showLoginToast = true; //login toast shown just once.
    public boolean warnedLanguage = false; // user was warned about different language settings on geocaching.com
    private boolean databaseCleaned = false; // database was cleaned
    private static cgeoapplication instance;

    public cgeoapplication() {
        setInstance(this);
        getStorage();
    }

    private static void setInstance(final cgeoapplication app) {
        instance = app;
    }

    public static cgeoapplication getInstance() {
        return instance;
    }

    @Override
    public void onLowMemory() {
        Log.i(Settings.tag, "Cleaning applications cache.");

        cachesCache.clear();
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

        getStorage().clean(more);
        databaseCleaned = true;
    }

    public boolean isThere(String geocode, String guid, boolean detailed, boolean checkTime) {
        return getStorage().isThere(geocode, guid, detailed, checkTime);
    }

    public boolean isOffline(String geocode, String guid) {
        return getStorage().isOffline(geocode, guid);
    }

    public String getGeocode(String guid) {
        return getStorage().getGeocodeForGuid(guid);
    }

    public String getCacheid(String geocode) {
        return getStorage().getCacheidForGeocode(geocode);
    }

    public static StatusCode getError(final cgSearch search) {
        if (search == null) {
            return null;
        }

        return search.error;
    }

    public static boolean setError(final cgSearch search, final StatusCode error) {
        if (search == null) {
            return false;
        }

        search.error = error;

        return true;
    }

    public static String getUrl(final cgSearch search) {
        if (search == null) {
            return null;
        }

        return search.url;
    }

    public static boolean setUrl(final cgSearch search, String url) {
        if (search == null) {
            return false;
        }

        search.url = url;

        return true;
    }

    public static String[] getViewstates(final cgSearch search) {
        if (search == null) {
            return null;
        }

        return search.viewstates;
    }

    public static boolean setViewstates(final cgSearch search, String[] viewstates) {
        if (cgBase.isEmpty(viewstates) || search == null) {
            return false;
        }

        search.viewstates = viewstates;

        return true;
    }

    public static Integer getTotal(final cgSearch search) {
        if (search == null) {
            return null;
        }

        return search.totalCnt;
    }

    public static Integer getCount(final cgSearch search) {
        if (search == null) {
            return 0;
        }

        return search.getCount();
    }

    public boolean hasUnsavedCaches(final cgSearch search) {
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

    public cgCache getCacheByGeocode(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        return getCacheByGeocode(geocode, cgCache.LOADWAYPOINTS);
    }

    public cgCache getCacheByGeocode(final String geocode, final int loadFlags) {
        if (cachesCache.containsKey(geocode)) {
            return cachesCache.get(geocode);
        }

        final cgCache cache = getStorage().loadCache(geocode, loadFlags);

        if (cache != null && cache.getDetailed() && loadFlags == cgCache.LOADALL) {
            putCacheInCache(cache);
        }

        return cache;
    }

    public cgTrackable getTrackableByGeocode(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        cgTrackable trackable = null;
        trackable = storage.loadTrackable(geocode);

        return trackable;
    }

    public void removeCacheFromCache(String geocode) {
        if (geocode != null && cachesCache.containsKey(geocode)) {
            cachesCache.remove(geocode);
        }
    }

    public void putCacheInCache(cgCache cache) {
        if (cache == null || cache.getGeocode() == null) {
            return;
        }

        if (cachesCache.containsKey(cache.getGeocode())) {
            cachesCache.remove(cache.getGeocode());
        }

        cachesCache.put(cache.getGeocode(), cache);
    }

    public String[] geocodesInCache() {
        return getStorage().allDetailedThere();
    }

    public cgWaypoint getWaypointById(Integer id) {
        if (id == null || id == 0) {
            return null;
        }

        return getStorage().loadWaypoint(id);
    }

    public List<Object> getBounds(String geocode) {
        if (geocode == null) {
            return null;
        }

        List<String> geocodeList = new ArrayList<String>();
        geocodeList.add(geocode);

        return getBounds(geocodeList);
    }

    public List<Object> getBounds(final cgSearch search) {
        if (search == null) {
            return null;
        }

        if (storage == null) {
            storage = new cgData(this);
        }

        final List<String> geocodeList = search.getGeocodes();

        return getBounds(geocodeList);
    }

    public List<Object> getBounds(final List<String> geocodes) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return null;
        }

        return getStorage().getBounds(geocodes.toArray());
    }

    public cgCache getCache(final cgSearch search) {
        if (search == null) {
            return null;
        }

        final List<String> geocodeList = search.getGeocodes();

        return getCacheByGeocode(geocodeList.get(0), cgCache.LOADALL);
    }

    /**
     * @param search
     * @param loadWaypoints
     *            only load waypoints for map usage. All other callers should set this to <code>false</code>
     * @return
     */
    public List<cgCache> getCaches(final cgSearch search, final boolean loadWaypoints) {
        return getCaches(search, null, null, null, null, (loadWaypoints ? cgCache.LOADWAYPOINTS : 0) | cgCache.LOADOFFLINELOG);
    }

    public List<cgCache> getCaches(final cgSearch search, Long centerLat, Long centerLon, Long spanLat, Long spanLon) {
        return getCaches(search, centerLat, centerLon, spanLat, spanLon, cgCache.LOADWAYPOINTS | cgCache.LOADOFFLINELOG);
    }

    public List<cgCache> getCaches(final cgSearch search, final Long centerLat, final Long centerLon, final Long spanLat, final Long spanLon, final int loadFlags) {
        if (search == null) {
            List<cgCache> cachesOut = new ArrayList<cgCache>();

            final List<cgCache> cachesPre = storage.loadCaches(null, centerLat, centerLon, spanLat, spanLon, loadFlags);
            if (cachesPre != null) {
                cachesOut.addAll(cachesPre);
            }

            return cachesOut;
        }

        List<cgCache> cachesOut = new ArrayList<cgCache>();

        final List<String> geocodeList = search.getGeocodes();

        // The list of geocodes is sufficient. more parameters generate an overly complex select.
        final List<cgCache> cachesPre = getStorage().loadCaches(geocodeList.toArray(), null, null, null, null, loadFlags);
        if (cachesPre != null) {
            cachesOut.addAll(cachesPre);
        }

        return cachesOut;
    }

    public cgSearch getBatchOfStoredCaches(final boolean detailedOnly, final Geopoint coords, final CacheType cacheType, final int list) {
        final List<String> geocodes = getStorage().loadBatchOfStoredGeocodes(detailedOnly, coords, cacheType, list);
        return new cgSearch(geocodes);
    }

    public List<cgDestination> getHistoryOfSearchedLocations() {
        return getStorage().loadHistoryOfSearchedLocations();
    }

    public cgSearch getHistoryOfCaches(final boolean detailedOnly, final CacheType cacheType) {
        final List<String> geocodes = getStorage().loadBatchOfHistoricGeocodes(detailedOnly, cacheType);
        return new cgSearch(geocodes);
    }

    public cgSearch getCachedInViewport(final Long centerLat, final Long centerLon, final Long spanLat, final Long spanLon, final CacheType cacheType) {
        final List<String> geocodes = getStorage().getCachedInViewport(centerLat, centerLon, spanLat, spanLon, cacheType);
        return new cgSearch(geocodes);
    }

    public cgSearch getStoredInViewport(final Long centerLat, final Long centerLon, final Long spanLat, final Long spanLon, final CacheType cacheType) {
        final List<String> geocodes = getStorage().getStoredInViewport(centerLat, centerLon, spanLat, spanLon, cacheType);
        return new cgSearch(geocodes);
    }

    public int getAllStoredCachesCount(final boolean detailedOnly, final CacheType cacheType, final Integer list) {
        return getStorage().getAllStoredCachesCount(detailedOnly, cacheType, list);
    }

    private cgData getStorage() {
        if (storage == null) {
            storage = new cgData(this);
        }
        return storage;
    }

    public int getAllHistoricCachesCount() {
        return getStorage().getAllHistoricCachesCount();
    }

    public void markStored(String geocode, int listId) {
        getStorage().markStored(geocode, listId);
    }

    public boolean markDropped(String geocode) {
        return getStorage().markDropped(geocode);
    }

    public boolean markFound(String geocode) {
        return getStorage().markFound(geocode);
    }

    public boolean clearSearchedDestinations() {
        return getStorage().clearSearchedDestinations();
    }

    public boolean saveSearchedDestination(cgDestination destination) {
        return getStorage().saveSearchedDestination(destination);
    }

    public boolean saveWaypoints(String geocode, List<cgWaypoint> waypoints, boolean drop) {
        return getStorage().saveWaypoints(geocode, waypoints, drop);
    }

    public boolean saveOwnWaypoint(int id, String geocode, cgWaypoint waypoint) {
        return getStorage().saveOwnWaypoint(id, geocode, waypoint);
    }

    public boolean deleteWaypoint(int id) {
        return getStorage().deleteWaypoint(id);
    }

    public boolean saveTrackable(cgTrackable trackable) {
        final List<cgTrackable> list = new ArrayList<cgTrackable>();
        list.add(trackable);

        return getStorage().saveInventory("---", list);
    }

    public static void addGeocode(final cgSearch search, final String geocode) {
        if (search == null || StringUtils.isBlank(geocode)) {
            return;
        }

        search.addGeocode(geocode);
    }

    public void addSearch(final List<cgCache> cacheList, final int reason) {
        if (CollectionUtils.isEmpty(cacheList)) {
            return;
        }

        if (storage == null) {
            storage = new cgData(this);
        }

        for (final cgCache cache : cacheList) {
            cache.setReason(reason);
            storeWithMerge(cache, false);
        }
    }

    public boolean addCacheToSearch(cgSearch search, cgCache cache) {
        if (search == null || cache == null) {
            return false;
        }

        final boolean status = storeWithMerge(cache, cache.getReason() >= 1);

        if (status) {
            search.addGeocode(cache.getGeocode());
        }

        return status;
    }

    /**
     * Checks if Cache is already in Database and if so does a merge.
     *
     * @param cache
     *            the cache to be saved
     * @param override
     *            override the check and persist the new state.
     * @return true if the cache has been saved correctly
     */

    private boolean storeWithMerge(final cgCache cache, final boolean override) {
        if (!override) {
            final cgCache oldCache = storage.loadCache(cache.getGeocode(), cgCache.LOADALL);
            cache.gatherMissingFrom(oldCache);
        }
        return storage.saveCache(cache);
    }

    public void dropStored(int listId) {
        getStorage().dropStored(listId);
    }

    public List<cgTrackable> loadInventory(String geocode) {
        return storage.loadInventory(geocode);
    }

    public Map<Integer, Integer> loadLogCounts(String geocode) {
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

    public boolean saveLogOffline(String geocode, Date date, int logtype, String log) {
        return storage.saveLogOffline(geocode, date, logtype, log);
    }

    public cgLog loadLogOffline(String geocode) {
        return storage.loadLogOffline(geocode);
    }

    public void clearLogOffline(String geocode) {
        storage.clearLogOffline(geocode);
    }

    public void saveVisitDate(String geocode) {
        storage.saveVisitDate(geocode);
    }

    public void clearVisitDate(String geocode) {
        storage.clearVisitDate(geocode);
    }

    public List<cgList> getLists() {
        return storage.getLists(getResources());
    }

    public cgList getList(int id) {
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
}
