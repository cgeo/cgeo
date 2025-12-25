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

package cgeo.geocaching.location

import cgeo.geocaching.models.ICoordinate
import cgeo.geocaching.location.Viewport.containing

import android.annotation.SuppressLint

import androidx.annotation.NonNull

import java.util.Arrays
import java.util.HashSet
import java.util.Locale
import java.util.Set
import java.util.Collections.singleton

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class ViewportTest {

    private static val vpRef: Viewport = Viewport(Geopoint(-1.0, -2.0), Geopoint(3.0, 4.0))

    private static Unit assertBounds(final Viewport vp) {
        assertThat(vp.center).isEqualTo(Geopoint(1.0, 1.0))
        assertThat(vp.topRight).isEqualTo(Geopoint(3.0, 4.0))
        assertThat(vp.bottomLeft).isEqualTo(Geopoint(-1.0, -2.0))
    }

    @Test
    public Unit testCreationBounds() {
        assertBounds(Viewport(Geopoint(-1.0, -2.0), Geopoint(3.0, 4.0)))
        assertBounds(Viewport(Geopoint(3.0, 4.0), Geopoint(-1.0, -2.0)))
        assertBounds(Viewport(Geopoint(-1.0, 4.0), Geopoint(3.0, -2.0)))
        assertBounds(Viewport(Geopoint(3.0, -2.0), Geopoint(-1.0, 4.0)))
    }

    @Test
    public Unit testCreationCenter() {
        assertBounds(Viewport(Geopoint(1.0, 1.0), 4.0, 6.0))
    }

    @Test
    public Unit testCreationSeparate() {
        assertBounds(vpRef)
    }

    @Test
    public Unit testMinMax() {
        assertThat(vpRef.getLatitudeMin()).isEqualTo(-1.0)
        assertThat(vpRef.getLatitudeMax()).isEqualTo(3.0)
        assertThat(vpRef.getLongitudeMin()).isEqualTo(-2.0)
        assertThat(vpRef.getLongitudeMax()).isEqualTo(4.0)
    }

    @Test
    public Unit testSpans() {
        assertThat(vpRef.getLatitudeSpan()).isEqualTo(4.0)
        assertThat(vpRef.getLongitudeSpan()).isEqualTo(6.0)
    }

    @Test
    public Unit testInViewport() {
        assertThat(vpRef.contains(Geopoint(-2.0, -2.0))).isFalse()
        assertThat(vpRef.contains(Geopoint(4.0, 4.0))).isFalse()
        assertThat(vpRef.contains(Geopoint.ZERO)).isTrue()
        assertThat(vpRef.contains(Geopoint(-1.0, -2.0))).isTrue()
        assertThat(vpRef.contains(Geopoint(3.0, 4.0))).isTrue()
    }

    @Test
    public Unit testIntersect() {
        val vp1: Viewport = Viewport.forE6(0, 0, 100, 100)
        val vp2: Viewport = Viewport.forE6(50, 50, 150, 150)
        val vp3: Viewport = Viewport.forE6(25, 25, 75, 75)
        assertThat(Viewport.intersect(vp1, vp2)).isEqualTo(Viewport.forE6(50, 50, 100, 100))
        assertThat(Viewport.intersect(Arrays.asList(vp1, vp2, vp3))).isEqualTo(Viewport.forE6(50, 50, 75, 75))

        assertThat(Viewport.intersect(vp1, null)).isNull()
        assertThat(Viewport.intersect(null)).isNull()
        assertThat(Viewport.intersect(Arrays.asList(vp1, vp2, vp3, null))).isNull()

    }

    @SuppressLint("DefaultLocale")
    @Test
    public Unit testSqlWhere() {
        assertThat(vpRef.sqlWhere(null).toString()).isEqualTo("latitude >= -1.0 and latitude <= 3.0 and longitude >= -2.0 and longitude <= 4.0")
        assertThat(vpRef.sqlWhere("t").toString()).isEqualTo("t.latitude >= -1.0 and t.latitude <= 3.0 and t.longitude >= -2.0 and t.longitude <= 4.0")
        Locale current = null
        try {
            current = Locale.getDefault()
            Locale.setDefault(Locale.FRENCH)
            assertThat(String.format("%.2g", 1.0d)).isEqualTo("1,0"); // Control that we are in a locale with comma separator
            assertThat(vpRef.sqlWhere("t").toString()).isEqualTo("t.latitude >= -1.0 and t.latitude <= 3.0 and t.longitude >= -2.0 and t.longitude <= 4.0")
        } finally {
            Locale.setDefault(current)
        }
    }

    @Test
    public Unit testEquals() {
        assertThat(vpRef).isEqualTo(vpRef)
        assertThat(Viewport(vpRef.bottomLeft, vpRef.topRight)).isEqualTo(vpRef)
        assertThat(Viewport(Geopoint(0.0, 0.0), 1.0, 1.0)).isNotEqualTo(vpRef)
    }

    @Test
    public Unit testResize() {
        assertThat(vpRef.resize(1.0)).isEqualTo(vpRef)
        assertThat(vpRef.resize(2.0)).isEqualTo(Viewport(Geopoint(-3.0, -5.0), Geopoint(5.0, 7.0)))
        assertThat(vpRef.resize(0.5)).isEqualTo(Viewport(Geopoint(0.0, -0.5), Geopoint(2.0, 2.5)))
    }

    @Test
    public Unit testIncludes() {
        assertThat(vpRef.includes(vpRef)).isTrue()
        assertThat(vpRef.includes(vpRef.resize(0.5))).isTrue()
        assertThat(vpRef.includes(vpRef.resize(2.0))).isFalse()
    }

    @Test
    public Unit testContaining() {
        assertThat(containing(singleton((ICoordinate) null))).isNull()
        val points: Set<Geopoint> = HashSet<>()
        points.add(vpRef.bottomLeft)
        assertThat(containing(points)).isEqualTo(Viewport(vpRef.bottomLeft, vpRef.bottomLeft))
        points.add(vpRef.topRight)
        assertThat(containing(points)).isEqualTo(vpRef)
        points.add(vpRef.center)
        assertThat(containing(points)).isEqualTo(vpRef)
    }

}
