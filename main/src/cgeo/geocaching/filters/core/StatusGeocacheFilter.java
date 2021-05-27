package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.expressions.ExpressionConfig;

import androidx.annotation.StringRes;

import java.util.List;

import org.apache.commons.lang3.BooleanUtils;

public class StatusGeocacheFilter extends BaseGeocacheFilter {

    public enum StatusType {
        OWN(R.string.cache_filter_status_select_all, R.string.cache_filter_status_select_only_own_no, R.string.cache_filter_status_select_only_own_yes),
        FOUND(R.string.cache_filter_status_select_all, R.string.cache_filter_status_select_only_found_no, R.string.cache_filter_status_select_only_found_yes);

        @StringRes public final int allId;
        @StringRes public final int noId;
        @StringRes public final int yesId;

        StatusType(@StringRes final int allId, @StringRes final int  noId, @StringRes final int yesId) {
            this.allId = allId;
            this.noId = noId;
            this.yesId = yesId;
        }

    }

    private boolean excludeActive = true;
    private boolean excludeDisabled = true;
    private boolean excludeArchived = true;

    private Boolean statusOwn = null;
    private Boolean statusFound = null;


    @Override
    public Boolean filter(final Geocache cache) {
        return
            ((!excludeActive && !cache.isDisabled() && !cache.isArchived()) ||
                (!excludeDisabled && cache.isDisabled()) ||
                (!excludeArchived && cache.isArchived())) &&
            (statusOwn == null || (cache.isOwner() == statusOwn)) &&
            (statusFound == null || cache.isFound() == statusFound);
    }

    public boolean isExcludeActive() {
        return excludeActive;
    }

    public void setExcludeActive(final boolean excludeActive) {
        this.excludeActive = excludeActive;
    }

    public boolean isExcludeDisabled() {
        return excludeDisabled;
    }

    public void setExcludeDisabled(final boolean excludeDisabled) {
        this.excludeDisabled = excludeDisabled;
    }

    public boolean isExcludeArchived() {
        return excludeArchived;
    }

    public void setExcludeArchived(final boolean excludeArchived) {
        this.excludeArchived = excludeArchived;
    }

    public Boolean getStatusOwn() {
        return statusOwn;
    }

    public void setStatusOwn(final Boolean statusOwn) {
        this.statusOwn = statusOwn;
    }

    public Boolean getStatusFound() {
        return statusFound;
    }

    public void setStatusFound(final Boolean statusFound) {
        this.statusFound = statusFound;
    }

    @Override
    public void setConfig(final ExpressionConfig config) {
        final List<String> value = config.getDefaultList();
        statusOwn = value.size() > 0 ? BooleanUtils.toBooleanObject(value.get(0)) : null;
        statusFound = value.size() > 1 ? BooleanUtils.toBooleanObject(value.get(1)) : null;
        excludeActive = value.size() > 2 && BooleanUtils.toBoolean(value.get(2));
        excludeDisabled = value.size() > 3 && BooleanUtils.toBoolean(value.get(3));
        excludeArchived = value.size() > 4 && BooleanUtils.toBoolean(value.get(4));
    }

    @Override
    public ExpressionConfig getConfig() {
        final ExpressionConfig result = new ExpressionConfig();
        result.addToDefaultList(
            BooleanUtils.toStringTrueFalse(statusOwn), BooleanUtils.toStringTrueFalse(statusFound),
            BooleanUtils.toStringTrueFalse(excludeActive), BooleanUtils.toStringTrueFalse(excludeDisabled),
            BooleanUtils.toStringTrueFalse(excludeArchived)
        );
        return result;

    }


    @Override
    public boolean isFiltering() {
        return statusOwn != null || statusFound != null || excludeArchived || excludeDisabled || excludeActive;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (!isFiltering()) {
            sqlBuilder.addWhereTrue();
        } else {
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
            if (statusOwn != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".owner_real " + (statusOwn ? "=" : "<>") + " ?", Settings.getUserName());
            }
            if (statusFound != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".found = " + (statusFound ? "1" : "0"));
            }
            if (excludeActive) {
                sqlBuilder.openWhere(SqlBuilder.WhereType.OR);
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".disabled = 1");
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".archived = 1");
                sqlBuilder.closeWhere();
            }
            if (excludeDisabled) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".disabled = 0");
            }
            if (excludeArchived) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".archived = 0");
            }
            sqlBuilder.closeWhere();
        }
    }

    @Override
    protected String getUserDisplayableConfig() {
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        count = addIfFirst(sb, count, statusFound, StatusType.FOUND);
        count = addIfFirst(sb, count, statusOwn, StatusType.OWN);
        count = addIfTrue(sb, count, excludeActive, R.string.cache_filter_status_exclude_active);
        count = addIfTrue(sb, count, excludeDisabled, R.string.cache_filter_status_exclude_disabled);
        count = addIfTrue(sb, count, excludeArchived, R.string.cache_filter_status_exclude_archived);
        if (count == 0) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none);
        }
        if (count > 1) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, count);
        }

        return sb.toString();
    }

    private int addIfFirst(final StringBuilder sb, final int cnt, final Boolean status, final StatusType statusType) {
        if (status != null) {
            if (cnt == 0) {
                sb.append(LocalizationUtils.getString(status ? statusType.yesId : statusType.noId));
            }
            return cnt + 1;
        }
        return cnt;
    }

    private int addIfTrue(final StringBuilder sb, final int cnt, final boolean status, @StringRes final int textId) {
        if (status) {
            if (cnt == 0) {
                sb.append(LocalizationUtils.getString(textId));
            }
            return cnt + 1;
        }
        return cnt;
    }
}
