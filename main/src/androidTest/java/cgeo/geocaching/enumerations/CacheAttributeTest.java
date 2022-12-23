package cgeo.geocaching.enumerations;

import android.test.AndroidTestCase;

import org.apache.commons.lang3.StringUtils;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CacheAttributeTest extends AndroidTestCase {

    public static void testTrimAttributeName() {
        for (final CacheAttribute attribute : CacheAttribute.values()) {
            final String rawName = attribute.rawName;
            assertThat(CacheAttribute.trimAttributeName(rawName)).as("attribute name").isEqualTo(rawName);
        }
    }

    public static void testIds() {
        for (final CacheAttribute attribute : CacheAttribute.values()) {
            if (attribute != CacheAttribute.UNKNOWN) {
                assertThat(StringUtils.isNotEmpty(attribute.rawName)).isTrue();
                assertThat(attribute.drawableId).isNotEqualTo(0);
                assertThat(attribute.stringIdYes).isNotEqualTo(0);
                assertThat(attribute.stringIdNo).isNotEqualTo(0);
            }
        }
    }

    public static void testGetL10n() {
        final CacheAttribute attribute = CacheAttribute.HIKING;
        // This test is language dependent. It does not make sense to test it
        // with every attribute. We just want to know if getL10n works
        // correctly
        assertThat(attribute.getL10n(true)).isNotEqualTo(attribute.getL10n(false));
    }

    public static void testGetBy() {
        final CacheAttribute attribute = CacheAttribute.HIKING; // an attribute that is present in GC and OC
        assertThat(attribute.gcid).overridingErrorMessage("Test cannot be run with this attribute").isGreaterThanOrEqualTo(0);
        assertThat(attribute.ocacode).overridingErrorMessage("Test cannot be run with this attribute").isGreaterThanOrEqualTo(0);
        assertThat(attribute).isSameAs(CacheAttribute.getByRawName(attribute.rawName));
        assertThat(attribute).isSameAs(CacheAttribute.getByOcACode(attribute.ocacode));
    }

    public static void testIsEnabled() {
        final String hikingYes = "hiking_yes";
        final String hikingNo = "hiking_no";
        assertThat(CacheAttribute.isEnabled(hikingYes)).isTrue();
        assertThat(CacheAttribute.isEnabled(hikingNo)).isFalse();
    }

}
