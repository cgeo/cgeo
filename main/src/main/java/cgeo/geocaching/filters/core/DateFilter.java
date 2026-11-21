package cgeo.geocaching.filters.core;

import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;


public class DateFilter {

    private static final DateTimeFormatter DAY_DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.US);
    private static final DateTimeFormatter DAY_MONTH_FORMAT = DateTimeFormatter.ofPattern("MM-dd", Locale.US);
    private static final DateTimeFormatter JSON_DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss", Locale.US);

    private final boolean truncateToDays;
    private Date minDate;
    private Date maxDate;
    private LocalDate minLocalDate;
    private LocalDate maxLocalDate;
    private boolean isRelative = false;
    private int minDateOffset = -1;
    private int maxDateOffset = -1;
    private boolean ignoreYear = false;

    public DateFilter() {
        this(true);
    }

    public DateFilter(final boolean truncateToDays) {
        this.truncateToDays = truncateToDays;
    }

    public Boolean matches(final Date value) {
        final EffectiveDateRange range = getEffectiveRange();
        if (value == null) {
            return range.min == null && range.max == null ? true : null;
        }

        if (ignoreYear) {
            return matchesDayOfYear(value, range);
        }

        if (range.min != null && value.before(range.min)) {
            return false;
        }
        return range.max == null || !value.after(range.max);
    }

    private Boolean matchesDayOfYear(final Date value, final EffectiveDateRange range) {
        if (range.minDay == null && range.maxDay == null) {
            return true;
        }

        final String valueDayOfYear = DAY_MONTH_FORMAT.format(toLocalDate(value, range.zone));
        final String minDayOfYear = formatLocalDate(range.minDay, DAY_MONTH_FORMAT);
        final String maxDayOfYear = formatLocalDate(range.maxDay, DAY_MONTH_FORMAT);

        if (minDayOfYear != null && maxDayOfYear != null) {
            if (minDayOfYear.compareTo(maxDayOfYear) <= 0) {
                // Normal range (e.g., 03-01 to 11-30)
                return valueDayOfYear.compareTo(minDayOfYear) >= 0 &&
                       valueDayOfYear.compareTo(maxDayOfYear) <= 0;
            }
            // Wraps around year boundary (e.g., 11-01 to 02-28)
            return valueDayOfYear.compareTo(minDayOfYear) >= 0 ||
                   valueDayOfYear.compareTo(maxDayOfYear) <= 0;
        } else if (minDayOfYear != null) {
            return valueDayOfYear.compareTo(minDayOfYear) >= 0;
        }
        return valueDayOfYear.compareTo(maxDayOfYear) <= 0;
    }

    public Date getMinDate() {
        return getEffectiveRange().min;
    }

    public Date getMaxDate() {
        return getEffectiveRange().max;
    }

    private EffectiveDateRange getEffectiveRange() {
        final ZoneId zone = ZoneId.systemDefault();
        if (isRelative) {
            return getEffectiveRelativeRange(zone);
        }
        if (truncateToDays) {
            return createDayRange(minLocalDate, maxLocalDate, zone);
        }
        return createExactRange(minDate, maxDate, zone);
    }

    private EffectiveDateRange getEffectiveRelativeRange(final ZoneId zone) {
        final ZonedDateTime now = ZonedDateTime.now(zone);
        if (truncateToDays) {
            final LocalDate today = now.toLocalDate();
            final LocalDate effectiveMinDay = minDateOffset == Integer.MIN_VALUE ? null : today.plusDays(minDateOffset);
            final LocalDate effectiveMaxDay = maxDateOffset == Integer.MAX_VALUE ? null : today.plusDays(maxDateOffset);
            return createDayRange(effectiveMinDay, effectiveMaxDay, zone);
        }

        final Date effectiveMin = minDateOffset == Integer.MIN_VALUE ? null : toDate(now.plusDays(minDateOffset).toInstant());
        final Date effectiveMax = maxDateOffset == Integer.MAX_VALUE ? null : toDate(now.plusDays(maxDateOffset).toInstant());
        return createExactRange(effectiveMin, effectiveMax, zone);
    }

    private EffectiveDateRange createDayRange(final LocalDate minDay, final LocalDate maxDay, final ZoneId zone) {
        final Date effectiveMin = minDay == null ? null : toDate(minDay.atStartOfDay(zone).toInstant());
        final Date effectiveMax = maxDay == null ? null :
                toDate(maxDay.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1));
        return new EffectiveDateRange(effectiveMin, effectiveMax, minDay, maxDay, zone);
    }

    private EffectiveDateRange createExactRange(final Date exactMin, final Date exactMax, final ZoneId zone) {
        return new EffectiveDateRange(exactMin, exactMax,
                exactMin == null ? null : toLocalDate(exactMin, zone),
                exactMax == null ? null : toLocalDate(exactMax, zone), zone);
    }

    public Date getConfiguredMinDate() {
        return truncateToDays ? createDayRange(minLocalDate, null, ZoneId.systemDefault()).min : minDate;
    }

    public Date getConfiguredMaxDate() {
        return truncateToDays ? createDayRange(null, maxLocalDate, ZoneId.systemDefault()).max : maxDate;
    }

    public boolean isRelative() {
        return isRelative;
    }

    public int getMinDateOffset() {
        return minDateOffset;
    }

    public int getMaxDateOffset() {
        return maxDateOffset;
    }

    public int getDaysSinceMinDate() {
        final long diffInMilliSecs = Math.abs(new Date().getTime() - getMinDate().getTime());
        return (int) Math.ceil(TimeUnit.DAYS.convert(diffInMilliSecs, TimeUnit.MILLISECONDS));
    }

    public void setMinMaxDate(final Date min, final Date max) {
        setConfiguredMinMaxDate(min, max, ZoneId.systemDefault());
        this.isRelative = false;
    }

    private void setConfiguredMinMaxDate(final Date min, final Date max, final ZoneId zone) {
        if (truncateToDays) {
            this.minLocalDate = min == null ? null : toLocalDate(min, zone);
            this.maxLocalDate = max == null ? null : toLocalDate(max, zone);
            this.minDate = null;
            this.maxDate = null;
        } else {
            this.minDate = min;
            this.maxDate = max;
            this.minLocalDate = null;
            this.maxLocalDate = null;
        }
    }

    private void setConfiguredLocalDates(final LocalDate min, final LocalDate max) {
        this.minLocalDate = min;
        this.maxLocalDate = max;
        this.minDate = null;
        this.maxDate = null;
    }

    public boolean isIgnoreYear() {
        return ignoreYear;
    }

    public void setIgnoreYear(final boolean ignoreYear) {
        this.ignoreYear = ignoreYear;
    }

    public void setRelativeDays(final int daysBeforeToday, final int daysAfterToday) {
        this.minDateOffset = daysBeforeToday;
        this.maxDateOffset = daysAfterToday;
        this.isRelative = true;
    }


    public void setConfig(final List<String> config) {
        if (config != null) {
            final String minText = config.isEmpty() ? null : config.get(0);
            final String maxText = config.size() > 1 ? config.get(1) : null;
            if (truncateToDays) {
                setConfiguredLocalDates(parseLocalDate(minText), parseLocalDate(maxText));
            } else {
                final ZoneId zone = ZoneId.systemDefault();
                setConfiguredMinMaxDate(parseDate(minText, zone), parseDate(maxText, zone), zone);
            }
            isRelative = config.size() > 2 && Boolean.parseBoolean(config.get(2));
            minDateOffset = config.size() > 3 ? Integer.parseInt(config.get(3)) : -1;
            maxDateOffset = config.size() > 4 ? Integer.parseInt(config.get(4)) : -1;
            ignoreYear = config.size() > 5 && Boolean.parseBoolean(config.get(5));
        }
    }

    public List<String> getConfig() {
        final List<String> config = new ArrayList<>();
        if (truncateToDays) {
            config.add(formatLocalDateOrDefault(minLocalDate, "-"));
            config.add(formatLocalDateOrDefault(maxLocalDate, "-"));
        } else {
            final ZoneId zone = ZoneId.systemDefault();
            config.add(minDate == null ? "-" : formatLocalDate(toLocalDate(minDate, zone), DAY_DATE_FORMAT));
            config.add(maxDate == null ? "-" : formatLocalDate(toLocalDate(maxDate, zone), DAY_DATE_FORMAT));
        }
        config.add(Boolean.toString(isRelative));
        config.add(String.valueOf(minDateOffset));
        config.add(String.valueOf(maxDateOffset));
        config.add(Boolean.toString(ignoreYear));
        return config;
    }

    private LocalDate parseLocalDate(final String text) {
        if (StringUtils.isBlank(text) || "-".equals(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text, DAY_DATE_FORMAT);
        } catch (DateTimeException e) {
            Log.w("Problem parsing '" + text + "' as date", e);
            return null;
        }
    }

    private Date parseDate(final String text, final ZoneId zone) {
        final LocalDate localDate = parseLocalDate(text);
        return localDate == null ? null : toDate(localDate.atStartOfDay(zone).toInstant());
    }

    private LocalDateTime parseJsonLocalDateTime(final String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, JSON_DATE_FORMAT);
        } catch (DateTimeException e) {
            Log.w("Problem parsing '" + text + "' as date and time", e);
            return null;
        }
    }

    private Date parseJsonDate(final String text, final ZoneId zone) {
        final LocalDateTime localDateTime = parseJsonLocalDateTime(text);
        return localDateTime == null ? null : toDate(localDateTime.atZone(zone).toInstant());
    }

    public void setJsonConfig(final JsonNode node) {
        if (node != null) {
            final String minText = JsonUtils.getText(node, "min", null);
            final String maxText = JsonUtils.getText(node, "max", null);
            if (truncateToDays) {
                final LocalDateTime parsedMin = parseJsonLocalDateTime(minText);
                final LocalDateTime parsedMax = parseJsonLocalDateTime(maxText);
                setConfiguredLocalDates(parsedMin == null ? null : parsedMin.toLocalDate(),
                        parsedMax == null ? null : parsedMax.toLocalDate());
            } else {
                final ZoneId zone = ZoneId.systemDefault();
                setConfiguredMinMaxDate(parseJsonDate(minText, zone), parseJsonDate(maxText, zone), zone);
            }
            isRelative = JsonUtils.getBoolean(node, "relative", false);
            minDateOffset = JsonUtils.getInt(node, "minOffset", -1);
            maxDateOffset = JsonUtils.getInt(node, "maxOffset", -1);
            ignoreYear = JsonUtils.getBoolean(node, "ignoreYear", false);
        }
    }


    public ObjectNode getJsonConfig() {
        final ObjectNode node = JsonUtils.createObjectNode();
        if (truncateToDays) {
            JsonUtils.setText(node, "min", formatLocalDateTime(minLocalDate, false));
            JsonUtils.setText(node, "max", formatLocalDateTime(maxLocalDate, true));
        } else {
            final ZoneId zone = ZoneId.systemDefault();
            JsonUtils.setText(node, "min", formatLocalDateTime(minDate, zone));
            JsonUtils.setText(node, "max", formatLocalDateTime(maxDate, zone));
        }
        JsonUtils.setBoolean(node, "relative", isRelative);
        JsonUtils.setInt(node, "minOffset", minDateOffset);
        JsonUtils.setInt(node, "maxOffset", maxDateOffset);
        JsonUtils.setBoolean(node, "ignoreYear", ignoreYear);
        return node;
    }


    public boolean isFilled() {
        final EffectiveDateRange range = getEffectiveRange();
        return range.min != null || range.max != null;
    }

    public void addToSql(final SqlBuilder sqlBuilder, final String valueExpression) {
        final EffectiveDateRange range = getEffectiveRange();
        if (valueExpression != null && (range.min != null || range.max != null)) {
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND);

            if (ignoreYear) {
                // SQLite's localtime modifier uses the same device-local calendar date as in-memory matching.
                final String dayOfYearExpression = "strftime('%m-%d', " + valueExpression + "/1000, 'unixepoch', 'localtime')";
                final String minDayOfYear = formatLocalDate(range.minDay, DAY_MONTH_FORMAT);
                final String maxDayOfYear = formatLocalDate(range.maxDay, DAY_MONTH_FORMAT);

                if (minDayOfYear != null && maxDayOfYear != null) {
                    if (minDayOfYear.compareTo(maxDayOfYear) <= 0) {
                        // Normal range (doesn't wrap around year boundary)
                        sqlBuilder.addWhere(dayOfYearExpression + " >= '" + minDayOfYear + "'");
                        sqlBuilder.addWhere(dayOfYearExpression + " <= '" + maxDayOfYear + "'");
                    } else {
                        // Wraps around year boundary (e.g., 11-01 to 02-28)
                        sqlBuilder.openWhere(SqlBuilder.WhereType.OR);
                        sqlBuilder.addWhere(dayOfYearExpression + " >= '" + minDayOfYear + "'");
                        sqlBuilder.addWhere(dayOfYearExpression + " <= '" + maxDayOfYear + "'");
                        sqlBuilder.closeWhere();
                    }
                } else if (minDayOfYear != null) {
                    sqlBuilder.addWhere(dayOfYearExpression + " >= '" + minDayOfYear + "'");
                } else {
                    sqlBuilder.addWhere(dayOfYearExpression + " <= '" + maxDayOfYear + "'");
                }
            } else {
                // Compare the stored epoch value directly with the same inclusive boundaries used by matches().
                if (range.min != null) {
                    sqlBuilder.addWhere(valueExpression + " >= " + range.min.getTime());
                }
                if (range.max != null) {
                    sqlBuilder.addWhere(valueExpression + " <= " + range.max.getTime());
                }
            }
            sqlBuilder.closeWhere();
        } else {
            sqlBuilder.addWhereTrue();
        }
    }

    public String getUserDisplayableConfig() {
        final EffectiveDateRange range = getEffectiveRange();
        final DateTimeFormatter formatter = ignoreYear ? DAY_MONTH_FORMAT : DAY_DATE_FORMAT;
        final String minValueString = formatLocalDate(range.minDay, formatter);
        final String maxValueString = formatLocalDate(range.maxDay, formatter);

        String value = UserDisplayableStringUtils.getUserDisplayableConfig(minValueString, maxValueString);
        if (isRelative) {
            value += "(r)";
        }
        if (ignoreYear) {
            value += "(y)";
        }
        return value;
    }

    private static LocalDate toLocalDate(final Date date, final ZoneId zone) {
        return Instant.ofEpochMilli(date.getTime()).atZone(zone).toLocalDate();
    }

    private static Date toDate(final Instant instant) {
        return Date.from(instant);
    }

    private static String formatLocalDate(final LocalDate date, final DateTimeFormatter formatter) {
        return date == null ? null : formatter.format(date);
    }

    private static String formatLocalDateOrDefault(final LocalDate date, final String defaultValue) {
        return date == null ? defaultValue : formatLocalDate(date, DAY_DATE_FORMAT);
    }

    private static String formatLocalDateTime(final LocalDate date, final boolean endOfDay) {
        if (date == null) {
            return null;
        }
        return JSON_DATE_FORMAT.format(endOfDay ? date.atTime(LocalTime.of(23, 59, 59)) : date.atStartOfDay());
    }

    private static String formatLocalDateTime(final Date date, final ZoneId zone) {
        return date == null ? null : JSON_DATE_FORMAT.format(Instant.ofEpochMilli(date.getTime()).atZone(zone));
    }

    private static final class EffectiveDateRange {
        private final Date min;
        private final Date max;
        private final LocalDate minDay;
        private final LocalDate maxDay;
        private final ZoneId zone;

        private EffectiveDateRange(final Date min, final Date max, final LocalDate minDay, final LocalDate maxDay, final ZoneId zone) {
            this.min = min;
            this.max = max;
            this.minDay = minDay;
            this.maxDay = maxDay;
            this.zone = zone;
        }
    }
}
