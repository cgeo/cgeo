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

package cgeo.geocaching.apps.navi

import cgeo.geocaching.apps.navi.NavigationAppFactory.NavigationAppsEnum

import java.util.HashSet
import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class NavigationAppFactoryTest {

    @Test
    public  Unit uniqueNavigationAppIds() {
        val idSet: Set<Integer> = HashSet<>()
        for (final NavigationAppsEnum navigationApp : NavigationAppsEnum.values()) {
            idSet.add(navigationApp.id)
        }
        assertThat(idSet).doesNotHaveDuplicates()
    }

}
