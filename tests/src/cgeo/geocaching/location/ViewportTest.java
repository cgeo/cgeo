package cgeo.geocaching.location;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.ICoordinates;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;

import org.eclipse.jdt.annotation.NonNull;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ViewportTest extends AndroidTestCase {

    final private static @NonNull
    Viewport vpRef = new Viewport(new Geopoint(-1.0, -2.0), new Geopoint(3.0, 4.0));

    public static void assertBounds(final Viewport vp) {
        assertEquals(new Geopoint(1.0, 1.0), vp.center);
        assertEquals(new Geopoint(3.0, 4.0), vp.topRight);
        assertEquals(new Geopoint(-1.0, -2.0), vp.bottomLeft);
    }

    public static void testCreationBounds() {
        assertBounds(new Viewport(new Geopoint(-1.0, -2.0), new Geopoint(3.0, 4.0)));
        assertBounds(new Viewport(new Geopoint(3.0, 4.0), new Geopoint(-1.0, -2.0)));
        assertBounds(new Viewport(new Geopoint(-1.0, 4.0), new Geopoint(3.0, -2.0)));
        assertBounds(new Viewport(new Geopoint(3.0, -2.0), new Geopoint(-1.0, 4.0)));
    }

    public static void testCreationCenter() {
        assertBounds(new Viewport(new Geopoint(1.0, 1.0), 4.0, 6.0));
    }

    public static void testCreationSeparate() {
        assertBounds(vpRef);
    }

    public static void testMinMax() {
        assertThat(vpRef.getLatitudeMin()).isEqualTo(-1.0);
        assertThat(vpRef.getLatitudeMax()).isEqualTo(3.0);
        assertThat(vpRef.getLongitudeMin()).isEqualTo(-2.0);
        assertThat(vpRef.getLongitudeMax()).isEqualTo(4.0);
    }

    public static void testSpans() {
        assertThat(vpRef.getLatitudeSpan()).isEqualTo(4.0);
        assertThat(vpRef.getLongitudeSpan()).isEqualTo(6.0);
    }

    public static void testInViewport() {
        assertThat(vpRef.contains(new Geopoint(-2.0, -2.0))).isFalse();
        assertThat(vpRef.contains(new Geopoint(4.0, 4.0))).isFalse();
        assertThat(vpRef.contains(Geopoint.ZERO)).isTrue();
        assertThat(vpRef.contains(new Geopoint(-1.0, -2.0))).isTrue();
        assertThat(vpRef.contains(new Geopoint(3.0, 4.0))).isTrue();
    }

    @SuppressLint("DefaultLocale")
    public static void testSqlWhere() {
        assertThat(vpRef.sqlWhere(null).toString()).isEqualTo("latitude >= -1.0 and latitude <= 3.0 and longitude >= -2.0 and longitude <= 4.0");
        assertThat(vpRef.sqlWhere("t").toString()).isEqualTo("t.latitude >= -1.0 and t.latitude <= 3.0 and t.longitude >= -2.0 and t.longitude <= 4.0");
        Locale current = null;
        try {
            current = Locale.getDefault();
            Locale.setDefault(Locale.FRENCH);
            assertThat(String.format("%.2g", 1.0d)).isEqualTo("1,0"); // Control that we are in a locale with comma separator
            assertThat(vpRef.sqlWhere("t").toString()).isEqualTo("t.latitude >= -1.0 and t.latitude <= 3.0 and t.longitude >= -2.0 and t.longitude <= 4.0");
        } finally {
            Locale.setDefault(current);
        }
    }

    public static void testEquals() {
        assertThat(vpRef).isEqualTo(vpRef);
        assertEquals(vpRef, new Viewport(vpRef.bottomLeft, vpRef.topRight));
        assertThat(vpRef.equals(new Viewport(new Geopoint(0.0, 0.0), 1.0, 1.0))).isFalse();
    }

    public static void testResize() {
        assertThat(vpRef.resize(1.0)).isEqualTo(vpRef);
        assertEquals(new Viewport(new Geopoint(-3.0, -5.0), new Geopoint(5.0, 7.0)), vpRef.resize(2.0));
        assertEquals(new Viewport(new Geopoint(0.0, -0.5), new Geopoint(2.0, 2.5)), vpRef.resize(0.5));
    }

    public static void testIncludes() {
        assertThat(vpRef.includes(vpRef)).isTrue();
        assertThat(vpRef.includes(vpRef.resize(0.5))).isTrue();
        assertThat(vpRef.includes(vpRef.resize(2.0))).isFalse();
    }

    public static void testContaining() {
        assertThat(Viewport.containing(Collections.singleton((ICoordinates) null))).isNull();
        final Set<Geopoint> points = new HashSet<Geopoint>();
        points.add(vpRef.bottomLeft);
        assertEquals(new Viewport(vpRef.bottomLeft, vpRef.bottomLeft), Viewport.containing(points));
        points.add(vpRef.topRight);
        assertThat(Viewport.containing(points)).isEqualTo(vpRef);
        points.add(vpRef.center);
        assertThat(Viewport.containing(points)).isEqualTo(vpRef);
    }

}
