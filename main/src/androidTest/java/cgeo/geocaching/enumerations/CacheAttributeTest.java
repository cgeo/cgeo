package cgeo.geocaching.enumerations;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CacheAttributeTest {

    @Test
    public void testTrimAttributeName() {
        for (final CacheAttribute attribute : CacheAttribute.values()) {
            final String rawName = attribute.rawName;
            assertThat(CacheAttribute.trimAttributeName(rawName)).as("attribute name").isEqualTo(rawName);
        }
    }

    @Test
    public void testIds() {
        for (final CacheAttribute attribute : CacheAttribute.values()) {
            if (attribute != CacheAttribute.UNKNOWN) {
                assertThat(StringUtils.isNotEmpty(attribute.rawName)).isTrue();
                assertThat(attribute.drawableId).isNotEqualTo(0);
                assertThat(attribute.stringIdYes).isNotEqualTo(0);
                assertThat(attribute.stringIdNo).isNotEqualTo(0);
            }
        }
    }

    @Test
    public void testGetL10n() {
        final CacheAttribute attribute = CacheAttribute.HIKING;
        // This test is language dependent. It does not make sense to test it
        // with every attribute. We just want to know if getL10n works
        // correctly
        assertThat(attribute.getL10n(true)).isNotEqualTo(attribute.getL10n(false));
    }

    @Test
    public void testGetBy() {
        final CacheAttribute attribute = CacheAttribute.HIKING; // an attribute that is present in GC and OC
        assertThat(attribute.gcid).overridingErrorMessage("Test cannot be run with this attribute").isGreaterThanOrEqualTo(0);
        assertThat(attribute.ocacode).overridingErrorMessage("Test cannot be run with this attribute").isGreaterThanOrEqualTo(0);
        assertThat(attribute).isSameAs(CacheAttribute.getByRawName(attribute.rawName));
        assertThat(attribute).isSameAs(CacheAttribute.getByOcACode(attribute.ocacode));
    }

    @Test
    public void testIsEnabled() {
        final String hikingYes = "hiking_yes";
        final String hikingNo = "hiking_no";
        assertThat(CacheAttribute.isEnabled(hikingYes)).isTrue();
        assertThat(CacheAttribute.isEnabled(hikingNo)).isFalse();
    }

}
