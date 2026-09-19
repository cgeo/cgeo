package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.models.geoitem.GeoStyleRule;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Utility class to work with GeoJson */
public class GeoJsonUtils {

    public static final String RULE_TYPE_STROKE_COLOR = "stroke-color";
    public static final String RULE_TYPE_FILL_COLOR = "fill-color";
    public static final String RULE_TYPE_STROKE_WIDTH = "stroke-width";


    private static final boolean UNIT_TEST_MODE = CgeoApplication.getInstance() == null;

    private static final GeopointConverter<Position> GP_CONVERTER = new GeopointConverter<>(
            gp -> new Position(gp.getLatitude(), gp.getLongitude()),
            p -> new Geopoint(p.getLatitude(), p.getLongitude()));

    private GeoJsonUtils() {
        //no instance
    }

    public static GeoItem parseGeoJson(final InputStream is) throws JSONException, IOException {
        final List<GeoItem> result = new ArrayList<>();
        parseGeoJson(GeoJSON.parse(is), result);
        return listToItem(result);
    }

    public static GeoItem parseGeoJson(final String string) throws JSONException {
        final List<GeoItem> result = new ArrayList<>();
        parseGeoJson(GeoJSON.parse(string), result);
        return listToItem(result);
    }

    private static GeoItem listToItem(final List<GeoItem> items) {
        if (items == null) {
            return GeoGroup.create();
        }
        //Sole invalid items should not make whole GeoJson unrenderable. Thus remove invalid items. See e.g. #15074
        CommonUtils.filterCollection(items, item -> item != null && item.isValid());

        if (items.size() == 1) {
            return items.get(0);
        }
        return GeoGroup.builder().addItems(items).setStyle(null).build();
    }

    private static void parseGeoJson(final GeoJSONObject geoJson, final List<GeoItem> list) throws JSONException {
        if (geoJson instanceof Feature) {
            parseGeoJsonFeature((Feature) geoJson, list);
        } else if (geoJson instanceof FeatureCollection) {
            parseGeoJsonFeatureCollection((FeatureCollection) geoJson, list);
        } else if (geoJson instanceof Point) {
            parseGeoJsonPoint((Point) geoJson, list);
        } else if (geoJson instanceof MultiPoint) {
            parseGeoJsonMultiPoint((MultiPoint) geoJson, list);
        } else if (geoJson instanceof LineString) {
            parseGeoJsonLineString((LineString) geoJson, list);
        } else if (geoJson instanceof MultiLineString) {
            parseGeoJsonMultiLineString((MultiLineString) geoJson, list);
        } else if (geoJson instanceof Polygon) {
            parseGeoJsonPolygon((Polygon) geoJson, list);
        } else if (geoJson instanceof MultiPolygon) {
            parseGeoJsonMultiPolygon((MultiPolygon) geoJson, list);
        } else if (geoJson instanceof GeometryCollection) {
            parseGeoJsonGeometryCollection((GeometryCollection) geoJson, list);
        } else {
            throw new JSONException("Unexpected Type in GeoJson: " + geoJson.getType());
        }

    }

    private static void parseGeoJsonFeature(final Feature feature, final List<GeoItem> list) throws JSONException {
        final GeoStyle style = parseGeoStyle(feature.getProperties());
        final List<GeoItem> sublist = new ArrayList<>();
        parseGeoJson(feature.getGeometry(), sublist);
        CommonUtils.filterCollection(sublist, item -> item != null && item.isValid());
        if (sublist.size() == 1 && sublist.get(0) instanceof GeoPrimitive item) {
            list.add(item.buildUpon().setStyle(style).build());
        } else {
            list.add(GeoGroup.builder().addItems(sublist).setStyle(style).build());
        }
    }

    private static void parseGeoJsonFeatureCollection(final FeatureCollection featureCollection, final List<GeoItem> list) throws JSONException {
        for (Feature feature : featureCollection.getFeatures()) {
            parseGeoJsonFeature(feature, list);
        }
    }

    private static void parseGeoJsonPoint(final Point point, final List<GeoItem> list) {
        list.add(GeoPrimitive.createPoint(GP_CONVERTER.from(point.getPosition()), emptyStyle()));
    }

    private static void parseGeoJsonMultiPoint(final MultiPoint multiPoint, final List<GeoItem> list) {
        for (Position p : multiPoint.getPositions()) {
            list.add(GeoPrimitive.createPoint(GP_CONVERTER.from(p), emptyStyle()));
        }
    }

    private static void parseGeoJsonLineString(final LineString lineString, final List<GeoItem> list) {
        list.add(GeoPrimitive.createPolyline(GP_CONVERTER.fromList(lineString.getPositions()), emptyStyle()));
    }

