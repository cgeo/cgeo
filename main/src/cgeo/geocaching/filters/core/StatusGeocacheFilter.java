package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.SqlBuilder;

import org.apache.commons.lang3.BooleanUtils;

public class StatusGeocacheFilter extends BaseGeocacheFilter {

    private Boolean statusDisabled = null;
    private Boolean statusArchived = null;
    private Boolean statusOwn = null;
    private Boolean statusFound = null;


    @Override
    public Boolean filter(final Geocache cache) {
        return
            (statusDisabled == null || cache.isDisabled() == statusDisabled) &&
            (statusArchived == null || cache.isArchived() == statusArchived) &&
            (statusOwn == null || (cache.isOwn() == statusOwn)) &&
            (statusFound == null || cache.isFound() == statusFound);
    }

    public Boolean getStatusDisabled() {
        return statusDisabled;
    }

    public void setStatusDisabled(final Boolean statusDisabled) {
        this.statusDisabled = statusDisabled;
    }

    public Boolean getStatusArchived() {
        return statusArchived;
    }

    public void setStatusArchived(final Boolean statusArchived) {
        this.statusArchived = statusArchived;
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
    public void setConfig(final String[] value) {
        statusOwn = value.length > 0 ? BooleanUtils.toBooleanObject(value[0]) : null;
        statusFound = value.length > 1 ? BooleanUtils.toBooleanObject(value[1]) : null;
        statusDisabled = value.length > 2 ? BooleanUtils.toBooleanObject(value[2]) : null;
        statusArchived = value.length > 3 ? BooleanUtils.toBooleanObject(value[3]) : null;
    }

    @Override
    public String[] getConfig() {
        return new String[]{BooleanUtils.toStringTrueFalse(statusOwn), BooleanUtils.toStringTrueFalse(statusFound),
            BooleanUtils.toStringTrueFalse(statusDisabled), BooleanUtils.toStringTrueFalse(statusArchived)};
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (statusOwn == null && statusFound == null && statusDisabled == null && statusArchived == null) {
            sqlBuilder.addWhereAlwaysInclude();
        } else {
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
            if (statusOwn != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".owner_real " + (statusOwn ? "=" : "<>") + " ?", Settings.getUserName());
            }
            if (statusFound != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".found = " + (statusFound ? "1" : "0"));
            }
            if (statusDisabled != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".disabled = " + (statusDisabled ? "1" : "0"));
            }
            if (statusArchived != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".archived = " + (statusArchived ? "1" : "0"));
            }
            sqlBuilder.closeWhere();
        }
    }
}
