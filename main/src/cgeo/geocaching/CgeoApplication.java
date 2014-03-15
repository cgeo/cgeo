package cgeo.geocaching;

import cgeo.geocaching.sensors.DirectionProvider;
import cgeo.geocaching.sensors.GeoDataProvider;
import cgeo.geocaching.sensors.IGeoData;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.tuple.ImmutablePair;
import rx.Observable;
import rx.functions.Func2;

import android.app.Application;

public class CgeoApplication extends Application {

    private boolean forceRelog = false; // c:geo needs to log into cache providers
    public boolean showLoginToast = true; //login toast shown just once.
    private boolean liveMapHintShownInThisSession = false; // livemap hint has been shown
    private static CgeoApplication instance;
    private Observable<ImmutablePair<IGeoData,Float>> geoDir;

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

    public synchronized Observable<ImmutablePair<IGeoData, Float>> geoDirObservable() {
        if (geoDir == null) {
            geoDir = Observable.combineLatest(GeoDataProvider.create(this), DirectionProvider.create(this), new Func2<IGeoData, Float, ImmutablePair<IGeoData, Float>>() {
                @Override
                public ImmutablePair<IGeoData, Float> call(final IGeoData geoData, final Float dir) {
                    return new ImmutablePair<IGeoData, Float>(geoData, dir);
                }
            });
        }
        return geoDir;
    }

    private ImmutablePair<IGeoData, Float> currentGeoDir() {
        return geoDirObservable().first().toBlockingObservable().single();
    }

    public IGeoData currentGeo() {
        return currentGeoDir().left;
    }

    public Float currentDirection() {
        return currentGeoDir().right;
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
