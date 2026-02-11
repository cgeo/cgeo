package cgeo.geocaching.filters.core;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;

/**
 * Filter for geocaches owned by the current user.
 * This filter considers the geocache's origin and uses the appropriate login name for that platform.
 */
public class SelfOwnedGeocacheFilter extends BaseGeocacheFilter {

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }

        final String ownerName = getOwnerNameForOrigin(cache.getGeocode());
        if (StringUtils.isEmpty(ownerName)) {
            return null;
        }

        final String cacheOwner = cache.getOwnerDisplayName();
        if (StringUtils.isEmpty(cacheOwner)) {
            return null;
        }

        return StringUtils.equalsIgnoreCase(cacheOwner, ownerName);
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        // Build an SQL OR clause for all active connectors with valid credentials
        final List<String> conditions = new ArrayList<>();
        final List<String> params = new ArrayList<>();

        for (final IConnector connector : ConnectorFactory.getConnectors()) {
            final String ownerName = getOwnerNameForConnector(connector);
            if (StringUtils.isEmpty(ownerName)) {
                continue;
            }

            // Get the geocode patterns for this connector (e.g., "GC%" for geocaching.com)
            final String[] geocodePatterns = connector.getGeocodeSqlLikeExpressions();
            if (geocodePatterns == null || geocodePatterns.length == 0) {
                continue;
            }

            // For each connector, create: (geocode LIKE 'pattern%' AND owner = 'username')
            for (final String pattern : geocodePatterns) {
                conditions.add("(" + sqlBuilder.getMainTableId() + ".geocode LIKE ? AND " + 
                              sqlBuilder.getMainTableId() + ".owner = ?)");
                params.add(pattern);
                params.add(ownerName);
            }
        }

        if (!conditions.isEmpty()) {
            sqlBuilder.openWhere(SqlBuilder.WhereType.OR);
            for (int i = 0; i < conditions.size(); i++) {
                sqlBuilder.addWhere(conditions.get(i), params.get(i * 2), params.get(i * 2 + 1));
            }
            sqlBuilder.closeWhere();
        } else {
            // If no conditions were added, add a true condition to ensure filter doesn't break OR statements
            sqlBuilder.addWhereTrue();
        }
    }

    @Override
    public boolean isFiltering() {
        // This filter is always active if at least one connector has valid credentials
        for (final IConnector connector : ConnectorFactory.getConnectors()) {
            if (StringUtils.isNotEmpty(getOwnerNameForConnector(connector))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the owner name for a geocache based on its origin (connector).
     *
     * @param geocode the geocode of the cache
     * @return the username of the current user on that platform, or null if not available
     */
    @Nullable
    public static String getOwnerNameForOrigin(@Nullable final String geocode) {
        if (StringUtils.isEmpty(geocode)) {
            return null;
        }

        final IConnector connector = ConnectorFactory.getConnector(geocode);
        return getOwnerNameForConnector(connector);
    }

    /**
     * Get the owner name for a specific connector.
     *
     * @param connector the connector
     * @return the username of the current user on that platform, or null if not available
     */
    @Nullable
    public static String getOwnerNameForConnector(@Nullable final IConnector connector) {
        if (connector == null) {
            return null;
        }

        // For connectors that support login (ILogin), use getUserName()
        if (connector instanceof ILogin) {
            final String userName = ((ILogin) connector).getUserName();
            if (StringUtils.isNotEmpty(userName)) {
                return userName;
            }
        }

        // For connectors with credentials (ICredentials), get username from settings
        if (connector instanceof ICredentials) {
            final String userName = Settings.getCredentials((ICredentials) connector).getUsernameRaw();
            if (StringUtils.isNotEmpty(userName)) {
                return userName;
            }
        }

        return null;
    }

    @NonNull
    @Override
    protected String getUserDisplayableConfig() {
        return "";
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        // This filter has no configuration
        return null;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode config) {
        // This filter has no configuration
    }
}