    private static void parseGeoJsonMultiLineString(final MultiLineString multiLineString, final List<GeoItem> list) {
        for (LineString ls : multiLineString.getLineStrings()) {
            parseGeoJsonLineString(ls, list);
        }
    }

    private static void parseGeoJsonPolygon(final Polygon polygon, final List<GeoItem> list) {
        final GeoPrimitive.Builder b = GeoPrimitive.builder().setType(GeoItem.GeoType.POLYGON).setStyle(emptyStyle());
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

    private static void parseGeoJsonMultiPolygon(final MultiPolygon multiPolygon, final List<GeoItem> list) {
        for (Polygon polygon : multiPolygon.getPolygons()) {
            parseGeoJsonPolygon(polygon, list);
        }
    }

    private static void parseGeoJsonGeometryCollection(final GeometryCollection geometryCollection, final List<GeoItem> list) throws JSONException {
        for (Geometry g : geometryCollection.getGeometries()) {
            parseGeoJson(g, list);
        }
    }

    private static GeoStyle emptyStyle() {
        return GeoStyle.dynamic(null);
    }

    @NonNull
    private static GeoStyle parseGeoStyle(@Nullable final JSONObject json) {
        if (json == null) {
            return emptyStyle();
        }
        final List<GeoStyleRule> rules = new ArrayList<>();
        try {
            final JSONArray ruleArray = json.optJSONArray("styleRules");
            if (ruleArray != null) {
                for (int i = 0; i < ruleArray.length(); i++) {
                    final JSONObject obj = ruleArray.getJSONObject(i);
                    if (obj == null) {
                        continue;
                    }
                    final String type = obj.optString("type", "");
                    switch (type) {
                        case RULE_TYPE_FILL_COLOR -> {
                            final Integer color = colorFromJson(obj, "value", null);
                            if (color != null) {
                                rules.add(GeoStyleRule.ofFillColor(color, obj.optString("condition", null)));
                            }
                        }
                        case RULE_TYPE_STROKE_COLOR -> {
                            final Integer color = colorFromJson(obj, "value", null);
                            if (color != null) {
                                rules.add(GeoStyleRule.ofStrokeColor(color, obj.optString("condition", null)));
                            }
                        }
                        case RULE_TYPE_STROKE_WIDTH -> {
                            final Float width = floatFromJson(obj, "value");
                            if (width != null) {
                                rules.add(GeoStyleRule.ofStrokeWidth(width, obj.optString("condition", null)));
                            }
                        }
                        default -> {
                            //unknown type, ignore
                        }
                    }
                }
            }
        } catch (JSONException ex) {
            //no rules array, ignore
        }
        final Integer markerColor = colorFromJson(json, "marker-color", "marker-opacity");
        if (markerColor != null) {
            rules.add(GeoStyleRule.ofStrokeColor(markerColor, null));
        }
        final Integer strokeColor = colorFromJson(json, "stroke", "stroke-opacity");
        if (strokeColor != null) {
            rules.add(GeoStyleRule.ofStrokeColor(strokeColor, null));
        }
        final Integer fillColor = colorFromJson(json, "fill", "fill-opacity");
        if (fillColor != null) {
            rules.add(GeoStyleRule.ofFillColor(fillColor, null));
        }
        final Float strokeWidth = floatFromJson(json, "stroke-width");
        if (strokeWidth != null) {
            rules.add(GeoStyleRule.ofStrokeWidth(strokeWidth, null));
        }
        return GeoStyle.dynamic(rules);
    }

    private static Float floatFromJson(final JSONObject json, final String key) {
        if (json != null && json.has(key)) {
            final double value = json.optDouble(key, Double.NaN);
            if (!Double.isNaN(value)) {
                return (float) value;
            }
        }
        return null;
    }

    private static Integer colorFromJson(final JSONObject json, final String key, final String keyOpacity) {
        try {
            if (json != null && json.has(key)) {
                final String colorString = json.getString(key);
                int color = UNIT_TEST_MODE ? colorString.length() : ColorUtils.parseColor(colorString, 17, true);
                if (color == 17) {
                    return null;
                }
                if (color == ColorUtils.COLOR_MAIN_VALUE) {
                    return color;
                }
                final double opacity = keyOpacity == null ? Double.NaN : json.optDouble(keyOpacity, Double.NaN);
                if (opacity >= 0d && opacity <= 1d) {
                    color = UNIT_TEST_MODE ? color + (int) (opacity * 1000) :
                            Color.argb((int) (opacity * 255), Color.red(color), Color.green(color), Color.blue(color));
                }
                return color;
            }
        } catch (JSONException ex) {
            Log.w("Problems parsing color in json: '" + key + "'='" + json.optString(key) + "', '" + keyOpacity + "'=" + json.optDouble(keyOpacity, Double.NaN), ex);
        }
        return null;
    }


}
