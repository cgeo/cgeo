package cgeo.geocaching;

import cgeo.geocaching.sensors.DirectionProvider;
import cgeo.geocaching.sensors.GeoDataProvider;
import cgeo.geocaching.sensors.IGeoData;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.OOMDumpingUncaughtExceptionHandler;

import rx.Observable;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;

import android.app.Application;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;

public class CgeoApplication extends Application {

    private boolean forceRelog = false; // c:geo needs to log into cache providers
    public boolean showLoginToast = true; //login toast shown just once.
    private boolean liveMapHintShownInThisSession = false; // livemap hint has been shown
    private static CgeoApplication instance;
    private Observable<IGeoData> geoDataObservable;
    private Observable<Float> directionObservable;
    private volatile IGeoData currentGeo = null;
    private volatile float currentDirection = 0.0f;

    public static void dumpOnOutOfMemory(final boolean enable) {

        if (enable) {

            if (!OOMDumpingUncaughtExceptionHandler.activateHandler()) {
                Log.e("OOM dumping handler not activated (either a problem occured or it was already active)");
            }
        } else {
            if (!OOMDumpingUncaughtExceptionHandler.resetToDefault()) {
                Log.e("OOM dumping handler not resetted (either a problem occured or it was not active)");
            }
        }
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
    public void onCreate() {
        try {
            final ViewConfiguration config = ViewConfiguration.get(this);
            final Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            menuKeyField.setAccessible(true);
            menuKeyField.setBoolean(config, false);
        } catch (final IllegalArgumentException e) {
            // ignore
        } catch (final NoSuchFieldException e) {
            // ignore
        } catch (final IllegalAccessException e) {
            // ignore
        }

        // Set language to English if the user decided so.
        Settings.setLanguage(Settings.isUseEnglish());

        // ensure initialization of lists
        DataStore.getLists();
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
        return currentGeo != null ? currentGeo : geoDataObservable().toBlocking().first();
    }

    public Float distanceNonBlocking(final ICoordinates target) {
        if (currentGeo == null || target.getCoords() == null) {
            return null;
        }
        return currentGeo.getCoords().distanceTo(target);
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
