package cgeo.geocaching.utils.expressions;

import cgeo.geocaching.utils.functions.Func1;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ExpressionConfig extends HashMap<String, List<String>> {

    /**
     * IF this config has only one single String value then this value is returned. Otherwise null is returned
     */
    public String getSingleValue() {
        final List<String> defaultList = getDefaultList();
        return size() == 1 && defaultList.size() == 1 ? defaultList.get(0) : null;
    }

    public <T> T getFirstValue(final String key, final T defaultValue, final Func1<String, T> converter) {
        final List<String> values = get(key);
        return values != null && !values.isEmpty() ? converter.call(values.get(0)) : defaultValue;
    }

    public void putList(final String key, final String... values) {
        put(key, new ArrayList<>(Arrays.asList(values)));
    }

    @NonNull
    public List<String> getDefaultList() {
        final List<String> result = get(null);
        return result == null ? Collections.emptyList() : result;
    }

    public ExpressionConfig addToDefaultList(final String... values) {
        if (get(null) == null) {
            put(null, new ArrayList<>());
        }
        get(null).addAll(Arrays.asList(values));
        return this;
    }

    public void putDefaultList(final List<String> list) {
        put(null, list);
    }

    /**
     * creates a new config object, taking the content of one key of this config and putting it into the default list
     */
    public ExpressionConfig getSubConfig(final String key) {
        final ExpressionConfig subConfig = new ExpressionConfig();
        subConfig.putDefaultList(this.get(key));
        return subConfig;
    }
}
