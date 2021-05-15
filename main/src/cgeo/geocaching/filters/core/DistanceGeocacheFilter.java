package cgeo.geocaching.filters.core;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.GeopointParser;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.BooleanUtils;

public class DistanceGeocacheFilter extends NumberRangeGeocacheFilter<Float> {

    private Geopoint coordinate;
    private boolean useCurrentPosition;

    public DistanceGeocacheFilter() {
        super(Float::valueOf, f -> f);
    }

    /** Gets fixed-value coordinate set to this filter, may be null */
    @Nullable
    public Geopoint getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(final Geopoint coordinate) {
        this.coordinate = coordinate;
    }

    public boolean isUseCurrentPosition() {
        return useCurrentPosition;
    }

    public void setUseCurrentPosition(final boolean useCurrentPosition) {
        this.useCurrentPosition = useCurrentPosition;
    }

    @Override
    protected Float getValue(final Geocache cache) {
        final Geopoint gp = (useCurrentPosition || coordinate == null)  ?
            Sensors.getInstance().currentGeo().getCoords() : coordinate;

        return gp.distanceTo(cache.getCoords());
    }

    /** Returns the coordinate which will effectively be used for calculation (either fixed-value or current position) */
    @NonNull
    public Geopoint getEffectiveCoordinate() {
        return (useCurrentPosition || coordinate == null)  ?
            Sensors.getInstance().currentGeo().getCoords() : coordinate;
    }

    @Override
    public void setConfig(final String[] value) {
        super.setConfig(value);
        useCurrentPosition = value.length > 2 && BooleanUtils.toBoolean(value[2]);
        coordinate = value.length > 3 && !"-".equals(value[3]) ? GeopointParser.parse(value[3], null) : null;
    }

    @Override
    public String[] getConfig() {
        final String[] superConfig = super.getConfig();
        return new String[]{superConfig[0], superConfig[1],
            Boolean.toString(useCurrentPosition), coordinate == null ? "-" : GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE, coordinate)};
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        final Geopoint gp = (useCurrentPosition || coordinate == null)  ?
            Sensors.getInstance().currentGeo().getCoords() : coordinate;
        final String sql = getSqlDistanceSquare(
            sqlBuilder.getMainTableId() + ".latitude", sqlBuilder.getMainTableId() + ".longitude", (float) gp.getLatitude(), (float) gp.getLongitude());

        addRangeToSqlBuilder(sqlBuilder, sql, v -> v * v);
    }


    private static String getSqlDistanceSquare(final String lat1, final String lon1, final float lat2, final float lon2) {
        //This is SQL! So we use a simplified distance calculation here, according to: https://www.mkompf.com/gps/distcalc.html
        //distance = sqrt(dx * dx + dy * dy)
        //with distance: Distance in km
        //dx = 111.3 * cos(lat) * (lon1 - lon2)
        //lat = (lat1 + lat2) / 2 * 0.01745
        //dy = 111.3 * (lat1 - lat2)
        //lat1, lat2, lon1, lon2: Latitude, Longitude in degrees (not radians!)

        //Unfortunately, SQLite in our version does not know functions like COS, SQRT or PI. So we have to perform some tricks...
        final String dxExceptLon1Lon2Square = String.valueOf(Math.pow(Math.cos(lat2 * Math.PI / 180 * 0.01745) * 111.3, 2));
        final String dyExceptLat1Lat2Square = String.valueOf(Math.pow(111.3, 2));

        final String dxSquare = "(" + dxExceptLon1Lon2Square + " * (" + lon1 + " - " + lon2 + ") * (" + lon1 + " - " + lon2 + "))";
        final String dySquare = "(" + dyExceptLat1Lat2Square + " * (" + lat1 + " - " + lat2 + ") * (" + lat1 + " - " + lat2 + "))";

        final String dist = "(" + dxSquare + " + " + dySquare + ")";
        return dist;
    }

}
