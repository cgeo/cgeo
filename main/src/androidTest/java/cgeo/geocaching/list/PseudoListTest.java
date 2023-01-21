package cgeo.geocaching.list;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class PseudoListTest {

    @Test
    public void testGetTitleAndCount() {
        final String title = PseudoList.ALL_LIST.getTitleAndCount();
        for (int i = 0; i < title.length(); i++) {
            assertThat(Character.isDigit(title.charAt(i))).overridingErrorMessage("pseudo lists shall not have a number shown in their title").isFalse();
        }
    }

    @Test
    public void testIsConcrete() {
        assertThat(PseudoList.ALL_LIST.isConcrete()).overridingErrorMessage("pseudo lists are not concrete lists").isFalse();
    }

}
