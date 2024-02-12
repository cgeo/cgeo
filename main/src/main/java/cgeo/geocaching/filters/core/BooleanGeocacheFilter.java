package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class BooleanGeocacheFilter extends BaseGeocacheFilter {

    public static final String yesFlag = "yes";
    public static final String noFlag = "no";

    private Boolean value = null;


    @Override
    public Boolean filter(final Geocache cache) {
        return value == null || filter(cache, value);
    }

    public abstract Boolean filter(Geocache cache, boolean value);


    public Boolean getValue() {
        return value;
    }

    public void setValue(final Boolean value) {
        this.value = value;
    }

    private void setConfigInternal(final List<String> configValues) {
        value = null;

        for (String value : configValues) {
            if (checkBooleanFlag(yesFlag, value)) {
                this.value = true;
            }
            if (checkBooleanFlag(noFlag, value)) {
                this.value = false;
            }

        }
    }

    private List<String> getConfigInternal() {
        final List<String> result = new ArrayList<>();
        if (value != null) {
            result.add(value ? yesFlag : noFlag);
        }
        return result;
    }


    @Override
    public boolean isFiltering() {
        return value != null;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (value == null) {
            sqlBuilder.addWhereTrue();
        } else {
            addToSql(sqlBuilder, value);
        }
    }

    public abstract void addToSql(SqlBuilder sqlBuilder, boolean value);

    @Override
    protected String getUserDisplayableConfig() {
        if (value != null) {
            return LocalizationUtils.getString(value ? R.string.cache_filter_status_select_yes : R.string.cache_filter_status_select_no);
        }
        return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none);
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.setTextCollection(node, "values", getConfigInternal());
        return node;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode node) {
        setConfigInternal(JsonUtils.getTextList(node, "values"));
    }
}
