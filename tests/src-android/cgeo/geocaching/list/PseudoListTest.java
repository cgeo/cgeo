package cgeo.geocaching.list;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class PseudoListTest extends TestCase {

    public static void testGetTitleAndCount() throws Exception {
        final String title = PseudoList.ALL_LIST.getTitleAndCount();
        for (int i = 0; i < title.length(); i++) {
            assertThat(Character.isDigit(title.charAt(i))).overridingErrorMessage("pseudo lists shall not have a number shown in their title").isFalse();
        }
    }

    public static void testIsConcrete() throws Exception {
        assertThat(PseudoList.ALL_LIST.isConcrete()).overridingErrorMessage("pseudo lists are not concrete lists").isFalse();
    }

}
