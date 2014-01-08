package cgeo.geocaching;

import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Log;

import rx.Observable;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.res.Resources;

import java.util.concurrent.atomic.AtomicBoolean;

public class CgeoApplication extends Application {

    private volatile Observable<IGeoData> geo;
    private volatile Observable<Float> dir;
    private boolean forceRelog = false; // c:geo needs to log into cache providers
    public boolean showLoginToast = true; //login toast shown just once.
    private boolean liveMapHintShown = false; // livemap hint has been shown
    private static CgeoApplication instance;

    public CgeoApplication() {
        setInstance(this);
    }

    private static void setInstance(final CgeoApplication application) {
        instance = application;
    }

    public static CgeoApplication getInstance() {
        return instance;
    }

    @Override
    public void onLowMemory() {
        Log.i("Cleaning applications cache.");
        DataStore.removeAllFromCache();
    }

    /**
     * Move the database to/from external cgdata in a new thread,
     * showing a progress window
     *
     * @param fromActivity
     */
    public void moveDatabase(final Activity fromActivity) {
        final Resources res = this.getResources();
        final ProgressDialog dialog = ProgressDialog.show(fromActivity, res.getString(R.string.init_dbmove_dbmove), res.getString(R.string.init_dbmove_running), true, false);
        final AtomicBoolean atomic = new AtomicBoolean(false);
        new Thread() {
            @Override
            public void run() {
                atomic.set(DataStore.moveDatabase());
                fromActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        boolean success = atomic.get();
                        String message = success ? res.getString(R.string.init_dbmove_success) : res.getString(R.string.init_dbmove_failed);
                        Dialogs.message(fromActivity, R.string.init_dbmove_dbmove, message);
                    }
                });
            }
        }.start();
    }

    public Observable<IGeoData> currentGeoObject() {
        if (geo == null) {
            synchronized(this) {
                if (geo == null) {
                    geo = GeoDataProvider.create(this);
                }
            }
        }
        return geo;
    }

    public IGeoData currentGeo() {
        return currentGeoObject().first().toBlockingObservable().single();
    }

    public Observable<Float> currentDirObject() {
        if (dir == null) {
            synchronized(this) {
                if (dir == null) {
                    dir = DirectionProvider.create(this);
                }
            }
        }
        return dir;
    }

    public Float currentDirection() {
        return currentDirObject().first().toBlockingObservable().single();
    }

    public boolean isLiveMapHintShown() {
        return liveMapHintShown;
    }

    public void setLiveMapHintShown() {
        liveMapHintShown = true;
    }

    /**
     * Check if cgeo must relog even if already logged in.
     *
     * @return <code>true</code> if it is necessary to relog
     */
    public boolean mustRelog() {
        final boolean mustLogin = forceRelog;
        forceRelog = false;
        return mustLogin;
    }

    /**
     * Force cgeo to relog when reaching the main activity.
     */
    public void forceRelog() {
        forceRelog = true;
    }

}
