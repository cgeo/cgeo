package cgeo.geocaching.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public abstract class LifecycleAwareBroadcastReceiver extends BroadcastReceiver implements DefaultLifecycleObserver {

    @NonNull
    private final Context applicationContext;
    @NonNull
    private final IntentFilter filter;

    public LifecycleAwareBroadcastReceiver(@NonNull final Context context, @NonNull final IntentFilter filter) {
        applicationContext = context.getApplicationContext();
        this.filter = filter;
    }

    public LifecycleAwareBroadcastReceiver(@NonNull final Context context, @NonNull final String action) {
        applicationContext = context.getApplicationContext();
        filter = new IntentFilter(action);
    }

    @Override
    public void onCreate(@NonNull final LifecycleOwner owner) {
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(this, filter);
    }

    @Override
    public void onDestroy(@NonNull final LifecycleOwner owner) {
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(this);
    }

    public static void sendBroadcast(@NonNull final Context context, @NonNull final String action) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(action));
    }

    public static void sendBroadcast(@NonNull final Context context, @NonNull final String action, @NonNull final String payloadId, @NonNull final String payload) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(action).putExtra(payloadId, payload));
    }

}
