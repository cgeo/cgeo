package cgeo.geocaching;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;
import cgeo.geocaching.enumerations.LogType;
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
import java.util.EnumSet;
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

        CacheCache.getInstance().removeAll();
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

    public cgCache getCacheByGeocode(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        return getCacheByGeocode(geocode, LoadFlags.LOADALL);
    }

    public cgCache getCacheByGeocode(final String geocode, final EnumSet<LoadFlag> loadFlags) {
        cgCache cache = CacheCache.getInstance().getCacheFromCache(geocode);
        if (cache != null) {
            return cache;
        }

        cache = storage.loadCache(geocode, loadFlags);

        if (cache != null && cache.isDetailed() && loadFlags == LoadFlags.LOADALL) {
            // "Store" cache for the next access in the cache
            CacheCache.getInstance().putCacheInCache(cache);
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

    @SuppressWarnings("static-method")
    public void removeCacheFromCache(final String geocode) {
        CacheCache.getInstance().removeCacheFromCache(geocode);
    }

    @SuppressWarnings("static-method")
    public void putCacheInCache(final cgCache cache) {
        CacheCache.getInstance().putCacheInCache(cache);
    }

    public String[] geocodesInCache() {
        return storage.allDetailedThere();
    }

    public List<Number> getBounds(String geocode) {
        if (geocode == null) {
            return null;
        }

        List<String> geocodeList = new ArrayList<String>();
        geocodeList.add(geocode);

        return getBounds(geocodeList);
    }

    public List<Number> getBounds(final SearchResult search) {
        if (search == null) {
            return null;
        }

        return getBounds(search.getGeocodes());
    }

    public List<Number> getBounds(final List<String> geocodes) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return null;
        }

        return storage.getBounds(geocodes.toArray());
    }

    public cgCache getCache(final SearchResult search) {
        if (search == null) {
            return null;
        }

        final List<String> geocodeList = search.getGeocodes();

        return getCacheByGeocode(geocodeList.get(0), LoadFlags.LOADALL);
    }

    /**
     * @param search
     * @param loadWaypoints
     *            only load waypoints for map usage. All other callers should set this to <code>false</code>
     * @return
     */
    public List<cgCache> getCaches(final SearchResult search, final boolean loadWaypoints) {
        return getCaches(search, null, null, null, null, loadWaypoints ? EnumSet.of(LoadFlag.LOADWAYPOINTS, LoadFlag.LOADOFFLINELOG) : EnumSet.of(LoadFlag.LOADOFFLINELOG));
    }

    public List<cgCache> getCaches(final SearchResult search, Long centerLat, Long centerLon, Long spanLat, Long spanLon) {
        return getCaches(search, centerLat, centerLon, spanLat, spanLon, EnumSet.of(LoadFlag.LOADWAYPOINTS, LoadFlag.LOADOFFLINELOG));
    }

    public List<cgCache> getCaches(final SearchResult search, final Long centerLat, final Long centerLon, final Long spanLat, final Long spanLon, final EnumSet<LoadFlag> loadFlags) {
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
        final List<cgCache> cachesPre = storage.loadCaches(geocodeList.toArray(), null, null, null, null, loadFlags);
        if (cachesPre != null) {
            cachesOut.addAll(cachesPre);
        }

        return cachesOut;
    }

    public SearchResult getBatchOfStoredCaches(final boolean detailedOnly, final Geopoint coords, final CacheType cacheType, final int list) {
        final List<String> geocodes = storage.loadBatchOfStoredGeocodes(detailedOnly, coords, cacheType, list);
        return new SearchResult(geocodes);
    }

    public List<cgDestination> getHistoryOfSearchedLocations() {
        return storage.loadHistoryOfSearchedLocations();
    }

    public SearchResult getHistoryOfCaches(final boolean detailedOnly, final CacheType cacheType) {
        final List<String> geocodes = storage.loadBatchOfHistoricGeocodes(detailedOnly, cacheType);
        return new SearchResult(geocodes);
    }

    public SearchResult getCachedInViewport(final Long centerLat, final Long centerLon, final Long spanLat, final Long spanLon, final CacheType cacheType) {
        final List<String> geocodes = storage.getCachedInViewport(centerLat, centerLon, spanLat, spanLon, cacheType);
        return new SearchResult(geocodes);
    }

    public SearchResult getStoredInViewport(final Long centerLat, final Long centerLon, final Long spanLat, final Long spanLon, final CacheType cacheType) {
        final List<String> geocodes = storage.getStoredInViewport(centerLat, centerLon, spanLat, spanLon, cacheType);
        return new SearchResult(geocodes);
    }

    public int getAllStoredCachesCount(final boolean detailedOnly, final CacheType cacheType, final Integer list) {
        return storage.getAllStoredCachesCount(detailedOnly, cacheType, list);
    }

    public int getAllHistoricCachesCount() {
        return storage.getAllHistoricCachesCount();
    }

    public void markStored(String geocode, int listId) {
        storage.markStored(geocode, listId);
    }

    public boolean markDropped(String geocode) {
        return storage.markDropped(geocode);
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
            removeCacheFromCache(geocode);
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

    public void addSearch(final List<cgCache> cacheList, final int listId) {
        if (CollectionUtils.isEmpty(cacheList)) {
            return;
        }

        for (final cgCache cache : cacheList) {
            cache.setListId(listId);
            storeWithMerge(cache, listId >= 1);
        }
    }

    public boolean addCacheToSearch(SearchResult search, cgCache cache) {
        if (search == null || cache == null) {
            return false;
        }

        final boolean status = storeWithMerge(cache, cache.getListId() >= 1);
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
            final cgCache oldCache = storage.loadCache(cache.getGeocode(), LoadFlags.LOADALL);
            cache.gatherMissingFrom(oldCache);
        }
        return storage.saveCache(cache);
    }

    public void dropStored(int listId) {
        storage.dropStored(listId);
    }

    /**
     * {@link cgData#dropCaches(List)}
     */
    public void dropCaches(final List<String> geocodes) {
        storage.dropCaches(geocodes);
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
        storage.saveVisitDate(geocode);
    }

    public void clearVisitDate(String geocode) {
        storage.clearVisitDate(geocode);
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
}
