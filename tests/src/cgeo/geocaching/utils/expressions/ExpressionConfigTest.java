package cgeo.geocaching.utils.expressions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ExpressionConfigTest {
    private static final List<String> stringListSingle = new ArrayList<>(Arrays.asList("singleValue"));
    private static final List<String> stringListMulti = new ArrayList<>(Arrays.asList("value1", "value2", "value3"));
    private static final String keyString = "testKey";

    /**
     * Put list with multi entries as default.
     * Try to get default list returns this list.
     */
    @Test
    public void testDefaultList() {
        final ExpressionConfig config = new ExpressionConfig();
        config.putDefaultList(stringListMulti);

        final List<String> defaultList = config.getDefaultList();
        assertThat(defaultList).isEqualTo(stringListMulti);
    }

    /**
     * Empty expression config.
     * Try to get default list returns empty list.
     */
    @Test
    public void testEmptyDefaultList() {
        final ExpressionConfig config = new ExpressionConfig();

        final List<String> defaultList = config.getDefaultList();
        assertThat(defaultList).isNotNull();
        assertThat(defaultList.size()).isEqualTo(0);
    }

    /**
     * Put list with multi entries as default.
     * Add single value to default list.
     * Try to get default list returns entries from multi-list plus the single value.
     */
    @Test
    public void testAddToDefaultList() {
        final ExpressionConfig config = new ExpressionConfig();
        config.putDefaultList(stringListMulti);
        final int sizeBefore = stringListMulti.size();
        config.addToDefaultList("newValue");

        final List<String> defaultList = config.getDefaultList();
        assertThat(defaultList.size()).isEqualTo(sizeBefore + 1);
    }

    /**
     * Put list with single entries as default.
     * Try to get single value returns value of list.
     */
    @Test
    public void testGetSingleValueListSingle() {
        final ExpressionConfig config = new ExpressionConfig();
        config.putDefaultList(stringListSingle);

        final String singleValue = config.getSingleValue();
        assertThat(singleValue).isNotNull();
        assertThat(singleValue).isEqualTo(stringListSingle.get(0));
    }

    /**
     * Put list with multi entries as default.
     * Try to get single value returns null.
     */
    @Test
    public void testGetSingleValueListMulti() {
        final ExpressionConfig config = new ExpressionConfig();
        config.putDefaultList(stringListMulti);

        final String singleValue = config.getSingleValue();
        assertThat(singleValue).isNull();
    }

    /**
     * Add value to config 'keyString'.
     * Try to get value from config.
     */
    @Test
    public void testGetKeyValue() {
        final ExpressionConfig config = new ExpressionConfig();
        config.put(keyString, stringListMulti);

        final List<String> value = config.get(keyString);
        assertThat(value).isNotNull();
        assertThat(value).isEqualTo(stringListMulti);
    }

    /**
     * Add single value to config 'keyString'.
     * Try to get first value.
     */
    @Test
    public void testGetFirstValue() {
        final ExpressionConfig config = new ExpressionConfig();
        config.putList(keyString, "true");

        final boolean firstValue = config.getFirstValue(keyString, false, BooleanUtils::toBoolean);
        assertThat(firstValue).isTrue();
    }

    /**
     * Add single value to config 'keyString', which is not valid for the converter.
     * Try to get first value returns default value.
     */
    @Test
    public void testGetFirstValueDefault() {
        final ExpressionConfig config = new ExpressionConfig();
        config.putList(keyString, "value");

        final boolean firstValue = config.getFirstValue(keyString, false, BooleanUtils::toBoolean);
        assertThat(firstValue).isFalse();
    }

    /**
     * Add value to config 'keyString'.
     * Create SubConfig from 'keyString' returns config with value from 'keyString' in defaultList.
     */
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
