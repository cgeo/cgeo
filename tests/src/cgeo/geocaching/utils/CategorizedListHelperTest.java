package cgeo.geocaching.utils;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CategorizedListHelperTest {

    private static CategorizedListHelper getExampleHelper() {
        //example list with categories "Color", "Animal" and "Thing":
        //              0c       1       2      3c        4      5      6        7c        8
        //Arrays.asList("Color", "blue", "red", "Animal", "cat", "dog", "snake", "Thing", "car");
        //construct a helper fitting the categorized list above
        final CategorizedListHelper clh = new CategorizedListHelper();
        clh.addOrMoveCategory("Animal", false, 3);
        clh.addOrMoveCategory("Color", true, 2);
        clh.addOrMoveCategory("Thing", false, 1);
        return clh;

    }

    @Test
    public void checkExampleHelperConsistency() {

        final CategorizedListHelper clh = getExampleHelper();
        //test
        assertThat(clh.getCategoryTitlePosition(null)).isEqualTo(-1);
        assertThat(clh.getCategoryTitlePosition("Animal")).isEqualTo(3);
        assertThat(clh.getCategoryInsertPosition("Animal")).isEqualTo(7);
        assertThat(clh.getCategoryTitlePosition("Thing")).isEqualTo(7);
        assertThat(clh.getCategoryInsertPosition("Thing")).isEqualTo(9);

        assertThat(clh.getContentSize()).isEqualTo(6);
        assertThat(clh.getListIndexForContentIndex(0)).isEqualTo(1);
        assertThat(clh.getListIndexForContentIndex(6)).isEqualTo(-1);
        assertThat(clh.getListIndexForContentIndex(5)).isEqualTo(8);
        assertThat(clh.getListIndexForContentIndex(4)).isEqualTo(6);
    }

    @Test
    public void addCount() {
        final CategorizedListHelper clh = getExampleHelper();
        clh.addToCount("Thing", 1);

        //test
        assertThat(clh.getCategoryTitlePosition(null)).isEqualTo(-1);
        assertThat(clh.getCategoryTitlePosition("Animal")).isEqualTo(3);
        assertThat(clh.getCategoryInsertPosition("Animal")).isEqualTo(7);
        assertThat(clh.getCategoryTitlePosition("Thing")).isEqualTo(7);
        assertThat(clh.getCategoryInsertPosition("Thing")).isEqualTo(10);

        assertThat(clh.getContentSize()).isEqualTo(7);
        assertThat(clh.getListIndexForContentIndex(0)).isEqualTo(1);
        assertThat(clh.getListIndexForContentIndex(6)).isEqualTo(9);
        assertThat(clh.getListIndexForContentIndex(5)).isEqualTo(8);
        assertThat(clh.getListIndexForContentIndex(4)).isEqualTo(6);
    }

    @Test
    public void moveCategory() {
        final CategorizedListHelper clh = getExampleHelper();
        clh.addOrMoveCategory("Thing", true);

        //result should be:
        //              0c       1      2c       3       4      5c        6      7       8
        //Arrays.asList("Thing", "car", "Color", "blue", "red", "Animal", "cat", "dog", "snake");

        assertThat(clh.getCategoryTitlePosition("Animal")).isEqualTo(5);
        assertThat(clh.getCategoryInsertPosition("Animal")).isEqualTo(9);
        assertThat(clh.getCategoryTitlePosition("Thing")).isEqualTo(0);
        assertThat(clh.getCategoryInsertPosition("Thing")).isEqualTo(2);

        assertThat(clh.getContentSize()).isEqualTo(6);
        assertThat(clh.getListIndexForContentIndex(0)).isEqualTo(1);
        assertThat(clh.getListIndexForContentIndex(5)).isEqualTo(8);
        assertThat(clh.getListIndexForContentIndex(2)).isEqualTo(4);
        assertThat(clh.getListIndexForContentIndex(3)).isEqualTo(6);

    }

    @Test
    public void setCategoryZero() {
        final CategorizedListHelper clh = getExampleHelper();
        clh.setCount("Animal", 0);

        assertThat(clh.getContentSize()).isEqualTo(3);
        assertThat(clh.getCategoryTitlePosition("Animal")).isEqualTo(3);
        assertThat(clh.getCategoryInsertPosition("Animal")).isEqualTo(4);
        assertThat(clh.getCategoryTitlePosition("Thing")).isEqualTo(4);
        assertThat(clh.getCategoryInsertPosition("Thing")).isEqualTo(6);

        assertThat(clh.getListIndexForContentIndex(0)).isEqualTo(1);
        assertThat(clh.getListIndexForContentIndex(1)).isEqualTo(2);
        assertThat(clh.getListIndexForContentIndex(2)).isEqualTo(5);

    }

    @Test
    public void removeCategory() {
        final CategorizedListHelper clh = getExampleHelper();
        clh.removeCategory("Animal");

        assertThat(clh.getContentSize()).isEqualTo(3);
        assertThat(clh.getCategoryTitlePosition("Animal")).isEqualTo(-1);
        assertThat(clh.getCategoryInsertPosition("Animal")).isEqualTo(-1);
        assertThat(clh.getCategoryTitlePosition("Thing")).isEqualTo(3);
        assertThat(clh.getCategoryInsertPosition("Thing")).isEqualTo(5);

        assertThat(clh.getListIndexForContentIndex(0)).isEqualTo(1);
        assertThat(clh.getListIndexForContentIndex(1)).isEqualTo(2);
        assertThat(clh.getListIndexForContentIndex(2)).isEqualTo(4);

    }


}
