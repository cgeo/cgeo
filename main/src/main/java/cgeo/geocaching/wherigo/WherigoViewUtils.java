package cgeo.geocaching.wherigo;

import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.app.Dialog;
import android.os.Looper;

public final class WherigoViewUtils {

    private WherigoViewUtils() {
        //no instances of Utility classes
    }

    public static void safeDismissDialog(final Dialog dialog) {
        if (dialog == null) {
            return;
        }
        try {
            dialog.dismiss();
        } catch (Exception ex) {
            Log.w("Exception when dismissing dialog", ex);
        }
    }

    public static void ensureRunOnUi(final Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            AndroidRxUtils.runOnUi(r);
        }
    }

}
