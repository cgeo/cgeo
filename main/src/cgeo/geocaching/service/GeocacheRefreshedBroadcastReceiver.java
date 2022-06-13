package cgeo.geocaching.service;

import static cgeo.geocaching.Intents.ACTION_GEOCACHE_REFRESHED;
import static cgeo.geocaching.Intents.EXTRA_GEOCODE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public abstract class GeocacheRefreshedBroadcastReceiver extends BroadcastReceiver implements DefaultLifecycleObserver {

    private final Context applicationContext;

    public GeocacheRefreshedBroadcastReceiver(final Context context) {
        applicationContext = context.getApplicationContext();
    }

    protected abstract void onReceive(final Context context, final String geocode);

    @Override
    public void onReceive(Context context, Intent intent) {
        final String geocode = intent.getStringExtra(EXTRA_GEOCODE);
        onReceive(context, geocode);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(this, new IntentFilter(ACTION_GEOCACHE_REFRESHED));
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(this);
    }

    public static void sendBroadcast(final Context context, final String geocode) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_GEOCACHE_REFRESHED).putExtra(EXTRA_GEOCODE, geocode));
    }
}
