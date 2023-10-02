package cgeo.geocaching.filters.core;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.config.LegacyFilterConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.BooleanUtils;


public class LogEntryGeocacheFilter extends BaseGeocacheFilter {

    private static final String CONFIG_KEY_INVERSE = "inverse";
    private static final String CONFIG_KEY_LOG_TEXT = "logtext";


    private final StringFilter foundByFilter = new StringFilter();
    private boolean inverse = false;
    private final StringFilter logTextFilter = new StringFilter();

    public boolean isInverse() {
        return inverse;
    }

    public void setInverse(final boolean inverse) {
        this.inverse = inverse;
    }

    public String getFoundByUser() {
        return foundByFilter.getTextValue();
    }

    public void setFoundByUser(final String foundByUser) {
        this.foundByFilter.setTextValue(foundByUser);
    }

    public String getLogText() {
        return logTextFilter.getTextValue();
    }

    public void setLogText(final String logText) {
        this.logTextFilter.setTextValue(logText);
    }

    @Nullable
    @Override
    public Boolean filter(final Geocache cache) {
        final String finder = cache.getSearchFinder();
        if (finder != null && !inverse && foundByFilter.matches(finder)) {
            return true;
        }
        final List<LogEntry> logEntries = cache.getLogs();
        if (logEntries.isEmpty() && !cache.inDatabase()) {
            return inverse ? true : null; //if inverse=true then this might be a gc.com search
        }
        for (LogEntry logEntry : logEntries) {
            if (foundByFilter.matches(logEntry.author) && logTextFilter.matches(logEntry.log)) {
                return !inverse;
            }
        }
        return inverse;
    }


    @Override
    public boolean isFiltering() {
        return foundByFilter.isFilled() || logTextFilter.isFilled();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (!isFiltering()) {
            sqlBuilder.addWhereTrue();
            return;
        }

        final String tid = sqlBuilder.getNewTableId();
        final StringBuilder sb = new StringBuilder();
        if (inverse) {
            sb.append("NOT ");
        }
        sb.append("EXISTS( SELECT ").append(tid).append(".geocode FROM cg_logs ").append(tid).append(" WHERE ").append(sqlBuilder.getMainTableId()).append(".geocode = ").append(tid).append(".geocode");
        if (foundByFilter.isFilled()) {
            sb.append(" AND ").append(foundByFilter.getRawLikeSqlExpression("author"));
        }
        if (logTextFilter.isFilled()) {
            sb.append(" AND ").append(logTextFilter.getRawLikeSqlExpression("log"));
        }
        sb.append(")");
        sqlBuilder.addWhere(sb.toString());
    }

    @Override
    public void setConfig(final LegacyFilterConfig config) {
        foundByFilter.setConfig(config.getDefaultList());
        inverse = config.getFirstValue(CONFIG_KEY_INVERSE, false, BooleanUtils::toBoolean);
        logTextFilter.setConfig(config.get(CONFIG_KEY_LOG_TEXT));
    }

    @Override
    public LegacyFilterConfig getConfig() {
        final LegacyFilterConfig config = new LegacyFilterConfig();
        config.putDefaultList(foundByFilter.getConfig());
        config.putList(CONFIG_KEY_INVERSE, Boolean.toString(inverse));
        config.put(CONFIG_KEY_LOG_TEXT, logTextFilter.getConfig());
        return config;
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.set(node, "foundBy", foundByFilter.getJsonConfig());
        JsonUtils.setBoolean(node, CONFIG_KEY_INVERSE, inverse);
        JsonUtils.set(node, "logText", logTextFilter.getJsonConfig());
        return node;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode node) {
        foundByFilter.setJsonConfig(JsonUtils.get(node, "foundBy"));
        inverse = JsonUtils.getBoolean(node, CONFIG_KEY_INVERSE, false);
        logTextFilter.setJsonConfig(JsonUtils.get(node, "logText"));
    }

    @Override
    protected String getUserDisplayableConfig() {
        return (inverse ? "-(" : "") +
                (foundByFilter.isFilled() ? foundByFilter.getTextValue() : "") + ":" +
                (logTextFilter.isFilled() ? logTextFilter.getTextValue() : "") +
                (inverse ? ")" : "");
    }
}
