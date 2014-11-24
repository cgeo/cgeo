package cgeo.geocaching.enumerations;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.StringUtils;

import android.test.AndroidTestCase;

public class CacheAttributeTest extends AndroidTestCase {

    public static void testTrimAttributeName() {
        for (CacheAttribute attribute : CacheAttribute.values()) {
            final String rawName = attribute.rawName;
            assertThat(CacheAttribute.trimAttributeName(rawName)).as("attribute name").isEqualTo(rawName);
        }
    }

    public static void testIds() {
        for (CacheAttribute attribute : CacheAttribute.values()) {
            if (attribute != CacheAttribute.UNKNOWN) {
                assertThat(StringUtils.isNotEmpty(attribute.rawName)).isTrue();
                assertThat(attribute.drawableId != 0).isTrue();
                assertThat(attribute.stringIdYes != 0).isTrue();
                assertThat(attribute.stringIdNo != 0).isTrue();
            }
        }
    }

    public static void testGetL10n() {
        final CacheAttribute attribute = CacheAttribute.HIKING;
        // This test is language dependent. It does not make sense to test it
        // with every attribute. We just want to know if getL10n works
        // correctly
        assertFalse("_yes and _no must not have the same translation",
                attribute.getL10n(true).equals(attribute.getL10n(false)));
    }

    public static void testGetBy() {
        final CacheAttribute attribute = CacheAttribute.HIKING; // an attribute that is present in GC and OC
        assertThat(attribute.gcid).overridingErrorMessage("Test cannot be run with this attribute").isGreaterThanOrEqualTo(0);
        assertThat(attribute.ocacode).overridingErrorMessage("Test cannot be run with this attribute").isGreaterThanOrEqualTo(0);
        assertThat(attribute).isSameAs(CacheAttribute.getByRawName(attribute.rawName));
        assertThat(attribute).isSameAs(CacheAttribute.getByOcACode(attribute.ocacode));
    }

    public static void testIsEnabled() {
        final String hiking_yes = "hiking_yes";
        final String hiking_no = "hiking_no";
        assertThat(CacheAttribute.isEnabled(hiking_yes)).isTrue();
        assertThat(CacheAttribute.isEnabled(hiking_no)).isFalse();
    }

}
