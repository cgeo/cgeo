package cgeo.geocaching.utils.expressions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ExpressionConfigTest {
    final static List<String> stringListMulti = new ArrayList<>(Arrays.asList("value1", "value2", "value3"));
    final static List<String> stringListSingle = new ArrayList<>(Arrays.asList("singleValue"));
    final static String keyString = "testKey";

    @Test
    public void testDefaultList() {
        final ExpressionConfig config = new ExpressionConfig();
        config.putDefaultList(stringListMulti);

        final List<String> defaultList = config.getDefaultList();
        assertThat(defaultList).isEqualTo(stringListMulti);
    }

    @Test
    public void testEmptyDefaultList() {
        final ExpressionConfig config = new ExpressionConfig();

        final List<String> defaultList = config.getDefaultList();
        assertThat(defaultList).isNotNull();
        assertThat(defaultList.size()).isEqualTo(0);
    }

    @Test
    public void testAddToDefaultList() {
        final ExpressionConfig config = new ExpressionConfig();
        config.putDefaultList(stringListMulti);
        final int sizeBefore = stringListMulti.size();
        config.addToDefaultList("newValue");

        final List<String> defaultList = config.getDefaultList();
        assertThat(defaultList.size()).isEqualTo(sizeBefore + 1);
    }

    @Test
    public void testGetSingleValueDefaultSizeSingle() {
        final ExpressionConfig config = new ExpressionConfig();
        config.putDefaultList(stringListSingle);

        final String singleValue = config.getSingleValue();
        assertThat(singleValue).isNotNull();
    }

    @Test
    public void testGetSingleValueDefaultSizeMulti() {
        final ExpressionConfig config = new ExpressionConfig();
        config.putDefaultList(stringListMulti);

        final String singleValue = config.getSingleValue();
        assertThat(singleValue).isNull();
    }

    @Test
    public void testGetFirstValue() {
        final ExpressionConfig config = new ExpressionConfig();
        config.putList(keyString, "true");

        final boolean firstValue = config.getFirstValue(keyString, false, BooleanUtils::toBoolean);
        assertThat(firstValue).isTrue();
    }

    @Test
    public void testGetFirstValueDefault() {
        final ExpressionConfig config = new ExpressionConfig();
        config.putList(keyString, "value");

        final boolean firstValue = config.getFirstValue(keyString, false, BooleanUtils::toBoolean);
        assertThat(firstValue).isFalse();
    }

    @Test
    public void testGetSubConfig() {
        final ExpressionConfig config = new ExpressionConfig();
        config.put(keyString, stringListMulti);

        final ExpressionConfig subConfig = config.getSubConfig(keyString);
        assertThat(subConfig).isNotNull();
        final List<String> defaultList = subConfig.getDefaultList();
        assertThat(defaultList).isEqualTo(stringListMulti);
    }
}
