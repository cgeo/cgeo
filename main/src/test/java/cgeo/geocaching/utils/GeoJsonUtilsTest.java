package cgeo.geocaching.utils;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;

import org.json.JSONException;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeoJsonUtilsTest {

    @Test
    public void parseSimpleLinestring() throws JSONException {
        final GeoItem item = GeoJsonUtils.parseGeoJson(
                "{\"type\": \"FeatureCollection\",\"features\": [{\"type\": \"Feature\"," +
                "\"properties\": {},\n" +
                "\"geometry\": {\"coordinates\": [[10.2,45.1],[11.9,46.8]],\"type\": \"LineString\"}" +
                "}]}");
        final GeoPrimitive go = assertOneGeoPrimitive(item);
        assertThat(go.getType()).isEqualTo(GeoItem.GeoType.POLYLINE);
        assertThat(go.getPoints()).hasSize(2);
        assertThat(go.getPoints().get(0)).isEqualTo(new Geopoint(45.1, 10.2));
        assertThat(go.getPoints().get(1)).isEqualTo(new Geopoint(46.8, 11.9));
        assertThat(go.getStyle().getStrokeColor()).isNull();
        assertThat(GeoStyle.getStrokeColor(go.getStyle())).isEqualTo(GeoStyle.SYSTEM_DEFAULT.getStrokeColor());
        assertThat(go.getStyle().getFillColor()).isNull();
        assertThat(GeoStyle.getFillColor(go.getStyle())).isEqualTo(GeoStyle.SYSTEM_DEFAULT.getFillColor());
    }

    @Test
    public void parseColor() throws JSONException {
        final GeoItem list = GeoJsonUtils.parseGeoJson(
                "{\"type\": \"Feature\"," +
                        "\"properties\": {\"stroke\": \"#123456\", \"stroke-opacity\": 0.5 },\n" +
                        "\"geometry\": {\"coordinates\": [10.2,45.1],\"type\": \"Point\"}" +
                        "}");
        final GeoPrimitive go = assertOneGeoPrimitive(list);
        assertThat(go.getType()).isEqualTo(GeoItem.GeoType.CIRCLE);
        assertThat(go.getPoints()).hasSize(1);
        assertThat(go.getPoints().get(0)).isEqualTo(new Geopoint(45.1, 10.2));
        assertThat(go.getStyle().getStrokeColor()).isEqualTo(7 + (int) (1000 * 0.5f));
        assertThat(go.getStyle().getFillColor()).isNull();
    }

    @Test(expected = JSONException.class)
    public void parseError() throws JSONException {
        GeoJsonUtils.parseGeoJson("");
    }

    private static GeoPrimitive assertOneGeoPrimitive(final GeoItem item) {
        assertThat(item).isOfAnyClassIn(GeoGroup.class, GeoPrimitive.class);
        if (item instanceof GeoPrimitive) {
            return (GeoPrimitive) item;
        }
        final GeoGroup group = (GeoGroup) item;
        assertThat(group.getItems()).hasSize(1);
        assertThat(group.getItems().get(0)).isOfAnyClassIn(GeoPrimitive.class);
        return (GeoPrimitive) group.getItems().get(0);
    }
}
