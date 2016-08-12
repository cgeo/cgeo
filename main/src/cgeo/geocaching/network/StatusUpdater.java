package cgeo.geocaching.network;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Version;

import android.app.Application;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

public class StatusUpdater {

    private StatusUpdater() {
        // Utility class
    }

    public static class Status {
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

        public static final Status CLOSEOUT_STATUS =
                new Status("", "status_closeout_warning", "attribute_abandonedbuilding", "http://faq.cgeo.org/#legacy");

        public static final Status defaultStatus(final Status upToDate) {
            if (upToDate != null && upToDate.message != null) {
                return upToDate;
            }
            return VERSION.SDK_INT < VERSION_CODES.ECLAIR_MR1 ? CLOSEOUT_STATUS : null;
        }
    }

    public static final BehaviorSubject<Status> LATEST_STATUS = BehaviorSubject.create(Status.defaultStatus(null));

    static {
        AndroidRxUtils.networkScheduler.createWorker().schedulePeriodically(new Action0() {
            @Override
            public void call() {
                final Application app = CgeoApplication.getInstance();
                final String installer = Version.getPackageInstaller(app);
                final Parameters installerParameters = StringUtils.isNotBlank(installer) ? new Parameters("installer", installer) : null;
                Network.requestJSON("https://cgeo-status.herokuapp.com/api/status.json",
                        Parameters.merge(new Parameters("version_code", String.valueOf(Version.getVersionCode(app)),
                                "version_name", Version.getVersionName(app),
                                "locale", Locale.getDefault().toString()), installerParameters))
                        .subscribe(new Action1<ObjectNode>() {
                            @Override
                            public void call(final ObjectNode json) {
                                LATEST_STATUS.onNext(Status.defaultStatus(new Status(json)));
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(final Throwable throwable) {
                                // Error has already been signalled during the request
                            }
                        });
            }
        }, 0, 1800, TimeUnit.SECONDS);
    }

}
