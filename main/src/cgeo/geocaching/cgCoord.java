package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;

public class cgCoord implements IBasicCache, IWaypoint {

    private Integer id = null; // only valid if constructed with a waypoint
    private WaypointType waypointType = null; // only valid if constructed with a waypoint
    private String guid = null; // only valid if constructed with a cache
    private CacheType cacheType = null; // only valid if constructed with a cache
    private String geocode = "";
    private String coordType = "cache"; // used values: { cache, waypoint }
    private String typeSpec = CacheType.TRADITIONAL.id;
    private String name = "";
    private boolean found = false;
    private boolean disabled = false;
    private Geopoint coords = new Geopoint(0, 0);
    private Float difficulty = null;
    private Float terrain = null;
    private CacheSize size = null;

    public cgCoord() {
    }

    public cgCoord(cgCache cache) {
        guid = cache.getGuid();
        disabled = cache.isDisabled();
        found = cache.isFound();
        geocode = cache.getGeocode();
        coords = cache.getCoords();
        name = cache.getName();
        coordType = "cache";
        typeSpec = cache.getType().id;
        difficulty = cache.getDifficulty();
        terrain = cache.getTerrain();
        size = cache.getSize();
        cacheType = cache.getType();
    }

    public cgCoord(cgWaypoint waypoint) {
        id = waypoint.getId();
        disabled = false;
        found = false;
        geocode = "";
        coords = waypoint.getCoords();
        name = waypoint.getName();
        coordType = "waypoint";
        typeSpec = waypoint.getWaypointType() != null ? waypoint.getWaypointType().id : null;
        waypointType = waypoint.getWaypointType();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getGeocode() {
        return geocode;
    }

    public void setGeocode(String geocode) {
        this.geocode = geocode;
    }

    public String getCoordType() {
        return coordType;
    }

    public void setCoordType(String type) {
        this.coordType = type;
    }

    public String getTypeSpec() {
        return typeSpec;
    }

    public void setTypeSpec(String typeSpec) {
        this.typeSpec = typeSpec;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Geopoint getCoords() {
        return coords;
    }

    public void setCoords(Geopoint coords) {
        this.coords = coords;
    }

    @Override
    public Float getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Float difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    public Float getTerrain() {
        return terrain;
    }

    public void setTerrain(Float terrain) {
        this.terrain = terrain;
    }

    @Override
    public CacheSize getSize() {
        return size;
    }

    public void setSize(CacheSize size) {
        this.size = size;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @Override
    public String getGuid() {
        return guid;
    }

    @Override
    public WaypointType getWaypointType() {
        return waypointType;
    }

    @Override
    public CacheType getType() {
        return cacheType;
    }
}
