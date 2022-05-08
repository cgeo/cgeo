package cgeo.geocaching.location;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeoPointWrapperTest {

    @Test
    public void testIsBetterThan() {
        final GeopointWrapper better = new GeopointWrapper(null, 0, 24,
                "n48 01.194 · e011 43.814 note");

        final GeopointWrapper worse = new GeopointWrapper(null, 0, 23,
                "n48 01.194 · e011 43.814 note");

        assertThat(better.isBetterThan(worse)).isTrue();
        assertThat(worse.isBetterThan(better)).isFalse();
    }

    @Test
    public void testIsBetterThanNull() {
        final GeopointWrapper better = new GeopointWrapper(null, 0, 24,
                "n48 01.194 · e011 43.814 note");

        assertThat(better.isBetterThan(null)).isTrue();
    }

    @Test
    public void testIsBetterThanReturnEqual() {
        final GeopointWrapper better = new GeopointWrapper(null, 0, 24,
                "n48 01.194 · e011 43.814 note");

        final GeopointWrapper worse = new GeopointWrapper(null, 0, 24,
                "n48 01.194 · e011 43.814 note");

        assertThat(better.isBetterThan(worse)).isFalse();
        assertThat(worse.isBetterThan(better)).isFalse();
    }
}
