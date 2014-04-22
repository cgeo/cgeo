package cgeo.geocaching.apps.cache.navi;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.apps.cache.navi.NavigationAppFactory.NavigationAppsEnum;

import java.util.HashSet;

import junit.framework.TestCase;

public class NavigationAppFactoryTest extends TestCase {

    public static void testUniqueNavigationAppIds() throws Exception {
        final HashSet<Integer> idSet = new HashSet<Integer>();
        for (NavigationAppsEnum navigationApp : NavigationAppsEnum.values()) {
            idSet.add(navigationApp.id);
        }
        assertThat(idSet).doesNotHaveDuplicates();
    }

}
