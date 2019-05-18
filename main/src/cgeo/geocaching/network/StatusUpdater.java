package cgeo.geocaching.network;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.gc.GCMemberState;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Version;

import android.app.Application;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;
import org.apache.commons.lang3.StringUtils;

public class StatusUpdater {

    /**
     * An observable with the successive status. Contains {@link Status#NO_STATUS} if there is no status to display.
     */
    public static final BehaviorSubject<Status> LATEST_STATUS = BehaviorSubject.createDefault(Status.defaultStatus(null));

    private StatusUpdater() {
        // Utility class
    }

    public static class Status {

        public static final Status NO_STATUS = new Status(null, null, null, null);
        static final Status CLOSEOUT_STATUS =
            new Status("", "status_closeout_warning_41", "attribute_abandonedbuilding", "https://www.cgeo.org/faq#legacy");

        public final String message;
        public final String messageId;
        public final String icon;
        public final String url;

        private Status(final String message, final String messageId, final String icon, final String url) {
            this.message = message;
            this.messageId = messageId;
            this.icon = icon;
            this.url = url;
        }

        Status(final ObjectNode response) {
            message = response.path("message").asText(null);
            messageId = response.path("message_id").asText(null);
            icon = response.path("icon").asText(null);
            url = response.path("url").asText(null);
        }

        @NonNull
        static Status defaultStatus(final Status upToDate) {
            if (upToDate != null && upToDate.message != null) {
                return upToDate;
            }
            return NO_STATUS;
        }
    }

    private static String getActiveConnectorsString() {
        final List<IConnector> activeConnectors = ConnectorFactory.getActiveConnectors();
        final List<String> activeConnectorsList = new ArrayList<>();
        for (final IConnector conn : activeConnectors) {
            try {
                activeConnectorsList.add(conn.getNameAbbreviated());
            } catch (IllegalStateException ignored) {
            }
        }
        return TextUtils.join(",", activeConnectorsList);
    }

    static {
        AndroidRxUtils.networkScheduler.schedulePeriodicallyDirect(new Runnable() {
            @Override
            public void run() {
                final Application app = CgeoApplication.getInstance();
                final String installer = Version.getPackageInstaller(app);
                final Parameters installerParameters = StringUtils.isNotBlank(installer) ? new Parameters("installer", installer) : null;
                final Parameters gcMembershipParameters;
                if (Settings.isGCConnectorActive()) {
                    final GCMemberState memberState = Settings.getGCMemberStatus();
                    gcMembershipParameters = new Parameters("gc_membership",
                            memberState == GCMemberState.PREMIUM || memberState == GCMemberState.CHARTER ? "premium" : "basic");
                } else {
                    gcMembershipParameters = null;
                }
                Network.requestJSON("https://status.cgeo.org/api/status.json",
                        Parameters.merge(new Parameters("version_code", String.valueOf(Version.getVersionCode(app)),
                                "version_name", Version.getVersionName(app),
                                "locale", Locale.getDefault().toString()), installerParameters, gcMembershipParameters,
                                new Parameters("active_connectors", getActiveConnectorsString())))
                        .subscribe(new Consumer<ObjectNode>() {
                            @Override
                            public void accept(final ObjectNode json) {
                                LATEST_STATUS.onNext(Status.defaultStatus(new Status(json)));
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(final Throwable throwable) {
                                // Error has already been signaled during the request
                            }
                        });
            }
        }, 0, 1800, TimeUnit.SECONDS);
    }

}
