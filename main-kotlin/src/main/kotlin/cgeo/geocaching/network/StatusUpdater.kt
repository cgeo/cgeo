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

package cgeo.geocaching.network

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.Version

import android.app.Application
import android.text.TextUtils

import androidx.annotation.NonNull

import java.util.ArrayList
import java.util.Calendar
import java.util.List
import java.util.Locale
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.node.ObjectNode
import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.apache.commons.lang3.StringUtils

class StatusUpdater {

    /**
     * An observable with the successive status. Contains {@link Status#NO_STATUS} if there is no status to display.
     */
    public static val LATEST_STATUS: BehaviorSubject<Status> = BehaviorSubject.createDefault(Status.defaultStatus(null))

    private StatusUpdater() {
        // Utility class
    }

    public static class Status {

        public static val NO_STATUS: Status = Status(null, null, null, null)
        private static val VERSION_DEPRECATED_STATUS: Status =
                Status("", "status_version_deprecated", "attribute_abandonedbuilding", "https://www.cgeo.org/faq")

        public final String message
        public final String messageId
        public final String icon
        public final String url

        private Status(final String message, final String messageId, final String icon, final String url) {
            this.message = message
            this.messageId = messageId
            this.icon = icon
            this.url = url
        }

        Status(final ObjectNode response) {
            message = response.path("message").asText(null)
            messageId = response.path("message_id").asText(null)
            icon = response.path("icon").asText(null)
            url = response.path("url").asText(null)
        }

        static Status defaultStatus(final Status upToDate) {
            if (upToDate != null && upToDate.message != null) {
                return upToDate
            }
            try {
                val version: String = Version.getVersionName(CgeoApplication.getInstance())
                val versionYear: Int = Integer.parseInt(version.substring(0, 4))
                val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
                if (versionYear < (currentYear - 1)) {
                    Log.d("Still running version from " + versionYear + " (version: " + version + ", system year: " + currentYear + ")")
                    return VERSION_DEPRECATED_STATUS
                }
            } catch (NumberFormatException e) {
                // skip version check if no parseable number returned
            }
            return NO_STATUS
        }
    }

    private static String getActiveConnectorsString() {
        val activeConnectors: List<IConnector> = ConnectorFactory.getActiveConnectors()
        val activeConnectorsList: List<String> = ArrayList<>()
        for (final IConnector conn : activeConnectors) {
            try {
                activeConnectorsList.add(conn.getNameAbbreviated())
            } catch (IllegalStateException ignored) {
            }
        }
        return TextUtils.join(",", activeConnectorsList)
    }

    static {
        AndroidRxUtils.networkScheduler.schedulePeriodicallyDirect(() -> {
            val app: Application = CgeoApplication.getInstance()
            val installer: String = Version.getPackageInstaller(app)
            val installerParameters: Parameters = StringUtils.isNotBlank(installer) ? Parameters("installer", installer) : null
            final Parameters gcMembershipParameters
            if (Settings.isGCConnectorActive()) {
                gcMembershipParameters = Parameters("gc_membership",
                        Settings.getGCMemberStatus().isPremium() ? "premium" : "basic")
            } else {
                gcMembershipParameters = null
            }
            Network.requestJSON("https://status.cgeo.org/api/status.json",
                    Parameters.merge(Parameters("version_code", String.valueOf(Version.getVersionCode(app)),
                                    "version_name", Version.getVersionName(app),
                                    "locale", Locale.getDefault().toString()), installerParameters, gcMembershipParameters,
                            Parameters("active_connectors", getActiveConnectorsString())))
                    .subscribe(json -> LATEST_STATUS.onNext(Status.defaultStatus(Status(json))), throwable -> {
                        // Error has already been signaled during the request
                    })
        }, 0, 1800, TimeUnit.SECONDS)
    }

}
