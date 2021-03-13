package cgeo.geocaching.filters.core;

import cgeo.geocaching.storage.SqlBuilder;

import java.util.regex.Pattern;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;


public class StringFilter {

    public enum StringFilterType { CONTAINS, STARTS_WITH, ENDS_WITH, PATTERN }

    private StringFilterType filterType;
    private String textValue;
    private boolean matchCase;

    public boolean isFilled() {
        return !StringUtils.isBlank(textValue);
    }

    public StringFilterType getFilterType() {
        return filterType;
    }

    public void setFilterType(final StringFilterType filterType) {
        this.filterType = filterType;
    }

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(final String textValue) {
        this.textValue = textValue;
    }

    public boolean isMatchCase() {
        return matchCase;
    }

    public void setMatchCase(final boolean matchCase) {
        this.matchCase = matchCase;
    }

    public void setFromConfig(final String[] config, final int startPos) {
        if (config != null && config.length > startPos) {
            setTextValue(config[startPos]);
            setFilterType(config.length > 1 ? EnumUtils.getEnum(StringFilterType.class,  config[startPos + 1], StringFilterType.CONTAINS) : StringFilterType.CONTAINS);
            setMatchCase(config.length > 2 && BooleanUtils.toBoolean(config[startPos + 2]));
        }
    }

    public String[] addToConfig(final String[] config, final int startPos) {
        if (config == null || config.length < startPos + 3) {
            return config;
        }
        config[startPos] = textValue;
        config[startPos + 1] = filterType.name();
        config[startPos + 2] = String.valueOf(matchCase);
        return config;
    }

    public boolean matches(final String value) {
        if (!isFilled()) {
            return true;
        }
        final String matchGcValue = this.matchCase ? value : StringUtils.lowerCase(value);
        final String matchTextValue = this.matchCase ? this.textValue : StringUtils.lowerCase(this.textValue);
        switch (this.filterType) {
            case CONTAINS:
                return matchGcValue.contains(matchTextValue);
            case STARTS_WITH:
                return matchGcValue.startsWith(matchTextValue);
            case ENDS_WITH:
                return matchGcValue.endsWith(matchTextValue);
            case PATTERN:
            default:
                return Pattern.compile(matchTextValue.replace('?', '.').replaceAll("\\*", ".*")).matcher(matchGcValue).matches();
        }
    }

    public String toSqlExpression(final String columnExpression) {
        if (!isFilled()) {
            return "1=1";
        }

        String matchTextValue = SqlBuilder.escape(this.matchCase ? this.textValue : StringUtils.lowerCase(this.textValue), true);
        final StringBuilder sb = new StringBuilder();
        if (!this.matchCase) {
            sb.append("LOWER(");
        }
        sb.append(columnExpression);
        if (!this.matchCase) {
            sb.append(")");
        }

        switch (this.filterType) {
            case CONTAINS:
                matchTextValue = "%" + matchTextValue + "%";
                break;
            case STARTS_WITH:
                matchTextValue = matchTextValue + "%";
                break;
            case ENDS_WITH:
                matchTextValue = "%" + matchTextValue;
                break;
            case PATTERN:
            default:
                matchTextValue = matchTextValue.replace('*', '%').replace('?', '_');
                break;
        }
        sb.append(SqlBuilder.createLikeExpression(matchTextValue));
        return sb.toString();
    }

}
