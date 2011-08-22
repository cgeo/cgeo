package cgeo.geocaching;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.text.Spannable;
import android.util.Log;
import cgeo.geocaching.activity.IAbstractActivity;

public class cgCache {

	public Long updated = null;
	public Long detailedUpdate = null;
	public Long visitedDate = null;
	public Integer reason = 0;
	public Boolean detailed = false;
	public String geocode = "";
	public String cacheid = "";
	public String guid = "";
	public String type = "";
	public String name = "";
	public Spannable nameSp = null;
	public String owner = "";
	public String ownerReal = "";
	public Date hidden = null;
	public String hint = "";
	public String size = "";
	public Float difficulty = Float.valueOf(0);
	public Float terrain = Float.valueOf(0);
	public Double direction = null;
	public Double distance = null;
	public String latlon = "";
	public String latitudeString = "";
	public String longitudeString = "";
	public String location = "";
	public Double latitude = null;
	public Double longitude = null;
	public boolean reliableLatLon = false;
	public Double elevation = null;
	public String personalNote = null;
	public String shortdesc = "";
	public String description = "";
	public boolean disabled = false;
	public boolean archived = false;
	public boolean members = false;
	public boolean found = false;
	public boolean favourite = false;
	public boolean own = false;
	public Integer favouriteCnt = null;
	public Float rating = null;
	public Integer votes = null;
	public Float myVote = null;
	public int inventoryItems = 0;
	public boolean onWatchlist = false;
	public ArrayList<String> attributes = null;
	public ArrayList<cgWaypoint> waypoints = null;
	public ArrayList<cgImage> spoilers = null;
	public ArrayList<cgLog> logs = null;
	public ArrayList<cgTrackable> inventory = null;
	public HashMap<Integer, Integer> logCounts = new HashMap<Integer, Integer>();
	public boolean logOffline = false;
	// temporary values
	public boolean statusChecked = false;
	public boolean statusCheckedView = false;
	public String directionImg = null;

