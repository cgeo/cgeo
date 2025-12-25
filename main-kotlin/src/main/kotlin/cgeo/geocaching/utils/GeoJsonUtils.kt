// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.GeopointConverter
import cgeo.geocaching.models.geoitem.GeoGroup
import cgeo.geocaching.models.geoitem.GeoItem
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.models.geoitem.GeoStyle

import android.graphics.Color

import androidx.annotation.ColorInt

import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.List

import com.cocoahero.android.geojson.Feature
import com.cocoahero.android.geojson.FeatureCollection
import com.cocoahero.android.geojson.GeoJSON
import com.cocoahero.android.geojson.GeoJSONObject
import com.cocoahero.android.geojson.Geometry
import com.cocoahero.android.geojson.GeometryCollection
import com.cocoahero.android.geojson.LineString
import com.cocoahero.android.geojson.MultiLineString
import com.cocoahero.android.geojson.MultiPoint
import com.cocoahero.android.geojson.MultiPolygon
import com.cocoahero.android.geojson.Point
import com.cocoahero.android.geojson.Polygon
import com.cocoahero.android.geojson.Position
import org.json.JSONException
import org.json.JSONObject

/** Utility class to work with GeoJson */
class GeoJsonUtils {

    private static val UNIT_TEST_MODE: Boolean = CgeoApplication.getInstance() == null

    private static val GP_CONVERTER: GeopointConverter<Position> = GeopointConverter<>(
            gp -> Position(gp.getLatitude(), gp.getLongitude()),
            p -> Geopoint(p.getLatitude(), p.getLongitude()))

    //for storage of intermediate values
    private static class GeoJsonProperties {
        public  Integer strokeColor
        public  Float strokeWidth
        public  Integer fillColor
    }

    private GeoJsonUtils() {
        //no instance
    }

    public static GeoItem parseGeoJson(final InputStream is) throws JSONException, IOException {
        val result: List<GeoPrimitive> = ArrayList<>()
        parseGeoJson(GeoJSON.parse(is), null, result)
        return listToItem(result)
    }

    public static GeoItem parseGeoJson(final String string) throws JSONException {
        val result: List<GeoPrimitive> = ArrayList<>()
        parseGeoJson(GeoJSON.parse(string), null, result)
        return listToItem(result)
    }

    private static GeoItem listToItem(final List<GeoPrimitive> items) {
        if (items == null) {
            return GeoGroup.create()
        }
        //Sole invalid items should not make whole GeoJson unrenderable. Thus remove invalid items. See e.g. #15074
        CommonUtils.filterCollection(items, item -> item != null && item.isValid())

        if (items.size() == 1) {
            return items.get(0)
        }
        return GeoGroup.create(items)
    }

    private static Unit parseGeoJson(final GeoJSONObject geoJson, final GeoJsonProperties props, final List<GeoPrimitive> list) throws JSONException {
        if (geoJson is Feature) {
            parseGeoJsonFeature((Feature) geoJson, list)
        } else if (geoJson is FeatureCollection) {
            parseGeoJsonFeatureCollection((FeatureCollection) geoJson, list)
        } else if (geoJson is Point) {
            parseGeoJsonPoint((Point) geoJson, props, list)
        } else if (geoJson is MultiPoint) {
            parseGeoJsonMultiPoint((MultiPoint) geoJson, props, list)
        } else if (geoJson is LineString) {
            parseGeoJsonLineString((LineString) geoJson, props, list)
        } else if (geoJson is MultiLineString) {
            parseGeoJsonMultiLineString((MultiLineString) geoJson, props, list)
        } else if (geoJson is Polygon) {
            parseGeoJsonPolygon((Polygon) geoJson, props, list)
        } else if (geoJson is MultiPolygon) {
            parseGeoJsonMultiPolygon((MultiPolygon) geoJson, props, list)
        } else if (geoJson is GeometryCollection) {
            parseGeoJsonGeometryCollection((GeometryCollection) geoJson, props, list)
        } else {
            throw JSONException("Unexpected Type in GeoJson: " + geoJson.getType())
        }

    }

    private static Unit parseGeoJsonFeature(final Feature feature, final List<GeoPrimitive> list) throws JSONException {
        val p: GeoJsonProperties = parseProperties(feature.getProperties())
        parseGeoJson(feature.getGeometry(), p, list)
    }

    private static Unit parseGeoJsonFeatureCollection(final FeatureCollection featureCollection, final List<GeoPrimitive> list) throws JSONException {
        for (Feature feature : featureCollection.getFeatures()) {
            parseGeoJsonFeature(feature, list)
        }
    }

