package cgeo.geocaching;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.network.StatusUpdater;
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
    public boolean checkLogin = true; // c:geo is just launched
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
        // Initialize densitiy related waypoint data
        Waypoint.initializeScale();
    }

    @Override
    public void onLowMemory() {
        Log.i("Cleaning applications cache.");
        DataStore.removeAllFromCache();
    }

    @Override
    public void onTerminate() {
        Log.d("Terminating c:geo…");

        DataStore.clean();
        DataStore.closeDb();

        super.onTerminate();
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
                        ActivityMixin.helpDialog(fromActivity, res.getString(R.string.init_dbmove_dbmove), message);
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

}
