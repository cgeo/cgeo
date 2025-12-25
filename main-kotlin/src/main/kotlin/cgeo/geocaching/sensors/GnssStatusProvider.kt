// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.sensors

import cgeo.geocaching.permission.PermissionContext
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Log

import android.content.Context
import android.location.GnssStatus
import android.location.GnssStatus.Callback
import android.location.LocationManager

import io.reactivex.rxjava3.core.Observable

class GnssStatusProvider {

    private static val NO_GNSS: Status = Status(false, 0, 0)

    private GnssStatusProvider() {
        // Utility class, not to be instantiated
    }

    public static class Status {
        public final Boolean gnssEnabled
        public final Int satellitesVisible
        public final Int satellitesFixed

        public Status(final Boolean gnssEnabled, final Int satellitesVisible, final Int satellitesFixed) {
            this.gnssEnabled = gnssEnabled
            this.satellitesVisible = satellitesVisible
            this.satellitesFixed = satellitesFixed
        }
    }

    private static Observable<Status> createGNSSObservable(final Context context) {
        return Observable.create(emitter -> {
            val geoManager: LocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE)
            val callback: Callback = Callback() {
                Status latest = NO_GNSS

                override                 public Unit onSatelliteStatusChanged(final GnssStatus status) {
                    val visible: Int = status.getSatelliteCount()
                    Int fixed = 0
                    for (Int satelliteIndex = 0; satelliteIndex < visible; satelliteIndex++) {
                        if (status.usedInFix(satelliteIndex)) {
                            fixed++
                        }
                    }
                    if (visible == latest.satellitesVisible && fixed == latest.satellitesFixed) {
                        return
                    }
                    latest = Status(true, visible, fixed)
                    emitter.onNext(latest)
                }

                override                 public Unit onStarted() {
                    latest = Status(true, 0, 0)
                    emitter.onNext(latest)
                }

                override                 public Unit onStopped() {
                    latest = NO_GNSS
                    emitter.onNext(latest)
                }
            }
            emitter.onNext(NO_GNSS)
            if (PermissionContext.LOCATION.hasAllPermissions()) {
                Log.d("GnssStatusProvider.createGNSSObservable: registering callback")
                try {
                    geoManager.registerGnssStatusCallback(callback)
                } catch (SecurityException ignore) {
                    Log.d("GnssStatusProvider.createGNSSObservable: Could not register provider, no Location permission available")
                }
            } else {
                Log.d("GnssStatusProvider.createGNSSObservable: Could not register provider, no Location permission available")
            }
            emitter.setDisposable(AndroidRxUtils.disposeOnCallbacksScheduler(() -> geoManager.unregisterGnssStatusCallback(callback)))
        })
    }

    public static Observable<Status> create(final Context context) {
        final Observable<Status> observable
        observable = createGNSSObservable(context)
        return observable.subscribeOn(AndroidRxUtils.looperCallbacksScheduler)
    }
}
