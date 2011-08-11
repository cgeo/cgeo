package cgeo.geocaching;

public class cgCoord {

	public Integer id = null;
	public String geocode = "";
	public String type = "cache";
	public String typeSpec = "traditional";
	public String name = "";
	public boolean found = false;
	public boolean disabled = false;
	public Double latitude = Double.valueOf(0);
	public Double longitude = Double.valueOf(0);
	public Float difficulty = null;
	public Float terrain = null;
	public String size = null;

	public cgCoord() {
	}

	public cgCoord(cgCache cache) {
		disabled = cache.disabled;
		found = cache.found;
		geocode = cache.geocode;
		latitude = cache.latitude;
		longitude = cache.longitude;
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
		latitude = waypoint.latitude;
		longitude = waypoint.longitude;
		name = waypoint.name;
		type = "waypoint";
		typeSpec = waypoint.type;
	}
}
