package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.SqlBuilder;

import org.apache.commons.lang3.BooleanUtils;

public class StatusGeocacheFilter extends BaseGeocacheFilter {

    private boolean showDisabled = true;
    private boolean showArchived = true;
    private boolean showOwnFound = true;


    @Override
    public Boolean filter(final Geocache cache) {
        return
            (showOwnFound || (!cache.isFound() && !cache.isOwner())) &&
            (showDisabled || (!cache.isDisabled())) &&
            (showArchived || (!cache.isArchived()));
    }

    public boolean isShowDisabled() {
        return showDisabled;
    }

    public void setShowDisabled(final boolean showDisabled) {
        this.showDisabled = showDisabled;
    }

    public boolean isShowArchived() {
        return showArchived;
    }

    public void setShowArchived(final boolean showArchived) {
        this.showArchived = showArchived;
    }

    public boolean isShowOwnFound() {
        return showOwnFound;
    }

    public void setShowOwnFound(final boolean showOwnFound) {
        this.showOwnFound = showOwnFound;
    }

    @Override
    public void setConfig(final String[] value) {
        showOwnFound = value.length > 0 && BooleanUtils.toBoolean(value[0]);
        showDisabled = value.length > 1 && BooleanUtils.toBoolean(value[1]);
        showArchived = value.length > 2 && BooleanUtils.toBoolean(value[2]);
    }

    @Override
    public String[] getConfig() {
        return new String[]{Boolean.toString(showOwnFound), Boolean.toString(showDisabled), Boolean.toString(showArchived)};
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (showArchived && showDisabled && showOwnFound) {
            sqlBuilder.addWhereAlwaysInclude();
        } else {
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
            if (!showOwnFound) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".found = 0");
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".owner_real <> ?", new String[]{Settings.getUserName()});
            }
            if (!showDisabled) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".disabled = 0");
            }
            if (!showArchived) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".archived = 0");
            }
            sqlBuilder.closeWhere();
        }
    }
}
