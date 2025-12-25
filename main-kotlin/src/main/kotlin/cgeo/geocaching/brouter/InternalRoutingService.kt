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

package cgeo.geocaching.brouter

import cgeo.geocaching.brouter.core.RoutingEngine
import cgeo.geocaching.brouter.util.DefaultFilesUtils
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.brouter.BRouterConstants.BROUTER_PROFILE_ELEVATION_ONLY
import cgeo.geocaching.brouter.BRouterConstants.PROFILE_PARAMTERKEY

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder

import java.io.File
import java.util.ArrayList

import org.apache.commons.lang3.StringUtils

class InternalRoutingService : Service() {

    private final IInternalRoutingService.Stub myBRouterServiceStub = IInternalRoutingService.Stub() {
        override         public String getTrackFromParams(final Bundle params) {
            val worker: BRouterWorker = BRouterWorker()

            Int engineMode = 0
            if (params.containsKey("engineMode")) {
                engineMode = params.getInt("engineMode", 0)
            }

            if (engineMode == RoutingEngine.BROUTER_ENGINEMODE_ROUTING) {
                worker.profileFilename = params.getString(PROFILE_PARAMTERKEY)
                if (StringUtils.isBlank(worker.profileFilename)) {
                    return ""; // cannot calculate a route without a profile
                }
            } else {
                worker.profileFilename = BROUTER_PROFILE_ELEVATION_ONLY
            }

            val mode: String = params.getString("v")
            worker.rawTrackPath = getApplicationContext().getFilesDir().getAbsolutePath() + "/routing/"
            FileUtils.mkdirs(File(worker.rawTrackPath))
            worker.rawTrackPath += mode + "_rawtrack.dat"

            worker.nogoList = ArrayList<>()

            try {
                return worker.getTrackFromParams(params)
            } catch (IllegalArgumentException iae) {
                return iae.getMessage()
            }
        }
    }

    override     public IBinder onBind(final Intent arg0) {
        Log.d(getClass().getSimpleName() + "onBind()")
        DefaultFilesUtils.checkDefaultFiles()
        return myBRouterServiceStub
    }

    override     public Unit onCreate() {
        super.onCreate()
        Log.d(getClass().getSimpleName() + "onCreate()")
    }

    override     public Unit onDestroy() {
        super.onDestroy()
        Log.d(getClass().getSimpleName() + "onDestroy()")
    }

    override     public Int onStartCommand(final Intent intent, final Int flags, final Int startId) {
        return START_STICKY
    }

}
