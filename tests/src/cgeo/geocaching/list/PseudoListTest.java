package cgeo.geocaching.list;

import org.apache.commons.lang3.StringUtils;

import junit.framework.TestCase;

public class PseudoListTest extends TestCase {

    public static void testGetTitleAndCount() throws Exception {
        final String title = PseudoList.ALL_LIST.title;
        assertTrue("pseudo lists shall not have a number shown in their title", StringUtils.isAlpha(title.substring(1, title.length() - 1)));
    }

    public static void testIsConcrete() throws Exception {
        assertFalse("pseudo lists are not concrete lists", PseudoList.ALL_LIST.isConcrete());
    }

}
