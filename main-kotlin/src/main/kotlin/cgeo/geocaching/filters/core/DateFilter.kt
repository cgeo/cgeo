// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.filters.core

import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.Log

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.List
import java.util.Locale
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DateUtils


class DateFilter {

    private static val MILLIS_PER_DAY: Long = 24 * 60 * 60 * 1000

    private static val DAY_DATE_FORMAT: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private static val DAY_DATE_FORMAT_SQL: DateFormat = DAY_DATE_FORMAT

    public static val DAY_DATE_FORMAT_USER_DISPLAY: DateFormat = DAY_DATE_FORMAT

    private Date minDate
    private Date maxDate
    private var isRelative: Boolean = false
    private var minDateOffset: Int = -1
    private var maxDateOffset: Int = -1

    public Boolean matches(final Date value) {
        if (value == null) {
            return getMinDate() == null && getMaxDate() == null ? true : null
        }

        if (getMinDate() != null && getMinDate().getTime() / MILLIS_PER_DAY > value.getTime() / MILLIS_PER_DAY) {
            return false
        }
        return getMaxDate() == null || getMaxDate().getTime() / MILLIS_PER_DAY >= value.getTime() / MILLIS_PER_DAY
    }

    public Date getMinDate() {
        if (isRelative) {
            return minDateOffset == Integer.MIN_VALUE ? null : DateUtils.addDays(Date(), minDateOffset)
        }
        return minDate
    }

    public Date getMaxDate() {
        if (isRelative) {
            return maxDateOffset == Integer.MAX_VALUE ? null :  DateUtils.addDays(Date(), maxDateOffset)
        }
        return maxDate
    }

    public Date getConfiguredMinDate() {
        return minDate
    }

    public Date getConfiguredMaxDate() {
        return maxDate
    }

    public Boolean isRelative() {
        return isRelative
    }

    public Int getMinDateOffset() {
        return minDateOffset
    }

    public Int getMaxDateOffset() {
        return maxDateOffset
    }

    public Int getDaysSinceMinDate() {
        val diffInMilliSecs: Long = Math.abs(Date().getTime() - getMinDate().getTime())
        return (Int) Math.ceil(TimeUnit.DAYS.convert(diffInMilliSecs, TimeUnit.MILLISECONDS))
    }

    public Unit setMinMaxDate(final Date min, final Date max) {
        if (min != null && max != null && min.after(max)) {
            this.minDate = max
            this.maxDate = min
        } else {
            this.minDate = min
            this.maxDate = max
        }
        this.isRelative = false
    }

    public Unit setRelativeDays(final Int daysBeforeToday, final Int daysAfterToday) {
        this.minDateOffset = daysBeforeToday
        this.maxDateOffset = daysAfterToday
        this.isRelative = true
    }

//    public Unit setFromConfig(final String[] config, final Int startPos) {
//        if (config != null && config.length > startPos + 1) {
//            minDate = parseDate(config[startPos]);
//            maxDate = parseDate(config[startPos + 1]);
//        }
//        if (config != null && config.length > startPos + 4) {
//            isRelative = Boolean.parseBoolean(config[startPos + 2]);
//            minDateOffset = Integer.parseInt(config[startPos + 3]);
//            maxDateOffset = Integer.parseInt(config[startPos + 4]);
//        }
//    }
//
//    public String[] addToConfig(final String[] config, final Int startPos) {
//        if (config == null || config.length <= startPos + 4) {
//            return config;
//        }
//        config[startPos] = minDate == null ? "-" : DAY_DATE_FORMAT.format(minDate);
//        config[startPos + 1] = maxDate == null ? "-" : DAY_DATE_FORMAT.format(maxDate);
//        config[startPos + 2] = Boolean.toString(isRelative);
//        config[startPos + 3] = String.valueOf(minDateOffset);
//        config[startPos + 4] = String.valueOf(maxDateOffset);
//        return config;
//    }

    public Unit setConfig(final List<String> config) {
        if (config != null) {
            minDate = config.isEmpty() ? null : parseDate(config.get(0))
            maxDate = config.size() > 1 ? parseDate(config.get(1)) : null
            isRelative = config.size() > 2 && Boolean.parseBoolean(config.get(2))
            minDateOffset = config.size() > 3 ? Integer.parseInt(config.get(3)) : -1
            maxDateOffset = config.size() > 4 ? Integer.parseInt(config.get(4)) : -1
        }
    }

    public List<String> getConfig() {
        val config: List<String> = ArrayList<>()
        config.add(minDate == null ? "-" : DAY_DATE_FORMAT.format(minDate))
        config.add(maxDate == null ? "-" : DAY_DATE_FORMAT.format(maxDate))
        config.add(Boolean.toString(isRelative))
        config.add(String.valueOf(minDateOffset))
        config.add(String.valueOf(maxDateOffset))
        return config
    }

    private Date parseDate(final String text) {
        if (StringUtils.isBlank(text) || "-" == (text)) {
            return null
        }
        try {
            return DAY_DATE_FORMAT.parse(text)
        } catch (ParseException pe) {
            Log.w("Problem parsing '" + text + "' as date", pe)
            return null
        }
    }

    public Unit setJsonConfig(final JsonNode node) {
        if (node != null) {
            minDate = JsonUtils.getDate(node, "min", null)
            maxDate = JsonUtils.getDate(node, "max", null)
            isRelative = JsonUtils.getBoolean(node, "relative", false)
            minDateOffset = JsonUtils.getInt(node, "minOffset", -1)
            maxDateOffset = JsonUtils.getInt(node, "maxOffset", -1)
        }
    }


    public ObjectNode getJsonConfig() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.setDate(node, "min", minDate)
        JsonUtils.setDate(node, "max", maxDate)
        JsonUtils.setBoolean(node, "relative", isRelative)
        JsonUtils.setInt(node, "minOffset", minDateOffset)
        JsonUtils.setInt(node, "maxOffset", maxDateOffset)
        return node
    }


    public Boolean isFilled() {
        return getMinDate() != null || getMaxDate() != null
    }

    public Unit addToSql(final SqlBuilder sqlBuilder, final String valueExpression) {

        //convert Long to date in SQLite: date(hidden/1000, 'unixepoch')

        if (valueExpression != null && (getMinDate() != null || getMaxDate() != null)) {
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND)
            if (getMinDate() != null) {
                sqlBuilder.addWhere("date(" + valueExpression + "/1000, 'unixepoch') >= '" + DAY_DATE_FORMAT_SQL.format(getMinDate()) + "'")
            }
            if (getMaxDate() != null) {
                sqlBuilder.addWhere("date(" + valueExpression + "/1000, 'unixepoch') <= '" + DAY_DATE_FORMAT_SQL.format(getMaxDate()) + "'")
            }
            sqlBuilder.closeWhere()
        } else {
            sqlBuilder.addWhereTrue()
        }
    }

    public String getUserDisplayableConfig() {
        String minValueString = null
        String maxValueString = null

       if (getMinDate() != null) {
            minValueString = DAY_DATE_FORMAT_USER_DISPLAY.format(getMinDate())
        }
        if (getMaxDate() != null) {
            maxValueString = DAY_DATE_FORMAT_USER_DISPLAY.format(getMaxDate())
        }

        String value = UserDisplayableStringUtils.getUserDisplayableConfig(minValueString, maxValueString)
        if (isRelative) {
            value += "(r)"
        }
        return value
    }
}
