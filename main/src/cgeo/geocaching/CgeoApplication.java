package cgeo.geocaching;

import cgeo.geocaching.playservices.LocationProvider;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDataProvider;
import cgeo.geocaching.sensors.GpsStatusProvider;
import cgeo.geocaching.sensors.GpsStatusProvider.Status;
import cgeo.geocaching.sensors.OrientationProvider;
import cgeo.geocaching.sensors.RotationProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.OOMDumpingUncaughtExceptionHandler;
import cgeo.geocaching.utils.RxUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.eclipse.jdt.annotation.NonNull;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import android.app.Application;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;

public class CgeoApplication extends Application {

    private boolean forceRelog = false; // c:geo needs to log into cache providers
    public boolean showLoginToast = true; //login toast shown just once.
    private boolean liveMapHintShownInThisSession = false; // livemap hint has been shown
    private static CgeoApplication instance;
    private Observable<GeoData> geoDataObservable;
    private Observable<GeoData> geoDataObservableLowPower;
    private Observable<Float> directionObservable;
    private Observable<Status> gpsStatusObservable;
    @NonNull private volatile GeoData currentGeo = GeoData.DUMMY_LOCATION;
    private volatile boolean hasValidLocation = false;
    private volatile float currentDirection = 0.0f;
    private boolean isGooglePlayServicesAvailable = false;
    private final Action1<GeoData> rememberGeodataAction = new Action1<GeoData>() {
        @Override
        public void call(final GeoData geoData) {
            currentGeo = geoData;
            hasValidLocation = true;
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
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException ignored) {
        }

        // Set language to English if the user decided so.
        Settings.setLanguage(Settings.isUseEnglish());

        // ensure initialization of lists
        DataStore.getLists();

        // Check if Google Play services is available
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            isGooglePlayServicesAvailable = true;
        }
        Log.i("Google Play services are " + (isGooglePlayServicesAvailable ? "" : "not ") + "available");
        setupGeoDataObservables(Settings.useGooglePlayServices(), Settings.useLowPowerMode());
        setupDirectionObservable(Settings.useLowPowerMode());
        gpsStatusObservable = GpsStatusProvider.create(this).replay(1).refCount();

        // Attempt to acquire an initial location before any real activity happens.
        geoDataObservableLowPower.subscribeOn(RxUtils.looperCallbacksScheduler).first().subscribe();
    }

    public void setupGeoDataObservables(final boolean useGooglePlayServices, final boolean useLowPowerLocation) {
        if (useGooglePlayServices) {
            geoDataObservable = LocationProvider.getMostPrecise(this).doOnNext(rememberGeodataAction);
            if (useLowPowerLocation) {
                geoDataObservableLowPower = LocationProvider.getLowPower(this).doOnNext(rememberGeodataAction);
            } else {
                geoDataObservableLowPower = geoDataObservable;
            }
        } else {
            geoDataObservable = RxUtils.rememberLast(GeoDataProvider.create(this).doOnNext(rememberGeodataAction));
            geoDataObservableLowPower = geoDataObservable;
        }
    }

    public void setupDirectionObservable(final boolean useLowPower) {
        directionObservable = RotationProvider.create(this, useLowPower).onErrorResumeNext(new Func1<Throwable, Observable<? extends Float>>() {
            @Override
            public Observable<? extends Float> call(final Throwable throwable) {
                return OrientationProvider.create(CgeoApplication.this);
            }
        }).onErrorResumeNext(new Func1<Throwable, Observable<? extends Float>>() {
            @Override
            public Observable<? extends Float> call(final Throwable throwable) {
                Log.e("Device orientation will not be available as no suitable sensors were found");
                return Observable.<Float>never().startWith(0.0f);
            }
        }).doOnNext(new Action1<Float>() {
            @Override
            public void call(final Float direction) {
                currentDirection = direction;
            }
        }).replay(1).refCount();
    }

    @Override
    public void onLowMemory() {
        Log.i("Cleaning applications cache.");
        DataStore.removeAllFromCache();
    }

    public Observable<GeoData> geoDataObservable(final boolean lowPower) {
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

    @NonNull
    public GeoData currentGeo() {
        return currentGeo;
    }

    public boolean hasValidLocation() {
        return hasValidLocation;
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
