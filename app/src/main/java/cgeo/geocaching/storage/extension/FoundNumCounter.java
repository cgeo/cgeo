package cgeo.geocaching.storage.extension;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.LocalizationUtils;
import static cgeo.geocaching.connector.capability.ILogin.UNKNOWN_FINDS;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

/**
 * Offline storage for the connector specific found number counter, mainly used in the NUMBER log template.
 */
public class FoundNumCounter extends DataStore.DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_FOUNDNUM;

    private FoundNumCounter(final DataStore.DBExtension copyFrom) {
        super(copyFrom);
    }

    public String getLastLoginUserName() {
        return getString1();
    }

    @Nullable
    private static FoundNumCounter load(final String serviceName) {
        final DataStore.DBExtension temp = load(type, serviceName);
        return null == temp ? null : new FoundNumCounter(temp);
    }

    private static String getStoredUsernameOrUnknown(final ILogin conn) {
        final FoundNumCounter f = FoundNumCounter.load(conn.getName());
        return f != null && !f.getString1().isEmpty() ? f.getString1() : LocalizationUtils.getString(R.string.user_unknown);
    }

    private static void updateFoundNumAndUserName(final ILogin conn, final int foundNum, final String userName) {
        final String serviceName = conn.getName();

        //avoid recreation of same key/value pair
        final FoundNumCounter f = load(serviceName);
        if (f != null && f.getLong1() == foundNum && (userName.isEmpty() || userName.equals(f.getString1()))) {
            return;
        }

        removeAll(type, serviceName);
        add(type, serviceName, foundNum, 0, 0, 0, !userName.isEmpty() ? userName : f != null ? f.getLastLoginUserName() : "", "", "", "");
    }

    /**
     * Number of caches the user has found in this connector.
     *
     * This is the preferred way to retrieve the correct found number.
     * It checks the connector data first and uses the DB cache if no connection is possible.
     */
    public static int getAndUpdateFoundNum(final ILogin conn) {
        int current = conn.getCachesFound();

        if (current == UNKNOWN_FINDS) { // try getting cached information, if counter is unknown
            final FoundNumCounter f = FoundNumCounter.load(conn.getName());
            if (f != null) {
                current = (int) f.getLong1();
            }
        } else { // if caches count is available from the connector, update the cached data
            FoundNumCounter.updateFoundNumAndUserName(conn, current, conn.getUserName());
        }
        return current;
    }

    /**
     * If connector is not logged in, gather the last known stored username, so that we can always show a username even if the user is offline.
     */
    public static String getNotBlankUserName(final ILogin conn) {
        return StringUtils.isNotBlank(conn.getUserName()) ? conn.getUserName() : getStoredUsernameOrUnknown(conn);
    }
}
