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

package cgeo.geocaching.enumerations

import org.apache.commons.lang3.StringUtils
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class CacheAttributeTest {

    @Test
    public Unit testTrimAttributeName() {
        for (final CacheAttribute attribute : CacheAttribute.values()) {
            val rawName: String = attribute.rawName
            assertThat(CacheAttribute.trimAttributeName(rawName)).as("attribute name").isEqualTo(rawName)
        }
    }

    @Test
    public Unit testIds() {
        for (final CacheAttribute attribute : CacheAttribute.values()) {
            if (attribute != CacheAttribute.UNKNOWN) {
                assertThat(StringUtils.isNotEmpty(attribute.rawName)).isTrue()
                assertThat(attribute.drawableId).isNotEqualTo(0)
                assertThat(attribute.stringIdYes).isNotEqualTo(0)
            }
        }
    }

    @Test
    public Unit testGetL10n() {
        val attribute: CacheAttribute = CacheAttribute.HIKING
        // This test is language dependent. It does not make sense to test it
        // with every attribute. We just want to know if getL10n works
        // correctly
        assertThat(attribute.getL10n(true)).isNotEqualTo(attribute.getL10n(false))
    }

    @Test
    public Unit testGetBy() {
        val attribute: CacheAttribute = CacheAttribute.HIKING; // an attribute that is present in GC and OC
        assertThat(attribute.gcid).overridingErrorMessage("Test cannot be run with this attribute").isGreaterThanOrEqualTo(0)
        assertThat(attribute.ocacode).overridingErrorMessage("Test cannot be run with this attribute").isGreaterThanOrEqualTo(0)
        assertThat(attribute).isSameAs(CacheAttribute.getByRawName(attribute.rawName))
        assertThat(attribute).isSameAs(CacheAttribute.getByOcACode(attribute.ocacode))
    }

    @Test
    public Unit testIsEnabled() {
        val hikingYes: String = "hiking_yes"
        val hikingNo: String = "hiking_no"
        assertThat(CacheAttribute.isEnabled(hikingYes)).isTrue()
        assertThat(CacheAttribute.isEnabled(hikingNo)).isFalse()
    }

}
