package cgeo.geocaching.filters.core;

import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;


public class DateFilter  {

    private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000;

    private static final DateFormat DAY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final DateFormat DAY_DATE_FORMAT_SQL = DAY_DATE_FORMAT;

    private Date minDate;
    private Date maxDate;

    public Boolean matches(final Date value) {
        if (value == null || value.getTime() == 0) {
            return minDate == null && maxDate == null ? true : null;
        }

        if (minDate != null && minDate.getTime() / MILLIS_PER_DAY > value.getTime() / MILLIS_PER_DAY) {
            return false;
        }
        return maxDate == null || maxDate.getTime() / MILLIS_PER_DAY >= value.getTime() / MILLIS_PER_DAY;
    }

    public Date getMinDate() {
        return minDate;
    }

    public Date getMaxDate() {
        return maxDate;
    }

    public void setMinMaxDate(final Date min, final Date max) {
        if (min != null && max != null && min.after(max)) {
            this.minDate = max;
            this.maxDate = min;
        } else {
            this.minDate = min;
            this.maxDate = max;
        }
    }

    public void setFromConfig(final String[] config, final int startPos) {
        if (config != null && config.length > startPos + 1) {
            minDate = parseDate(config[startPos]);
            maxDate = parseDate(config[startPos + 1]);
        }
    }

    public String[] addToConfig(final String[] config, final int startPos) {
        if (config == null || config.length <= startPos + 1) {
            return config;
        }
        config[startPos] = minDate == null ? "-" : DAY_DATE_FORMAT.format(minDate);
        config[startPos + 1] = maxDate == null ? "-" : DAY_DATE_FORMAT.format(maxDate);
        return config;
    }

     private Date parseDate(final String text) {
        if (StringUtils.isBlank(text) || "-".equals(text)) {
            return null;
        }
        try {
            return DAY_DATE_FORMAT.parse(text);
        } catch (ParseException pe) {
            Log.w("Problem parsing '" + text + "' as date", pe);
            return null;
        }
    }

    public boolean isFilled() {
        return minDate != null || maxDate != null;
    }

    public void addToSql(final SqlBuilder sqlBuilder, final String valueExpression) {

        //convert long to date in SQLite: date(hidden/1000, 'unixepoch')

        if (valueExpression != null && (minDate != null || maxDate != null)) {
            sqlBuilder.openWhere(SqlBuilder.WhereType.OR);
            sqlBuilder.addWhere("(" + valueExpression + ") == 0"); //those are the inconclusives
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
            if (minDate != null) {
                sqlBuilder.addWhere("date(" + valueExpression + "/1000, 'unixepoch') >= '" + DAY_DATE_FORMAT_SQL.format(minDate) + "'");
            }
            if (maxDate != null) {
                sqlBuilder.addWhere("date(" + valueExpression + "/1000, 'unixepoch') <= '" + DAY_DATE_FORMAT_SQL.format(maxDate) + "'");
            }
            sqlBuilder.closeWhere();
            sqlBuilder.closeWhere();
        } else {
            sqlBuilder.addWhereAlwaysInclude();
        }
    }


    public String getUserDisplayableConfig() {

        final StringBuilder sb = new StringBuilder();
        sb.append(minDate == null ? "*" : DAY_DATE_FORMAT.format(minDate));
        sb.append("-");
        sb.append(maxDate == null ? "*" : DAY_DATE_FORMAT.format(maxDate));
        return sb.toString();
    }

}
