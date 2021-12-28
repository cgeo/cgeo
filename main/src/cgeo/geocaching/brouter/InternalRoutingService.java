package cgeo.geocaching.brouter;

import cgeo.geocaching.brouter.util.DefaultFilesUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.brouter.BRouterConstants.PROFILE_PARAMTERKEY;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

public class InternalRoutingService extends Service {

    private final IInternalRoutingService.Stub myBRouterServiceStub = new IInternalRoutingService.Stub() {
        @Override
        public String getTrackFromParams(final Bundle params) {
            final BRouterWorker worker = new BRouterWorker();

            worker.profileFilename = params.getString(PROFILE_PARAMTERKEY);
            if (StringUtils.isBlank(worker.profileFilename)) {
                return ""; // cannot calculate a route without a profile
            }

            final String mode = params.getString("v");
            worker.rawTrackPath = getApplicationContext().getFilesDir().getAbsolutePath() + "/routing/";
            FileUtils.mkdirs(new File(worker.rawTrackPath));
            worker.rawTrackPath += mode + "_rawtrack.dat";

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
