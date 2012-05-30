package cgeo.geocaching.network;

import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.utils.MemorySubject;
import cgeo.geocaching.utils.PeriodicHandler;
import cgeo.geocaching.utils.Version;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Looper;

import java.util.Locale;

public class StatusUpdater extends MemorySubject<StatusUpdater.Status> implements Runnable {

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
    }

    private void requestUpdate() {
        final JSONObject response =
                Network.requestJSON("http://status.cgeo.org/api/status.json",
                        new Parameters("version_code", "" + Version.getVersionCode(cgeoapplication.getInstance()),
                                "version_name", Version.getVersionName(cgeoapplication.getInstance()),
                                "locale", Locale.getDefault().toString()));
        if (response != null) {
            notifyObservers(new Status(get(response, "message"), get(response, "message_id"), get(response, "icon"), get(response, "url")));
        }
    }

    private static String get(final JSONObject json, final String key) {
        try {
            return json.getString(key);
        } catch (final JSONException e) {
            return null;
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        new PeriodicHandler(1800000L) {
            @Override
            public void act() {
                requestUpdate();
            }
        }.start();
        Looper.loop();
    }

}
