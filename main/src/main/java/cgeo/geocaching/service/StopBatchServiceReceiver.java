package cgeo.geocaching.service;

import cgeo.geocaching.utils.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Broadcast receiver that cancels any {@link AbstractGeocacheBatchService} subclass.
 * The service class name is passed via {@link AbstractGeocacheBatchService#EXTRA_SERVICE_CLASS}.
 */
public class StopBatchServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String className = intent.getStringExtra(AbstractGeocacheBatchService.EXTRA_SERVICE_CLASS);
        if (className == null) {
            return;
        }
        try {
            final Class<?> clazz = Class.forName(className);
            if (AbstractGeocacheBatchService.class.isAssignableFrom(clazz)) {
                AbstractGeocacheBatchService.requestStop(clazz.asSubclass(AbstractGeocacheBatchService.class));
            }
        } catch (final ClassNotFoundException e) {
            Log.e("StopBatchServiceReceiver: unknown service class: " + className);
        }
    }
}
