package cgeo.geocaching.maps;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.LiveMapInfoDialogBuilder;

import android.app.Activity;

/**
 * Singleton holding just the information whether the live map has been shown in this session.
 */
public final class LiveMapHint {

    private static LiveMapHint INSTANCE;

    private LiveMapHint() {
        // singleton
    }

    public static LiveMapHint getInstance() {
        synchronized (LiveMapHint.class) {
            if (INSTANCE == null) {
                INSTANCE = new LiveMapHint();
            }
            return INSTANCE;
        }
    }

    private boolean liveMapHintShownInThisSession = false;

    public void showHint(final Activity activity) {
        if (!liveMapHintShownInThisSession && Settings.getLiveMapHintShowCount() <= 3) {
            liveMapHintShownInThisSession = true;
            LiveMapInfoDialogBuilder.create(activity).show();
        }
    }

}