	public cgCache merge(cgData storage) {

		boolean loadA = true;
		boolean loadW = true;
		boolean loadS = true;
		boolean loadL = true;
		boolean loadI = true;

		if (attributes == null || attributes.isEmpty()) {
			loadA = false;
		}
		if (waypoints == null || waypoints.isEmpty()) {
			loadW = false;
		}
		if (spoilers == null || spoilers.isEmpty()) {
			loadS = false;
		}
		if (logs == null || logs.isEmpty()) {
			loadL = false;
		}
		if (inventory == null || inventory.isEmpty()) {
			loadI = false;
		}

		final cgCache oldCache = storage.loadCache(geocode, guid, loadA, loadW, loadS, loadL, loadI, false);

		if (oldCache == null) {
			return this;
		}

		updated = System.currentTimeMillis();
		if (detailed == false && oldCache.detailed) {
			detailed = true;
			detailedUpdate = System.currentTimeMillis();
		}

		if (visitedDate == null || visitedDate == 0) {
			visitedDate = oldCache.visitedDate;
		}
		if (reason == null || reason == 0) {
			reason = oldCache.reason;
		}
		if (geocode == null || geocode.length() == 0) {
			geocode = oldCache.geocode;
		}
		if (cacheid == null || cacheid.length() == 0) {
			cacheid = oldCache.cacheid;
		}
		if (guid == null || guid.length() == 0) {
			guid = oldCache.guid;
		}
		if (type == null || type.length() == 0) {
			type = oldCache.type;
		}
		if (name == null || name.length() == 0) {
			name = oldCache.name;
		}
		if (nameSp == null || nameSp.length() == 0) {
			nameSp = oldCache.nameSp;
		}
		if (owner == null || owner.length() == 0) {
			owner = oldCache.owner;
		}
		if (ownerReal == null || ownerReal.length() == 0) {
			ownerReal = oldCache.ownerReal;
		}
		if (hidden == null) {
			hidden = oldCache.hidden;
		}
		if (hint == null || hint.length() == 0) {
			hint = oldCache.hint;
		}
		if (size == null || size.length() == 0) {
			size = oldCache.size;
		}
		if (difficulty == null || difficulty == 0) {
			difficulty = oldCache.difficulty;
		}
		if (terrain == null || terrain == 0) {
			terrain = oldCache.terrain;
		}
		if (direction == null) {
			direction = oldCache.direction;
		}
		if (distance == null) {
			distance = oldCache.distance;
		}
		if (latlon == null || latlon.length() == 0) {
			latlon = oldCache.latlon;
		}
		if (latitudeString == null || latitudeString.length() == 0) {
			latitudeString = oldCache.latitudeString;
		}
		if (longitudeString == null || longitudeString.length() == 0) {
			longitudeString = oldCache.longitudeString;
		}
		if (location == null || location.length() == 0) {
			location = oldCache.location;
		}
		if (latitude == null) {
			latitude = oldCache.latitude;
		}
		if (longitude == null) {
			longitude = oldCache.longitude;
		}
		if (elevation == null) {
			elevation = oldCache.elevation;
		}
		if (personalNote == null || personalNote.length() == 0) {
			personalNote = oldCache.personalNote;
		}
		if (shortdesc == null || shortdesc.length() == 0) {
			shortdesc = oldCache.shortdesc;
		}
		if (description == null || description.length() == 0) {
			description = oldCache.description;
		}
		if (favouriteCnt == null) {
			favouriteCnt = oldCache.favouriteCnt;
		}
		if (rating == null) {
			rating = oldCache.rating;
		}
		if (votes == null) {
			votes = oldCache.votes;
		}
		if (myVote == null) {
			myVote = oldCache.myVote;
		}
		if (inventoryItems == 0) {
			inventoryItems = oldCache.inventoryItems;
		}
		if (attributes == null) {
			attributes = oldCache.attributes;
		}
		if (waypoints == null) {
			waypoints = oldCache.waypoints;
		}
		cgWaypoint.mergeWayPoints(waypoints, oldCache.waypoints);
		if (spoilers == null) {
			spoilers = oldCache.spoilers;
		}
		if (inventory == null) {
			inventory = oldCache.inventory;
		}
		if (logs == null || logs.isEmpty()) { // keep last known logs if none
			logs = oldCache.logs;
		}

		return this;
	}

	public boolean hasTrackables(){
		return inventoryItems > 0;
	}

	public boolean canBeAddedToCalendar() {
		// is event type?
		if (!isEventCache()) {
			return false;
		}
		// has event date set?
		if (hidden == null) {
			return false;
		}
		// is in future?
		Date today = new Date();
		today.setHours(0);
		today.setMinutes(0);
		today.setSeconds(0);
		if (hidden.compareTo(today) <= 0) {
			return false;
		}
		return true;
	}

	/**
	 * checks if a page contains the guid of a cache
	 *
	 * @param cache  the cache to look for
	 * @param page   the page to search in
	 *
	 * @return  true: page contains guid of cache, false: otherwise
	 */
	boolean isGuidContainedInPage(final String page) {
		// check if the guid of the cache is anywhere in the page
		if (guid == null  || guid.length() == 0) {
			return false;
		}
		Pattern patternOk = Pattern.compile(guid, Pattern.CASE_INSENSITIVE);
		Matcher matcherOk = patternOk.matcher(page);
		if (matcherOk.find()) {
			Log.d(cgSettings.tag, "cgCache.isGuidContainedInPage: guid '" + guid + "' found");
			return true;
		} else {
			Log.d(cgSettings.tag, "cgCache.isGuidContainedInPage: guid '" + guid + "' not found");
			return false;
		}
	}

	public boolean isEventCache() {
		return ("event".equalsIgnoreCase(type) || "mega".equalsIgnoreCase(type) || "cito".equalsIgnoreCase(type));
	}

