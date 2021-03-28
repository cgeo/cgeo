package cgeo.geocaching.brouter;

import cgeo.geocaching.brouter.util.DefaultFilesUtils;
import cgeo.geocaching.utils.Log;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;

import java.util.ArrayList;

public class InternalRoutingService extends Service {

    private final IInternalRoutingService.Stub myBRouterServiceStub = new IInternalRoutingService.Stub() {
        @Override
        public String getTrackFromParams(final Bundle params) {
            final BRouterWorker worker = new BRouterWorker();

            final String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/cgeo/routing";
            worker.segmentDir = baseDir + "/segments4/";

            // should be:
            // final Folder base = PersistableFolder.ROUTING_BASE.getFolder();
            // worker.segmentDir = PersistableFolder.ROUTING_TILES.getFolder();

            final String fast = params.getString("fast");
            final boolean isFast = "1".equals(fast) || "true".equals(fast) || "yes".equals(fast);
            final String mode = params.getString("v");
            final String modeKey = mode + "_" + (isFast ? "fast" : "short");

            // c:geo uses default profile mapping:
            if (mode.equals("motorcar")) {
                worker.profileName = isFast ? "car-fast" : "moped";
            } else if (mode.equals("bicycle")) {
                worker.profileName = isFast ? "fastbike" : "trekking";
            } else if (mode.equals("foot")) {
                worker.profileName = "shortest";
            } else {
                Log.e("no brouter service config found, mode " + modeKey);
                return "no brouter service config found, mode " + modeKey;
            }
            worker.rawTrackPath = baseDir + "/" + modeKey + "_rawtrack.dat";
            worker.nogoList = new ArrayList<>();

            try {
                return worker.getTrackFromParams(params);
            } catch (IllegalArgumentException iae) {
                return iae.getMessage();
            }
        }

    };

    @Override
    public IBinder onBind(final Intent arg0) {
        Log.d(getClass().getSimpleName() + "onBind()");
        DefaultFilesUtils.checkDefaultFiles();
        return myBRouterServiceStub;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(getClass().getSimpleName() + "onCreate()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(getClass().getSimpleName() + "onDestroy()");
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        return START_STICKY;
    }

}
