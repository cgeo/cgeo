package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.GeoObject;
import cgeo.geocaching.location.Geopoint;

import android.graphics.Color;

import androidx.annotation.ColorInt;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.cocoahero.android.geojson.Geometry;
import com.cocoahero.android.geojson.GeometryCollection;
import com.cocoahero.android.geojson.LineString;
import com.cocoahero.android.geojson.MultiLineString;
import com.cocoahero.android.geojson.MultiPoint;
import com.cocoahero.android.geojson.MultiPolygon;
import com.cocoahero.android.geojson.Point;
import com.cocoahero.android.geojson.Polygon;
import com.cocoahero.android.geojson.Position;
import com.cocoahero.android.geojson.Ring;
import org.json.JSONException;
import org.json.JSONObject;

/** Utility class to work with GeoJson */
public class GeoJsonUtils {

    private static final boolean UNIT_TEST_MODE = CgeoApplication.getInstance() == null;

    //for storage of intermediate values
    private static class GeoJsonProperties {
        public  Integer strokeColor;
        public  Float strokeWidth;
        public  Integer fillColor;
    }

    private GeoJsonUtils() {
        //no instance
    }

    public static List<GeoObject> parseGeoJson(final InputStream is) throws JSONException, IOException {
        final List<GeoObject> result = new ArrayList<>();
        parseGeoJson(GeoJSON.parse(is), null, result);
        return result;
    }

    public static List<GeoObject> parseGeoJson(final String string) throws JSONException {
        final List<GeoObject> result = new ArrayList<>();
        parseGeoJson(GeoJSON.parse(string), null, result);
        return result;
    }

    private static void parseGeoJson(final GeoJSONObject geoJson, final GeoJsonProperties props, final List<GeoObject> list) throws JSONException {
        if (geoJson instanceof Feature) {
            parseGeoJsonFeature((Feature) geoJson, list);
        } else if (geoJson instanceof FeatureCollection) {
            parseGeoJsonFeatureCollection((FeatureCollection) geoJson, list);
        } else if (geoJson instanceof Point) {
            parseGeoJsonPoint((Point) geoJson, props, list);
        } else if (geoJson instanceof MultiPoint) {
            parseGeoJsonMultiPoint((MultiPoint) geoJson, props, list);
        } else if (geoJson instanceof LineString) {
            parseGeoJsonLineString((LineString) geoJson, props, list);
        } else if (geoJson instanceof MultiLineString) {
            parseGeoJsonMultiLineString((MultiLineString) geoJson, props, list);
        } else if (geoJson instanceof Polygon) {
            parseGeoJsonPolygon((Polygon) geoJson, props, list);
        } else if (geoJson instanceof MultiPolygon) {
            parseGeoJsonMultiPolygon((MultiPolygon) geoJson, props, list);
        } else if (geoJson instanceof GeometryCollection) {
            parseGeoJsonGeometryCollection((GeometryCollection) geoJson, props, list);
        } else {
            throw new JSONException("Unexpected Type in GeoJson: " + geoJson.getType());
        }

    }

    private static void parseGeoJsonFeature(final Feature feature, final List<GeoObject> list) throws JSONException {
        final GeoJsonProperties p = parseProperties(feature.getProperties());
        parseGeoJson(feature.getGeometry(), p, list);
    }

    private static void parseGeoJsonFeatureCollection(final FeatureCollection featureCollection, final List<GeoObject> list) throws JSONException {
        for (Feature feature : featureCollection.getFeatures()) {
            parseGeoJsonFeature(feature, list);
        }
    }

    private static void parseGeoJsonPoint(final Point point, final GeoJsonProperties props, final List<GeoObject> list) {
        list.add(GeoObject.createPoint(posToGeopoint(point.getPosition()), props.strokeColor, props.strokeWidth));
    }

    private static void parseGeoJsonMultiPoint(final MultiPoint multiPoint, final GeoJsonProperties props, final List<GeoObject> list) {
        for (Position p : multiPoint.getPositions()) {
            list.add(GeoObject.createPoint(posToGeopoint(p), props.strokeColor, props.strokeWidth));
        }
    }