	public boolean logVisit(IAbstractActivity fromActivity) {
		if (cacheid == null || cacheid.length() == 0) {
			fromActivity.showToast(((Activity)fromActivity).getResources().getString(R.string.err_cannot_log_visit));
			return true;
		}
		Intent logVisitIntent = new Intent((Activity)fromActivity, cgeovisit.class);
		logVisitIntent.putExtra(cgeovisit.EXTRAS_ID, cacheid);
		logVisitIntent.putExtra(cgeovisit.EXTRAS_GEOCODE, geocode.toUpperCase());
		logVisitIntent.putExtra(cgeovisit.EXTRAS_FOUND, found);

		((Activity)fromActivity).startActivity(logVisitIntent);

		return true;
	}
	
	public boolean logOffline(final IAbstractActivity fromActivity, final int logType) {
		logOffline(fromActivity, "", Calendar.getInstance(), logType);
		return true;
	}
	
	void logOffline(final IAbstractActivity fromActivity, final String log, Calendar date, final int logType) {
		if (logType <= 0) {
			return;
		}
		cgeoapplication app = (cgeoapplication)((Activity)fromActivity).getApplication();
		final boolean status = app.saveLogOffline(geocode, date.getTime(), logType, log);
		
		Resources res = ((Activity)fromActivity).getResources();
		if (status) {
			fromActivity.showToast(res.getString(R.string.info_log_saved));
			app.saveVisitDate(geocode);
		} else {
			fromActivity.showToast(res.getString(R.string.err_log_post_failed));
		}
	}
	
	public ArrayList<Integer> getPossibleLogTypes(cgSettings settings) {
		boolean isOwner = owner != null && owner.equalsIgnoreCase(settings.getUsername());
		ArrayList<Integer> types = new ArrayList<Integer>();
		if ("event".equals(type) || "mega".equals(type) || "cito".equals(type) || "lostfound".equals(type)) {
			types.add(cgBase.LOG_WILL_ATTEND);
			types.add(cgBase.LOG_NOTE);
			types.add(cgBase.LOG_ATTENDED);
			types.add(cgBase.LOG_NEEDS_ARCHIVE);
			if (isOwner) {
				types.add(cgBase.LOG_ANNOUNCEMENT);
			}
		} else if ("webcam".equals(type)) {
			types.add(cgBase.LOG_WEBCAM_PHOTO_TAKEN);
			types.add(cgBase.LOG_DIDNT_FIND_IT);
			types.add(cgBase.LOG_NOTE);
			types.add(cgBase.LOG_NEEDS_ARCHIVE);
			types.add(cgBase.LOG_NEEDS_MAINTENANCE);
		} else {
			types.add(cgBase.LOG_FOUND_IT);
			types.add(cgBase.LOG_DIDNT_FIND_IT);
			types.add(cgBase.LOG_NOTE);
			types.add(cgBase.LOG_NEEDS_ARCHIVE);
			types.add(cgBase.LOG_NEEDS_MAINTENANCE);
		}
		if (isOwner) {
			types.add(cgBase.LOG_OWNER_MAINTENANCE);
			types.add(cgBase.LOG_TEMP_DISABLE_LISTING);
			types.add(cgBase.LOG_ENABLE_LISTING);
			types.add(cgBase.LOG_ARCHIVE);
			types.remove(Integer.valueOf(cgBase.LOG_UPDATE_COORDINATES));
		}
		return types;
	}

	public void openInBrowser(Activity fromActivity) {
		fromActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getCacheUrl() + geocode)));
	}

	private String getCacheUrl() {
		if (geocode.startsWith("OC")) { // TODO refactor method into connectors, once available
			return "http://www.opencaching.de/viewcache.php?wp=";
		}
		return "http://www.geocaching.com/seek/cache_details.aspx?wp=";
	}

}
