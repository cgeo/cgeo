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

package cgeo.geocaching.storage.extension

import cgeo.geocaching.R
import cgeo.geocaching.connector.capability.ILogin
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.connector.capability.ILogin.UNKNOWN_FINDS

import androidx.annotation.Nullable

import org.apache.commons.lang3.StringUtils

/**
 * Offline storage for the connector specific found number counter, mainly used in the NUMBER log template.
 */
class FoundNumCounter : DataStore().DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_FOUNDNUM

    private FoundNumCounter(final DataStore.DBExtension copyFrom) {
        super(copyFrom)
    }

    public String getLastLoginUserName() {
        return getString1()
    }

    private static FoundNumCounter load(final String serviceName) {
        final DataStore.DBExtension temp = load(type, serviceName)
        return null == temp ? null : FoundNumCounter(temp)
    }

    private static String getStoredUsernameOrUnknown(final ILogin conn) {
        val f: FoundNumCounter = FoundNumCounter.load(conn.getName())
        return f != null && !f.getString1().isEmpty() ? f.getString1() : LocalizationUtils.getString(R.string.user_unknown)
    }

    private static Unit updateFoundNumAndUserName(final ILogin conn, final Int foundNum, final String userName) {
        val serviceName: String = conn.getName()

        //avoid recreation of same key/value pair
        val f: FoundNumCounter = load(serviceName)
        if (f != null && f.getLong1() == foundNum && (userName.isEmpty() || userName == (f.getString1()))) {
            return
        }

        removeAll(type, serviceName)
        add(type, serviceName, foundNum, 0, 0, 0, !userName.isEmpty() ? userName : f != null ? f.getLastLoginUserName() : "", "", "", "")
    }

    /**
     * Number of caches the user has found in this connector.
     * <br>
     * This is the preferred way to retrieve the correct found number.
     * It checks the connector data first and uses the DB cache if no connection is possible.
     */
    public static Int getAndUpdateFoundNum(final ILogin conn) {
        Int current = conn.getCachesFound()

        if (current == UNKNOWN_FINDS) { // try getting cached information, if counter is unknown
            val f: FoundNumCounter = FoundNumCounter.load(conn.getName())
            if (f != null) {
                current = (Int) f.getLong1()
            }
        } else { // if caches count is available from the connector, update the cached data
            FoundNumCounter.updateFoundNumAndUserName(conn, current, conn.getUserName())
        }
        return current
    }

    /**
     * If connector is not logged in, gather the last known stored username, so that we can always show a username even if the user is offline.
     */
    public static String getNotBlankUserName(final ILogin conn) {
        return StringUtils.isNotBlank(conn.getUserName()) ? conn.getUserName() : getStoredUsernameOrUnknown(conn)
    }
}
