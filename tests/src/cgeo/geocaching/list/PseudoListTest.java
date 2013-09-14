package cgeo.geocaching.list;

import junit.framework.TestCase;

public class PseudoListTest extends TestCase {

    public static void testGetTitleAndCount() throws Exception {
        final String title = PseudoList.ALL_LIST.title;
        for (int i = 0; i < title.length(); i++) {
            assertFalse("pseudo lists shall not have a number shown in their title", Character.isDigit(title.charAt(i)));
        }
    }

    public static void testIsConcrete() throws Exception {
        assertFalse("pseudo lists are not concrete lists", PseudoList.ALL_LIST.isConcrete());
    }

}
