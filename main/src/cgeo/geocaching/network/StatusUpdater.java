package cgeo.geocaching.network;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Version;

import android.app.Application;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;

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
                new Status("", "status_closeout_warning", "attribute_abandonedbuilding", "http://faq.cgeo.org/#legacy");

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
            return VERSION.SDK_INT < VERSION_CODES.ICE_CREAM_SANDWICH ? CLOSEOUT_STATUS : NO_STATUS;
        }
    }

    static {
        AndroidRxUtils.networkScheduler.schedulePeriodicallyDirect(new Runnable() {
            @Override
            public void run() {
                final Application app = CgeoApplication.getInstance();
                final String installer = Version.getPackageInstaller(app);
                final Parameters installerParameters = StringUtils.isNotBlank(installer) ? new Parameters("installer", installer) : null;
                Network.requestJSON("https://status.cgeo.org/api/status.json",
                        Parameters.merge(new Parameters("version_code", String.valueOf(Version.getVersionCode(app)),
                                "version_name", Version.getVersionName(app),
                                "locale", Locale.getDefault().toString()), installerParameters))
                        .subscribe(new Consumer<ObjectNode>() {
                            @Override
                            public void accept(final ObjectNode json) {
                                LATEST_STATUS.onNext(Status.defaultStatus(new Status(json)));
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(final Throwable throwable) {
                                // Error has already been signalled during the request
                            }
                        });
            }
        }, 0, 1800, TimeUnit.SECONDS);
    }

}
