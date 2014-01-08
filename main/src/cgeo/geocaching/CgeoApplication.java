package cgeo.geocaching;

import cgeo.geocaching.network.StatusUpdater;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.IObserver;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.res.Resources;

import java.util.concurrent.atomic.AtomicBoolean;

public class CgeoApplication extends Application {

    private volatile GeoDataProvider geo;
    private volatile DirectionProvider dir;
    private boolean forceRelog = false; // c:geo needs to log into cache providers
    public boolean showLoginToast = true; //login toast shown just once.
    private boolean liveMapHintShown = false; // livemap hint has been shown
    final private StatusUpdater statusUpdater = new StatusUpdater();
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
    public void onCreate() {
        new Thread(statusUpdater).start();
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

    /**
     * Register an observer to receive GeoData information.
     * <br/>
     * If there is a chance that no observers are registered before this
     * method is called, it is necessary to call it from a task implementing
     * a looper interface as the data provider will use listeners that
     * require a looper thread to run.
     *
     * @param observer a geodata observer
     */
    public void addGeoObserver(final IObserver<? super IGeoData> observer) {
        currentGeoObject().addObserver(observer);
    }

    public void deleteGeoObserver(final IObserver<? super IGeoData> observer) {
        currentGeoObject().deleteObserver(observer);
    }

    private GeoDataProvider currentGeoObject() {
        if (geo == null) {
            synchronized(this) {
                if (geo == null) {
                    geo = new GeoDataProvider(this);
                }
            }
        }
        return geo;
    }

    public IGeoData currentGeo() {
        return currentGeoObject().getMemory();
    }

    public void addDirectionObserver(final IObserver<? super Float> observer) {
        currentDirObject().addObserver(observer);
    }

    public void deleteDirectionObserver(final IObserver<? super Float> observer) {
        currentDirObject().deleteObserver(observer);
    }

    private DirectionProvider currentDirObject() {
        if (dir == null) {
            synchronized(this) {
                if (dir == null) {
                    dir = new DirectionProvider(this);
                }
            }
        }
        return dir;
    }

    public Float currentDirection() {
        return currentDirObject().getMemory();
    }

    public StatusUpdater getStatusUpdater() {
        return statusUpdater;
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
