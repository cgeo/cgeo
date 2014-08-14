package cgeo.geocaching;

import cgeo.geocaching.playservices.LocationProvider;
import cgeo.geocaching.sensors.DirectionProvider;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDataProvider;
import cgeo.geocaching.sensors.GpsStatusProvider;
import cgeo.geocaching.sensors.GpsStatusProvider.Status;
import cgeo.geocaching.sensors.IGeoData;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.OOMDumpingUncaughtExceptionHandler;
import cgeo.geocaching.utils.RxUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import rx.Observable;
import rx.functions.Action1;

import android.app.Application;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;

public class CgeoApplication extends Application {

    private boolean forceRelog = false; // c:geo needs to log into cache providers
    public boolean showLoginToast = true; //login toast shown just once.
    private boolean liveMapHintShownInThisSession = false; // livemap hint has been shown
    private static CgeoApplication instance;
    private Observable<IGeoData> geoDataObservable;
    private Observable<IGeoData> geoDataObservableLowPower;
    private Observable<Float> directionObservable;
    private Observable<Status> gpsStatusObservable;
    private volatile IGeoData currentGeo = GeoData.dummyLocation();
    private volatile float currentDirection = 0.0f;
    private boolean isGooglePlayServicesAvailable = false;
    private final Action1<IGeoData> REMEMBER_GEODATA = new Action1<IGeoData>() {
        @Override
        public void call(final IGeoData geoData) {
            currentGeo = geoData;
        }
    };

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
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException ignore) {
        }
        // ensure initialization of lists
        DataStore.getLists();
        // Check if Google Play services is available
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            isGooglePlayServicesAvailable = true;
        }
        Log.i("Google Play services are " + (isGooglePlayServicesAvailable ? "" : "not ") + "available");
        setupGeoDataObservables(Settings.useGooglePlayServices());
        geoDataObservableLowPower.subscribeOn(RxUtils.looperCallbacksScheduler).first().subscribe(REMEMBER_GEODATA);
        directionObservable = DirectionProvider.create(this).replay(1).refCount().doOnNext(new Action1<Float>() {
            @Override
            public void call(final Float direction) {
                currentDirection = direction;
            }
        });
        gpsStatusObservable = GpsStatusProvider.create(this).share();
    }

    public void setupGeoDataObservables(final boolean useGooglePlayServices) {
        final Action1<IGeoData> rememberGeoData = new Action1<IGeoData>() {
            @Override
            public void call(final IGeoData geoData) {
                currentGeo = geoData;
            }
        };
        if (isGooglePlayServicesAvailable) {
            geoDataObservable = LocationProvider.getMostPrecise(this, true).replay(1).refCount().doOnNext(rememberGeoData);
            geoDataObservableLowPower = LocationProvider.getLowPower(this, true).replay(1).refCount().doOnNext(rememberGeoData);
            LocationProvider.getInitialLocation(this, false).subscribeOn(RxUtils.looperCallbacksScheduler).subscribe(rememberGeoData);
        } else {
            geoDataObservable = GeoDataProvider.create(this).replay(1).refCount().doOnNext(rememberGeoData);
            geoDataObservableLowPower = geoDataObservable;
            geoDataObservable.first().subscribeOn(RxUtils.looperCallbacksScheduler).subscribe(rememberGeoData);
        }
    }

    @Override
    public void onLowMemory() {
        Log.i("Cleaning applications cache.");
        DataStore.removeAllFromCache();
    }

    public Observable<IGeoData> geoDataObservable(final boolean lowPower) {
        return lowPower ? geoDataObservableLowPower : geoDataObservable;
    }

    public Observable<Float> directionObservable() {
        return directionObservable;
    }

    public Observable<Status> gpsStatusObservable() {
        if (gpsStatusObservable == null) {
            gpsStatusObservable = GpsStatusProvider.create(this).share();
        }
        return gpsStatusObservable;
    }

    public IGeoData currentGeo() {
        return currentGeo;
    }

    public Float distanceNonBlocking(final ICoordinates target) {
        if (target.getCoords() == null) {
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

    public boolean isGooglePlayServicesAvailable() {
        return isGooglePlayServicesAvailable;
    }

}
