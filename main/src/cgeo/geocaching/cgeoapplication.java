package cgeo.geocaching;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.network.StatusUpdater;
import cgeo.geocaching.utils.IObserver;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;

import java.util.concurrent.atomic.AtomicBoolean;

public class cgeoapplication extends Application {

    private volatile GeoDataProvider geo;
    private volatile DirectionProvider dir;
    public boolean firstRun = true; // c:geo is just launched
    public boolean showLoginToast = true; //login toast shown just once.
    private boolean liveMapHintShown = false; // livemap hint has been shown
    final private StatusUpdater statusUpdater = new StatusUpdater();
    private static cgeoapplication instance;

    public cgeoapplication() {
        instance = this;
    }

    public static cgeoapplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        new Thread(statusUpdater).start();
    }

    @Override
    public void onLowMemory() {
        Log.i("Cleaning applications cache.");
        cgData.removeAllFromCache();
    }

    @Override
    public void onTerminate() {
        Log.d("Terminating c:geo…");

        cgData.clean();
        cgData.closeDb();

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
        Thread moveThread = new Thread() {
            final Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    dialog.dismiss();
                    boolean success = atomic.get();
                    String message = success ? res.getString(R.string.init_dbmove_success) : res.getString(R.string.init_dbmove_failed);
                    ActivityMixin.helpDialog(fromActivity, res.getString(R.string.init_dbmove_dbmove), message);
                }
            };

            @Override
            public void run() {
                atomic.set(cgData.moveDatabase());
                handler.sendMessage(handler.obtainMessage());
            }
        };
        moveThread.start();
    }

    /**
     * restore the database in a new thread, showing a progress window
     *
     * @param fromActivity
     *            calling activity
     */
    public void restoreDatabase(final Activity fromActivity) {
        final Resources res = this.getResources();
        final ProgressDialog dialog = ProgressDialog.show(fromActivity, res.getString(R.string.init_backup_restore), res.getString(R.string.init_restore_running), true, false);
        final AtomicBoolean atomic = new AtomicBoolean(false);
        Thread restoreThread = new Thread() {
            final Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    dialog.dismiss();
                    boolean restored = atomic.get();
                    String message = restored ? res.getString(R.string.init_restore_success) : res.getString(R.string.init_restore_failed);
                    ActivityMixin.helpDialog(fromActivity, res.getString(R.string.init_backup_restore), message);
                    if (fromActivity instanceof cgeo) {
                        ((cgeo) fromActivity).updateCacheCounter();
                    }
                }
            };

            @Override
            public void run() {
                atomic.set(cgData.restoreDatabase());
                handler.sendMessage(handler.obtainMessage());
            }
        };
        restoreThread.start();
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
