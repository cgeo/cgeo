package cgeo.geocaching.network;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.Version;

import org.json.JSONException;
import org.json.JSONObject;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.util.functions.Func1;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StatusUpdater {

    static public class Status {
        final public String message;
        final public String messageId;
        final public String icon;
        final public String url;

        Status(final String message, final String messageId, final String icon, final String url) {
            this.message = message;
            this.messageId = messageId;
            this.icon = icon;
            this.url = url;
        }

        Status(final JSONObject response) {
            message = get(response, "message");
            messageId = get(response, "message_id");
            icon = get(response, "icon");
            url = get(response, "url");
        }

        final static public Status closeoutStatus =
                new Status("", "status_closeout_warning", "attribute_abandonedbuilding", "http://faq.cgeo.org/#7_69");

        final static public Status defaultStatus(final Status upToDate) {
            if (upToDate != null && upToDate.message != null) {
                return upToDate;
            }
            return VERSION.SDK_INT < VERSION_CODES.ECLAIR_MR1 ? closeoutStatus : null;
        }
    }

    final static private Observable<Status> statusObservable =
            Observable.interval(1800, TimeUnit.SECONDS).startWith(-1L).flatMap(new Func1<Long, Observable<Status>>() {
                @Override
                public Observable<Status> call(Long id) {
                    final JSONObject response =
                            Network.requestJSON("http://status.cgeo.org/api/status.json",
                                    new Parameters("version_code", String.valueOf(Version.getVersionCode(CgeoApplication.getInstance())),
                                            "version_name", Version.getVersionName(CgeoApplication.getInstance()),
                                            "locale", Locale.getDefault().toString()));
                    return response != null ? Observable.from(Status.defaultStatus((new Status(response)))) : Observable.<Status>empty();
                }
            }).subscribeOn(Schedulers.io());

    final static public BehaviorSubject<Status> latestStatus = BehaviorSubject.create(Status.defaultStatus(null));

    static {
        statusObservable.subscribe(latestStatus);
    }

    private static String get(final JSONObject json, final String key) {
        try {
            return json.getString(key);
        } catch (final JSONException e) {
            return null;
        }
    }

}
