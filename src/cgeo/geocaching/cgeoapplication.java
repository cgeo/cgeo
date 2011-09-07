package cgeo.geocaching;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.CollectionUtils;

public class cgeoapplication extends Application {

	private cgData storage = null;
	private String action = null;
	private Geopoint lastCoords = null;
	private cgGeo geo = null;
	private boolean geoInUse = false;
	private cgDirection dir = null;
	private boolean dirInUse = false;
	final private Map<UUID, cgSearch> searches = new HashMap<UUID, cgSearch>(); // information about searches
	final private Map<String, cgCache> cachesCache = new HashMap<String, cgCache>(); // caching caches into memory
	public boolean firstRun = true; // c:geo is just launched
	public boolean warnedLanguage = false; // user was warned about different language settings on geocaching.com
	private boolean databaseCleaned = false; // database was cleaned

	public cgeoapplication() {
		if (storage == null) {
			storage = new cgData(this);
		}
	}

	@Override
	public void onLowMemory() {
		Log.i(cgSettings.tag, "Cleaning applications cache.");

		cachesCache.clear();
	}

	@Override
	public void onTerminate() {
		Log.d(cgSettings.tag, "Terminating c:geo...");

		if (geo != null) {
			geo.closeGeo();
			geo = null;
		}

		if (dir != null) {
			dir.closeDir();
			dir = null;
		}

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

	public boolean restoreDatabase() {
		return storage.restoreDatabase();
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

	public cgGeo startGeo(Context context, cgUpdateLoc geoUpdate, cgBase base, cgSettings settings, int time, int distance) {
		if (geo == null) {
			geo = new cgGeo(context, this, geoUpdate, base, settings, time, distance);
			geo.initGeo();

			Log.i(cgSettings.tag, "Location service started");
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

			if (geoInUse == false && geo != null) {
				geo.closeGeo();
				geo = null;

				Log.i(cgSettings.tag, "Location service stopped");
			}
		}
	}

	public cgDirection startDir(Context context, cgUpdateDir dirUpdate) {
		if (dir == null) {
			dir = new cgDirection(context, dirUpdate);
			dir.initDir();

			Log.i(cgSettings.tag, "Direction service started");
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

			if (dirInUse == false && dir != null) {
				dir.closeDir();
				dir = null;

				Log.i(cgSettings.tag, "Direction service stopped");
			}
		}
	}

	public void cleanDatabase(boolean more) {
		if (databaseCleaned) {
			return;
		}

		if (storage == null) {
			storage = new cgData(this);
		}
		storage.clean(more);
		databaseCleaned = true;
	}

	public Boolean isThere(String geocode, String guid, boolean detailed, boolean checkTime) {
		if (storage == null) {
			storage = new cgData(this);
		}
		return storage.isThere(geocode, guid, detailed, checkTime);
	}

	public Boolean isOffline(String geocode, String guid) {
		if (storage == null) {
			storage = new cgData(this);
		}
		return storage.isOffline(geocode, guid);
	}

	public String getGeocode(String guid) {
		if (storage == null) {
			storage = new cgData(this);
		}
		return storage.getGeocodeForGuid(guid);
	}

	public String getCacheid(String geocode) {
		if (storage == null) {
			storage = new cgData(this);
		}
		return storage.getCacheidForGeocode(geocode);
	}

	public String getError(final UUID searchId) {
		if (searchId == null || searches.containsKey(searchId) == false) {
			return null;
		}

		return searches.get(searchId).error;
	}

	public boolean setError(final UUID searchId, String error) {
		if (searchId == null || searches.containsKey(searchId) == false) {
			return false;
		}

		searches.get(searchId).error = error;

		return true;
	}

	public String getUrl(final UUID searchId) {
		if (searchId == null || searches.containsKey(searchId) == false) {
			return null;
		}

		return searches.get(searchId).url;
	}

	public boolean setUrl(final UUID searchId, String url) {
		if (searchId == null || searches.containsKey(searchId) == false) {
			return false;
		}

		searches.get(searchId).url = url;

		return true;
	}

	public String[] getViewstates(final UUID searchId) {
		if (searchId == null || searches.containsKey(searchId) == false) {
			return null;
		}

		return searches.get(searchId).viewstates;
	}

	public boolean setViewstates(final UUID searchId, String[] viewstates) {
		if (ArrayUtils.isEmpty(viewstates)) {
			return false;
		}
		if (searchId == null || searches.containsKey(searchId) == false) {
			return false;
		}

		searches.get(searchId).viewstates = viewstates;

		return true;
	}

	public Integer getTotal(final UUID searchId) {
		if (searchId == null || searches.containsKey(searchId) == false) {
			return null;
		}

		return searches.get(searchId).totalCnt;
	}

	public Integer getCount(final UUID searchId) {
		if (searchId == null || searches.containsKey(searchId) == false) {
			return 0;
		}

		return searches.get(searchId).getCount();
	}

	public Integer getNotOfflineCount(final UUID searchId) {
		if (searchId == null || searches.containsKey(searchId) == false) {
			return 0;
		}

		int count = 0;
		List<String> geocodes = searches.get(searchId).getGeocodes();
		if (geocodes != null) {
			for (String geocode : geocodes) {
				if (isOffline(geocode, null) == false) {
					count++;
				}
			}
		}

		return count;
	}

	public cgCache getCacheByGeocode(String geocode) {
		return getCacheByGeocode(geocode, false, true, false, false, false, false);
	}

	public cgCache getCacheByGeocode(String geocode, boolean loadA, boolean loadW, boolean loadS, boolean loadL, boolean loadI, boolean loadO) {
		if (StringUtils.isBlank(geocode)) {
			return null;
		}

		cgCache cache = null;
		if (cachesCache.containsKey(geocode)) {
			cache = cachesCache.get(geocode);
		} else {
			if (storage == null) {
				storage = new cgData(this);
			}
			cache = storage.loadCache(geocode, null, loadA, loadW, loadS, loadL, loadI, loadO);

			if (cache != null && cache.detailed && loadA && loadW && loadS && loadL && loadI) {
				putCacheInCache(cache);
			}
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
		if (cache == null || cache.geocode == null) {
			return;
		}

		if (cachesCache.containsKey(cache.geocode)) {
			cachesCache.remove(cache.geocode);
		}

		cachesCache.put(cache.geocode, cache);
	}

	public String[] geocodesInCache() {
		if (storage == null) {
			storage = new cgData(this);
		}

		return storage.allDetailedThere();
	}

	public cgWaypoint getWaypointById(Integer id) {
		if (id == null || id == 0) {
			return null;
		}

		if (storage == null) {
			storage = new cgData(this);
		}
		return storage.loadWaypoint(id);
	}

	public List<Object> getBounds(String geocode) {
		if (geocode == null) {
			return null;
		}

		List<String> geocodeList = new ArrayList<String>();
		geocodeList.add(geocode);

		return getBounds(geocodeList);
	}

	public List<Object> getBounds(final UUID searchId) {
		if (searchId == null || searches.containsKey(searchId) == false) {
			return null;
		}

		if (storage == null) {
			storage = new cgData(this);
		}

		final cgSearch search = searches.get(searchId);
		final List<String> geocodeList = search.getGeocodes();

		return getBounds(geocodeList);
	}

	public List<Object> getBounds(List<String> geocodes) {
		if (geocodes == null || geocodes.isEmpty()) {
			return null;
		}

		if (storage == null) {
			storage = new cgData(this);
		}

		return storage.getBounds(geocodes.toArray());
	}

	public cgCache getCache(final UUID searchId) {
		if (searchId == null || searches.containsKey(searchId) == false) {
			return null;
		}

		cgSearch search = searches.get(searchId);
		List<String> geocodeList = search.getGeocodes();

		return getCacheByGeocode(geocodeList.get(0), true, true, true, true, true, true);
	}

	public List<cgCache> getCaches(final UUID searchId) {
		return getCaches(searchId, null, null, null, null, false, true, false, false, false, true);
	}

	public List<cgCache> getCaches(final UUID searchId, boolean loadA, boolean loadW, boolean loadS, boolean loadL, boolean loadI, boolean loadO) {
		return getCaches(searchId, null, null, null, null, loadA, loadW, loadS, loadL, loadI, loadO);
	}

	public List<cgCache> getCaches(final UUID searchId, Long centerLat, Long centerLon, Long spanLat, Long spanLon) {
		return getCaches(searchId, centerLat, centerLon, spanLat, spanLon, false, true, false, false, false, true);
	}

	public List<cgCache> getCaches(final UUID searchId, Long centerLat, Long centerLon, Long spanLat, Long spanLon, boolean loadA, boolean loadW, boolean loadS, boolean loadL, boolean loadI, boolean loadO) {
		if (searchId == null || searches.containsKey(searchId) == false) {
			List<cgCache> cachesOut = new ArrayList<cgCache>();

			final List<cgCache> cachesPre = storage.loadCaches(null , null, centerLat, centerLon, spanLat, spanLon, loadA, loadW, loadS, loadL, loadI, loadO);

			if (cachesPre != null) {
				cachesOut.addAll(cachesPre);
			}

			return cachesOut;
		}

		List<cgCache> cachesOut = new ArrayList<cgCache>();

		cgSearch search = searches.get(searchId);
		List<String> geocodeList = search.getGeocodes();

		if (storage == null) {
			storage = new cgData(this);
		}

		final List<cgCache> cachesPre = storage.loadCaches(geocodeList.toArray(), null, centerLat, centerLon, spanLat, spanLon, loadA, loadW, loadS, loadL, loadI, loadO);
		if (cachesPre != null) {
			cachesOut.addAll(cachesPre);
		}

		return cachesOut;
	}

	public cgSearch getBatchOfStoredCaches(boolean detailedOnly, Double latitude, Double longitude, String cachetype, int list) {
		if (storage == null) {
			storage = new cgData(this);
		}
		cgSearch search = new cgSearch();

		List<String> geocodes = storage.loadBatchOfStoredGeocodes(detailedOnly, latitude, longitude, cachetype, list);
		if (geocodes != null && geocodes.isEmpty() == false) {
			for (String gccode : geocodes) {
				search.addGeocode(gccode);
			}
		}
		searches.put(search.getCurrentId(), search);

		return search;
	}

	public List<cgDestination> getHistoryOfSearchedLocations() {
		if (storage == null) {
			storage = new cgData(this);
		}

		return storage.loadHistoryOfSearchedLocations();
	}

	public cgSearch getHistoryOfCaches(boolean detailedOnly, String cachetype) {
		if (storage == null) {
			storage = new cgData(this);
		}
		cgSearch search = new cgSearch();

		List<String> geocodes = storage.loadBatchOfHistoricGeocodes(detailedOnly, cachetype);
		if (geocodes != null && geocodes.isEmpty() == false) {
			for (String gccode : geocodes) {
				search.addGeocode(gccode);
			}
		}
		searches.put(search.getCurrentId(), search);

		return search;
	}

	public UUID getCachedInViewport(Long centerLat, Long centerLon, Long spanLat, Long spanLon, String cachetype) {
		if (storage == null) {
			storage = new cgData(this);
		}
		cgSearch search = new cgSearch();

		List<String> geocodes = storage.getCachedInViewport(centerLat, centerLon, spanLat, spanLon, cachetype);
		if (geocodes != null && geocodes.isEmpty() == false) {
			for (String gccode : geocodes) {
				search.addGeocode(gccode);
			}
		}
		searches.put(search.getCurrentId(), search);

		return search.getCurrentId();
	}

	public UUID getStoredInViewport(Long centerLat, Long centerLon, Long spanLat, Long spanLon, String cachetype) {
		if (storage == null) {
			storage = new cgData(this);
		}
		cgSearch search = new cgSearch();

		List<String> geocodes = storage.getStoredInViewport(centerLat, centerLon, spanLat, spanLon, cachetype);
		if (geocodes != null && geocodes.isEmpty() == false) {
			for (String gccode : geocodes) {
				search.addGeocode(gccode);
			}
		}
		searches.put(search.getCurrentId(), search);

		return search.getCurrentId();
	}

	public UUID getOfflineAll(String cachetype) {
		if (storage == null) {
			storage = new cgData(this);
		}
		cgSearch search = new cgSearch();

		List<String> geocodes = storage.getOfflineAll(cachetype);
		if (geocodes != null && geocodes.isEmpty() == false) {
			for (String gccode : geocodes) {
				search.addGeocode(gccode);
			}
		}
		searches.put(search.getCurrentId(), search);

		return search.getCurrentId();
	}

	public int getAllStoredCachesCount(boolean detailedOnly, String cachetype, Integer list) {
		if (storage == null) {
			storage = new cgData(this);
		}

		return storage.getAllStoredCachesCount(detailedOnly, cachetype, list);
	}

	public int getAllHistoricCachesCount(boolean detailedOnly, String cachetype) {
		if (storage == null) {
			storage = new cgData(this);
		}

		return storage.getAllHistoricCachesCount(detailedOnly, cachetype);
	}

	public void markStored(String geocode, int listId) {
		if (storage == null) {
			storage = new cgData(this);
		}
		storage.markStored(geocode, listId);
	}

	public boolean markDropped(String geocode) {
		if (storage == null) {
			storage = new cgData(this);
		}
		return storage.markDropped(geocode);
	}

	public boolean markFound(String geocode) {
		if (storage == null) {
			storage = new cgData(this);
		}
		return storage.markFound(geocode);
	}

	public boolean clearSearchedDestinations() {
		if (storage == null) {
			storage = new cgData(this);
		}

		return storage.clearSearchedDestinations();
	}

	public boolean saveSearchedDestination(cgDestination destination) {
		if (storage == null) {
			storage = new cgData(this);
		}

		return storage.saveSearchedDestination(destination);
	}

	public boolean saveWaypoints(String geocode, List<cgWaypoint> waypoints, boolean drop) {
		if (storage == null) {
			storage = new cgData(this);
		}
		return storage.saveWaypoints(geocode, waypoints, drop);
	}

	public boolean saveOwnWaypoint(int id, String geocode, cgWaypoint waypoint) {
		if (storage == null) {
			storage = new cgData(this);
		}
		return storage.saveOwnWaypoint(id, geocode, waypoint);
	}

	public boolean deleteWaypoint(int id) {
		if (storage == null) {
			storage = new cgData(this);
		}
		return storage.deleteWaypoint(id);
	}

	public boolean saveTrackable(cgTrackable trackable) {
		if (storage == null) {
			storage = new cgData(this);
		}

		final List<cgTrackable> list = new ArrayList<cgTrackable>();
		list.add(trackable);

		return storage.saveInventory("---", list);
	}

	public void addGeocode(final UUID searchId, String geocode) {
		if (this.searches.containsKey(searchId) == false || StringUtils.isBlank(geocode)) {
			return;
		}

		this.searches.get(searchId).addGeocode(geocode);
	}

	public UUID addSearch(final UUID searchId, List<cgCache> cacheList, Boolean newItem, int reason) {
		if (this.searches.containsKey(searchId) == false) {
			return null;
		}

		cgSearch search = this.searches.get(searchId);

		return addSearch(search, cacheList, newItem, reason);
	}

	public UUID addSearch(final cgSearch search, final List<cgCache> cacheList, final boolean newItem, final int reason) {
		if (CollectionUtils.isEmpty(cacheList)) {
			return null;
		}

		final UUID searchId = search.getCurrentId();
		searches.put(searchId, search);

		if (storage == null) {
			storage = new cgData(this);
		}
		if (newItem) {
			// save only newly downloaded data
			for (cgCache cache : cacheList) {
				String geocode = cache.geocode.toUpperCase();
				String guid = cache.guid.toLowerCase();

				cache.reason = reason;

				if (storage.isThere(geocode, guid, false, false)) {
					cgCache mergedCache = cache.merge(storage);
					storage.saveCache(mergedCache);
				} else {
					// cache is not saved, new data are for storing
					storage.saveCache(cache);
				}
			}
		}

		return searchId;
	}

	public boolean addCacheToSearch(cgSearch search, cgCache cache) {
		if (search == null || cache == null) {
			return false;
		}

		final UUID searchId = search.getCurrentId();

		if (searches.containsKey(searchId) == false) {
			searches.put(searchId, search);
		}

		String geocode = cache.geocode.toUpperCase();
		String guid = cache.guid.toLowerCase();

		boolean status = false;

		if (storage.isThere(geocode, guid, false, false) == false || cache.reason >= 1) { // if for offline, do not merge
			status = storage.saveCache(cache);
		} else {
			cgCache mergedCache = cache.merge(storage);

			status = storage.saveCache(mergedCache);
		}

		if (status) {
			search.addGeocode(cache.geocode);
		}

		return status;
	}

	public void dropStored(int listId) {
		if (storage == null) {
			storage = new cgData(this);
		}
		storage.dropStored(listId);
	}

	public List<cgTrackable> loadInventory(String geocode) {
		return storage.loadInventory(geocode);
	}

	public Map<Integer,Integer> loadLogCounts(String geocode) {
		return storage.loadLogCounts(geocode);
	}

	public List<cgImage> loadSpoilers(String geocode) {
		return storage.loadSpoilers(geocode);
	}

	public cgWaypoint loadWaypoint(int id) {
		return storage.loadWaypoint(id);
	}

	public void setAction(String act) {
		action = act;
	}

	public String getAction() {
		if (action == null) {
			return "";
		}
		return action;
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

	public void setLastLoc(final Geopoint coords) {
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

	public boolean removeList(int id) {
		return storage.removeList(id);
	}

	public boolean removeSearchedDestinations(cgDestination destination) {
		return storage.removeSearchedDestination(destination);
	}

	public void moveToList(String geocode, int listId) {
		storage.moveToList(geocode, listId);
	}
}