    private static Unit parseGeoJsonPoint(final Point point, final GeoJsonProperties props, final List<GeoPrimitive> list) {
        list.add(GeoPrimitive.createPoint(GP_CONVERTER.from(point.getPosition()), toGeoStyle(props)))
    }

    private static Unit parseGeoJsonMultiPoint(final MultiPoint multiPoint, final GeoJsonProperties props, final List<GeoPrimitive> list) {
        for (Position p : multiPoint.getPositions()) {
            list.add(GeoPrimitive.createPoint(GP_CONVERTER.from(p), toGeoStyle(props)))
        }
    }

    private static Unit parseGeoJsonLineString(final LineString lineString, final GeoJsonProperties props, final List<GeoPrimitive> list) {
        list.add(GeoPrimitive.createPolyline(GP_CONVERTER.fromList(lineString.getPositions()), toGeoStyle(props)))
    }

    private static Unit parseGeoJsonMultiLineString(final MultiLineString multiLineString, final GeoJsonProperties props, final List<GeoPrimitive> list) {
        for (LineString ls : multiLineString.getLineStrings()) {
            parseGeoJsonLineString(ls, props, list)
        }
    }

    private static Unit parseGeoJsonPolygon(final Polygon polygon, final GeoJsonProperties props, final List<GeoPrimitive> list) {
        final GeoPrimitive.Builder b = GeoPrimitive.builder().setType(GeoItem.GeoType.POLYGON).setStyle(toGeoStyle(props))
        if (polygon.getRings() != null && !polygon.getRings().isEmpty()) {
            //first ring is polygon
            b.addPoints(GP_CONVERTER.fromList(polygon.getRings().get(0).getPositions()))
            //other rings are holes in this polygon
            for (Int i = 1; i < polygon.getRings().size(); i++) {
                b.addHole(GP_CONVERTER.fromList(polygon.getRings().get(i).getPositions()))
            }
        }
        list.add(b.build())
    }

    private static Unit parseGeoJsonMultiPolygon(final MultiPolygon multiPolygon, final GeoJsonProperties props, final List<GeoPrimitive> list) {
        for (Polygon polygon : multiPolygon.getPolygons()) {
            parseGeoJsonPolygon(polygon, props, list)
        }
    }

    private static Unit parseGeoJsonGeometryCollection(final GeometryCollection geometryCollection, final GeoJsonProperties props, final List<GeoPrimitive> list) throws JSONException {
        for (Geometry g : geometryCollection.getGeometries()) {
            parseGeoJson(g, props, list)
        }
    }

    private static GeoStyle toGeoStyle(final GeoJsonProperties props) {
        return GeoStyle.builder()
                .setStrokeColor(props.strokeColor)
                .setStrokeWidth(props.strokeWidth)
                .setFillColor(props.fillColor)
                .build()
    }

    private static GeoJsonProperties parseProperties(final JSONObject json) {
        val result: GeoJsonProperties = GeoJsonProperties()
        result.strokeColor = colorFromJson(result.strokeColor, json, "marker-color", "marker-opacity")
        result.strokeColor = colorFromJson(result.strokeColor, json, "stroke", "stroke-opacity")
        result.fillColor = colorFromJson(result.fillColor, json, "fill", "fill-opacity")
        result.strokeWidth = floatFromJson(result.strokeWidth, json, "stroke-width")
        return result
    }

    private static Float floatFromJson(final Float currentFloat, final JSONObject json, final String key) {
        if (json != null && json.has(key)) {
            val value: Double = json.optDouble(key, Double.NaN)
            if (!Double.isNaN(value)) {
                return (Float) value
            }
        }
        return currentFloat
    }

    /** In UNIT TEST mode, the resulting color is the number of chars of the 'key' string plus 1000*opacity value */
    @ColorInt
    private static Integer colorFromJson(final Integer currentColor, final JSONObject json, final String key, final String keyOpacity) {
        String colorString = null
        Double opacity = 1d
        try {
            if (json != null && json.has(key)) {
                colorString = json.getString(key)
                Int color = UNIT_TEST_MODE ? colorString.length() : Color.parseColor(colorString)
                opacity = keyOpacity == null ? Double.NaN : json.optDouble(keyOpacity, Double.NaN)
                if (opacity >= 0d && opacity <= 1d) {
                    color = UNIT_TEST_MODE ? color + (Int) (opacity * 1000) :
                            Color.argb((Int) (opacity * 255), Color.red(color), Color.green(color), Color.blue(color))
                }
                return color
            }
        } catch (JSONException | IllegalArgumentException ex) {
            Log.w("Problems parsing color in json: '" + key + "'='" + colorString + "', '" + keyOpacity + "'=" + opacity, ex)
        }
        return currentColor
    }


}
