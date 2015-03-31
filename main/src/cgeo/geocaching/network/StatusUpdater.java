package cgeo.geocaching.network;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.RxUtils;
import cgeo.geocaching.utils.Version;

import com.fasterxml.jackson.databind.node.ObjectNode;

import rx.functions.Action0;
import rx.subjects.BehaviorSubject;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StatusUpdater {

    private StatusUpdater() {
        // Utility class
    }

    static public class Status {
        final public String message;
        final public String messageId;
        final public String icon;
        final public String url;

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

        final static public Status CLOSEOUT_STATUS =
                new Status("", "status_closeout_warning", "attribute_abandonedbuilding", "http://faq.cgeo.org/#7_69");

        final static public Status defaultStatus(final Status upToDate) {
            if (upToDate != null && upToDate.message != null) {
                return upToDate;
            }
            return VERSION.SDK_INT < VERSION_CODES.ECLAIR_MR1 ? CLOSEOUT_STATUS : null;
        }
    }

    final static public BehaviorSubject<Status> LATEST_STATUS = BehaviorSubject.create(Status.defaultStatus(null));

    static {
        RxUtils.networkScheduler.createWorker().schedulePeriodically(new Action0() {
            @Override
            public void call() {
                final ObjectNode response =
                        Network.requestJSON("http://status.cgeo.org/api/status.json",
                                new Parameters("version_code", String.valueOf(Version.getVersionCode(CgeoApplication.getInstance())),
                                        "version_name", Version.getVersionName(CgeoApplication.getInstance()),
                                        "locale", Locale.getDefault().toString()));
                if (response != null) {
                    LATEST_STATUS.onNext(Status.defaultStatus(new Status(response)));
                }
            }
        }, 0, 1800, TimeUnit.SECONDS);
    }

}
