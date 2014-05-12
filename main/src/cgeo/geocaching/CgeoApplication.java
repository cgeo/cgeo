package cgeo.geocaching;

import cgeo.geocaching.sensors.DirectionProvider;
import cgeo.geocaching.sensors.GeoDataProvider;
import cgeo.geocaching.sensors.IGeoData;
import cgeo.geocaching.utils.Log;

import rx.Observable;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;

import android.app.Application;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

public class CgeoApplication extends Application {

    private boolean forceRelog = false; // c:geo needs to log into cache providers
    public boolean showLoginToast = true; //login toast shown just once.
    private boolean liveMapHintShownInThisSession = false; // livemap hint has been shown
    private static CgeoApplication instance;
    private Observable<IGeoData> geoDataObservable;
    private Observable<Float> directionObservable;
    private volatile IGeoData currentGeo = null;
    private volatile float currentDirection = 0.0f;

    private static final UncaughtExceptionHandler defaultHandler;

    static {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.e("UncaughtException", ex);
                Throwable exx = ex;
                while (exx.getCause() != null) {
                    exx = exx.getCause();
                }
                if (exx.getClass().equals(OutOfMemoryError.class))
                {
                    try {
                        Log.e("OutOfMemory");
                        android.os.Debug.dumpHprofData("/sdcard/dump.hprof");
                    } catch (IOException e) {
                        Log.e("Error writing dump", e);
                    }
                }
                defaultHandler.uncaughtException(thread, ex);
            }
        });
    }

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

    public synchronized Observable<IGeoData> geoDataObservable() {
        if (geoDataObservable == null) {
            final ConnectableObservable<IGeoData> onDemand = GeoDataProvider.create(this).replay(1);
            onDemand.subscribe(new Action1<IGeoData>() {
                                  @Override
                                  public void call(final IGeoData geoData) {
                                      currentGeo = geoData;
                                  }
                              });
            geoDataObservable = onDemand.refCount();
        }
        return geoDataObservable;
    }

    public synchronized Observable<Float> directionObservable() {
        if (directionObservable == null) {
            final ConnectableObservable<Float> onDemand = DirectionProvider.create(this).replay(1);
            onDemand.subscribe(new Action1<Float>() {
                                  @Override
                                  public void call(final Float direction) {
                                      currentDirection = direction;
                                  }
                              });
            directionObservable = onDemand.refCount();
        }
        return directionObservable;
    }

    public IGeoData currentGeo() {
        return currentGeo != null ? currentGeo : geoDataObservable().toBlockingObservable().first();
    }

    public float currentDirection() {
        return currentDirection;
    }

    public boolean isLiveMapHintShownInThisSession() {
        return liveMapHintShownInThisSession;
    }

    public void setLiveMapHintShownInThisSession() {
        liveMapHintShownInThisSession = true;
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
