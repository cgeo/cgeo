package cgeo.geocaching.utils;

import cgeo.geocaching.location.GeoObject;
import cgeo.geocaching.location.Geopoint;

import android.graphics.Color;

import java.util.List;

import org.json.JSONException;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeoJsonUtilsTest {

    @Test
    public void parseSimpleLinestring() throws JSONException {
        final List<GeoObject> list = GeoJsonUtils.parseGeoJson(
                "{\"type\": \"FeatureCollection\",\"features\": [{\"type\": \"Feature\"," +
                "\"properties\": {},\n" +
                "\"geometry\": {\"coordinates\": [[10.2,45.1],[11.9,46.8]],\"type\": \"LineString\"}" +
                "}]}");
        assertThat(list).hasSize(1);
        final GeoObject go = list.get(0);
        assertThat(go.getType()).isEqualTo(GeoObject.GeoType.POLYLINE);
        assertThat(go.getPoints()).hasSize(2);
        assertThat(go.getPoints()[0]).isEqualTo(new Geopoint(45.1, 10.2));
        assertThat(go.getPoints()[1]).isEqualTo(new Geopoint(46.8, 11.9));
        assertThat(go.getStrokeColor()).isEqualTo(Color.BLACK);
        assertThat(go.getFillColor()).isEqualTo(Color.TRANSPARENT);
    }

    @Test
    public void parseColor() throws JSONException {
        final List<GeoObject> list = GeoJsonUtils.parseGeoJson(
                "{\"type\": \"Feature\"," +
                        "\"properties\": {\"stroke\": \"#123456\", \"stroke-opacity\": 0.5 },\n" +
                        "\"geometry\": {\"coordinates\": [10.2,45.1],\"type\": \"Point\"}" +
                        "}");
        assertThat(list).hasSize(1);
        final GeoObject go = list.get(0);
        assertThat(go.getType()).isEqualTo(GeoObject.GeoType.POINT);
        assertThat(go.getPoints()).hasSize(1);
        assertThat(go.getPoints()[0]).isEqualTo(new Geopoint(45.1, 10.2));
        assertThat(go.getStrokeColor()).isEqualTo(7 + (int) (1000 * 0.5f));
        assertThat(go.getFillColor()).isEqualTo(Color.TRANSPARENT);
    }

    @Test(expected = JSONException.class)
    public void parseError() throws JSONException {
        GeoJsonUtils.parseGeoJson("");

    }
}
