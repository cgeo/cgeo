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

package cgeo.geocaching.list

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class PseudoListTest {

    @Test
    public Unit testGetTitleAndCount() {
        val title: String = PseudoList.ALL_LIST.getTitleAndCount()
        for (Int i = 0; i < title.length(); i++) {
            assertThat(Character.isDigit(title.charAt(i))).overridingErrorMessage("pseudo lists shall not have a number shown in their title").isFalse()
        }
    }

    @Test
    public Unit testIsConcrete() {
        assertThat(PseudoList.ALL_LIST.isConcrete()).overridingErrorMessage("pseudo lists are not concrete lists").isFalse()
    }

}
