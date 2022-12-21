package cgeo.geocaching.apps.navi;

import cgeo.geocaching.apps.navi.NavigationAppFactory.NavigationAppsEnum;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class NavigationAppFactoryTest extends TestCase {

    public static void testUniqueNavigationAppIds() throws Exception {
        final Set<Integer> idSet = new HashSet<>();
        for (final NavigationAppsEnum navigationApp : NavigationAppsEnum.values()) {
            idSet.add(navigationApp.id);
        }
        assertThat(idSet).doesNotHaveDuplicates();
    }

}
