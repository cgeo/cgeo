package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.LocalizationUtils;

import java.util.HashMap;
import java.util.Map;

public class StoredSinceGeocacheFilter extends NumberRangeGeocacheFilter<Long> {

    private static final Long[] range;
    private static final Map<Long, String> labelMap = new HashMap<>();
    private static final Map<Long, String> shortLabelMap = new HashMap<>();

    static {
        final int[] values = LocalizationUtils.getIntArray(R.array.cache_filter_stored_since_stored_values_h);
        final String[] labels = LocalizationUtils.getStringArray(R.array.cache_filter_stored_since_stored_values_label);
        final String[] shortLabels = LocalizationUtils.getStringArray(R.array.cache_filter_stored_since_stored_values_label_short);
        range = new Long[values.length + 3];
        range[0] = -10L;
        range[1] = 0L;
        range[range.length - 1] = Long.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            final Long value = (long) values[i] * 60 * 60;
            range[i + 2] = value;
            labelMap.put(value, i < labels.length ? labels[i] : values[i] + " hour(s)");
            shortLabelMap.put(value, i < shortLabels.length ? shortLabels[i] : values[i] + "h");
        }
    }

    public StoredSinceGeocacheFilter() {
        super(Long::valueOf);
    }

    /**
     * returns stored time in SECONDS, or -1 if cache is not stored at all
     */
    @Override
    public Long getValue(final Geocache cache) {
        if (!cache.isOffline()) {
            return -10L;
        }
        return (System.currentTimeMillis() - cache.getDetailedUpdate()) / 1000;
    }

    @Override
    protected String getSqlColumnExpression(final SqlBuilder sqlBuilder) {
        return "((" + System.currentTimeMillis() + " - " + sqlBuilder.getMainTableId() + ".detailedupdate) / 1000)";
    }

    @Override
    protected String getUserDisplayableConfig() {
        return toUserDisplayValue(getMinRangeValue(), true) + "-" + toUserDisplayValue(getMaxRangeValue(), true);
    }

    public static Long[] getValueRange() {
        return range;
    }

    public static String toUserDisplayValue(final Long value, final boolean useShort) {
        if (value < 0) {
            return useShort ? LocalizationUtils.getString(R.string.cache_filter_stored_since_notstored_short) : LocalizationUtils.getString(R.string.cache_filter_stored_since_notstored);
        }
        if (value == 0) {
            return useShort ? LocalizationUtils.getString(R.string.cache_filter_stored_since_stored_zero_short) : LocalizationUtils.getString(R.string.cache_filter_stored_since_stored_zero);
        }
        if (labelMap.containsKey(value)) {
            return useShort ? shortLabelMap.get(value) : labelMap.get(value);
        }
        return useShort ? LocalizationUtils.getString(R.string.cache_filter_stored_since_stored_max_short) : LocalizationUtils.getString(R.string.cache_filter_stored_since_stored_max);
    }

}