    private static void parseGeoJsonLineString(final LineString lineString, final GeoJsonProperties props, final List<GeoObject> list) {
        list.add(GeoObject.createPolyline(CollectionStream.of(lineString.getPositions()).map(GeoJsonUtils::posToGeopoint).toList(), props.strokeColor, props.strokeWidth));
    }

    private static void parseGeoJsonMultiLineString(final MultiLineString multiLineString, final GeoJsonProperties props, final List<GeoObject> list) {
        for (LineString ls : multiLineString.getLineStrings()) {
            parseGeoJsonLineString(ls, props, list);
        }
    }

    private static void parseGeoJsonPolygon(final Polygon polygon, final GeoJsonProperties props, final List<GeoObject> list) {
        //as of now, polygon rings are parsed as separate polygons
        for (Ring r : polygon.getRings()) {
            list.add(GeoObject.createPolygon(posToGeopoints(r.getPositions()), props.strokeColor, props.strokeWidth, props.fillColor));
        }
    }

    private static void parseGeoJsonMultiPolygon(final MultiPolygon multiPolygon, final GeoJsonProperties props, final List<GeoObject> list) {
        for (Polygon polygon : multiPolygon.getPolygons()) {
            parseGeoJsonPolygon(polygon, props, list);
        }
    }

    private static void parseGeoJsonGeometryCollection(final GeometryCollection geometryCollection, final GeoJsonProperties props, final List<GeoObject> list) throws JSONException {
        for (Geometry g : geometryCollection.getGeometries()) {
            parseGeoJson(g, props, list);
        }
    }

    private static Geopoint posToGeopoint(final Position pos) {
        return new Geopoint(pos.getLatitude(), pos.getLongitude());
    }

    private static List<Geopoint> posToGeopoints(final Collection<Position> pos) {
        return CollectionStream.of(pos).map(GeoJsonUtils::posToGeopoint).toList();
    }

    private static GeoJsonProperties parseProperties(final JSONObject json) {
        final GeoJsonProperties result = new GeoJsonProperties();
        result.strokeColor = colorFromJson(result.strokeColor, json, "marker-color", "marker-opacity");
        result.strokeColor = colorFromJson(result.strokeColor, json, "stroke", "stroke-opacity");
        result.fillColor = colorFromJson(result.fillColor, json, "fill", "fill-opacity");
        result.strokeWidth = floatFromJson(result.strokeWidth, json, "stroke-width");
        return result;
    }

    private static Float floatFromJson(final Float currentFloat, final JSONObject json, final String key) {
        if (json.has(key)) {
            final double value = json.optDouble(key, Double.NaN);
            if (!Double.isNaN(value)) {
                return (float) value;
            }
        }
        return currentFloat;
    }

    /** In UNIT TEST mode, the resulting color is the number of chars of the 'key' string plus 1000*opacity value */
    @ColorInt
    private static Integer colorFromJson(final Integer currentColor, final JSONObject json, final String key, final String keyOpacity) {
        String colorString = null;
        double opacity = 1d;
        try {
            if (json.has(key)) {
                colorString = json.getString(key);
                int color = UNIT_TEST_MODE ? colorString.length() : Color.parseColor(colorString);
                opacity = keyOpacity == null ? Double.NaN : json.optDouble(keyOpacity, Double.NaN);
                if (opacity >= 0d && opacity <= 1d) {
                    color = UNIT_TEST_MODE ? color + (int) (opacity * 1000) :
                            Color.argb((int) (opacity * 255), Color.red(color), Color.green(color), Color.blue(color));
                }
                return color;
            }
        } catch (JSONException | IllegalArgumentException ex) {
            Log.w("Problems parsing color in json: '" + key + "'='" + colorString + "', '" + keyOpacity + "'=" + opacity, ex);
        }
        return currentColor;
    }


}
