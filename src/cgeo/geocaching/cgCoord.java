package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.geopoint.Geopoint;

public class cgCoord {

	public Integer id = null;
	public String geocode = "";
	public String type = "cache";
	public String typeSpec = "traditional";
	public String name = "";
	public boolean found = false;
	public boolean disabled = false;
	public Geopoint coords = new Geopoint(0, 0);
	public Float difficulty = null;
	public Float terrain = null;
	public CacheSize size = null;

	public cgCoord() {
	}

	public cgCoord(cgCache cache) {
		disabled = cache.disabled;
		found = cache.found;
		geocode = cache.geocode;
		coords = cache.coords;
		name = cache.name;
		type = "cache";
		typeSpec = cache.type;
		difficulty = cache.difficulty;
		terrain = cache.terrain;
		size = cache.size;
	}

	public cgCoord(cgWaypoint waypoint) {
		id = waypoint.id;
		disabled = false;
		found = false;
		geocode = "";
		coords = waypoint.coords;
		name = waypoint.name;
		type = "waypoint";
		typeSpec = waypoint.type;
	}
}
