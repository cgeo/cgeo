package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;

import android.graphics.Color;

import androidx.annotation.ColorInt;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import org.json.JSONException;
import org.json.JSONObject;

/** Utility class to work with GeoJson */
public class GeoJsonUtils {

    private static final boolean UNIT_TEST_MODE = CgeoApplication.getInstance() == null;

    private static final GeopointConverter<Position> GP_CONVERTER = new GeopointConverter<>(
            gp -> new Position(gp.getLatitude(), gp.getLongitude()),
            p -> new Geopoint(p.getLatitude(), p.getLongitude()));

    //for storage of intermediate values
    private static class GeoJsonProperties {
        public  Integer strokeColor;
        public  Float strokeWidth;
        public  Integer fillColor;
    }

    private GeoJsonUtils() {
        //no instance
    }

    public static GeoItem parseGeoJson(final InputStream is) throws JSONException, IOException {
        final List<GeoPrimitive> result = new ArrayList<>();
        parseGeoJson(GeoJSON.parse(is), null, result);
        return listToItem(result);
    }

    public static GeoItem parseGeoJson(final String string) throws JSONException {
        final List<GeoPrimitive> result = new ArrayList<>();
        parseGeoJson(GeoJSON.parse(string), null, result);
        return listToItem(result);
    }

    private static GeoItem listToItem(final List<GeoPrimitive> items) {
        if (items == null) {
            return GeoGroup.create();
        }
        //Sole invalid items should not make whole GeoJson unrenderable. Thus remove invalid items. See e.g. #15074
        CommonUtils.filterCollection(items, item -> item != null && item.isValid());

        if (items.size() == 1) {
            return items.get(0);
        }
        return GeoGroup.create(items);
    }

    private static void parseGeoJson(final GeoJSONObject geoJson, final GeoJsonProperties props, final List<GeoPrimitive> list) throws JSONException {
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

    private static void parseGeoJsonFeature(final Feature feature, final List<GeoPrimitive> list) throws JSONException {
        final GeoJsonProperties p = parseProperties(feature.getProperties());
        parseGeoJson(feature.getGeometry(), p, list);
    }

    private static void parseGeoJsonFeatureCollection(final FeatureCollection featureCollection, final List<GeoPrimitive> list) throws JSONException {
        for (Feature feature : featureCollection.getFeatures()) {
            parseGeoJsonFeature(feature, list);
        }
    }

    private static void parseGeoJsonPoint(final Point point, final GeoJsonProperties props, final List<GeoPrimitive> list) {
        list.add(GeoPrimitive.createPoint(GP_CONVERTER.from(point.getPosition()), toGeoStyle(props)));
    }

    private static void parseGeoJsonMultiPoint(final MultiPoint multiPoint, final GeoJsonProperties props, final List<GeoPrimitive> list) {
        for (Position p : multiPoint.getPositions()) {
            list.add(GeoPrimitive.createPoint(GP_CONVERTER.from(p), toGeoStyle(props)));
        }
    }

    private static void parseGeoJsonLineString(final LineString lineString, final GeoJsonProperties props, final List<GeoPrimitive> list) {
        list.add(GeoPrimitive.createPolyline(GP_CONVERTER.fromList(lineString.getPositions()), toGeoStyle(props)));
    }

    private static void parseGeoJsonMultiLineString(final MultiLineString multiLineString, final GeoJsonProperties props, final List<GeoPrimitive> list) {
        for (LineString ls : multiLineString.getLineStrings()) {
            parseGeoJsonLineString(ls, props, list);
        }
    }

    private static void parseGeoJsonPolygon(final Polygon polygon, final GeoJsonProperties props, final List<GeoPrimitive> list) {
        final GeoPrimitive.Builder b = GeoPrimitive.builder().setType(GeoItem.GeoType.POLYGON).setStyle(toGeoStyle(props));
        if (polygon.getRings() != null && !polygon.getRings().isEmpty()) {
            //first ring is polygon
            b.addPoints(GP_CONVERTER.fromList(polygon.getRings().get(0).getPositions()));
            //other rings are holes in this polygon
            for (int i = 1; i < polygon.getRings().size(); i++) {
                b.addHole(GP_CONVERTER.fromList(polygon.getRings().get(i).getPositions()));
            }
        }
        list.add(b.build());
    }

    private static void parseGeoJsonMultiPolygon(final MultiPolygon multiPolygon, final GeoJsonProperties props, final List<GeoPrimitive> list) {
        for (Polygon polygon : multiPolygon.getPolygons()) {
            parseGeoJsonPolygon(polygon, props, list);
        }
    }

    private static void parseGeoJsonGeometryCollection(final GeometryCollection geometryCollection, final GeoJsonProperties props, final List<GeoPrimitive> list) throws JSONException {
        for (Geometry g : geometryCollection.getGeometries()) {
            parseGeoJson(g, props, list);
        }
    }

    private static GeoStyle toGeoStyle(final GeoJsonProperties props) {
        return GeoStyle.builder()
                .setStrokeColor(props.strokeColor)
                .setStrokeWidth(props.strokeWidth)
                .setFillColor(props.fillColor)
                .build();
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
        if (json != null && json.has(key)) {
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
            if (json != null && json.has(key)) {
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
