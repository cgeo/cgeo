package cgeo.geocaching.service;

import static cgeo.geocaching.Intents.ACTION_GEOCACHE_CHANGED;
import static cgeo.geocaching.Intents.EXTRA_GEOCODE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public abstract class GeocacheChangedBroadcastReceiver extends BroadcastReceiver implements DefaultLifecycleObserver {

    private final Context applicationContext;
    private final boolean receiveEventsWhileBeingStopped;

    public GeocacheChangedBroadcastReceiver(final Context context) {
        this(context, false);
    }

    public GeocacheChangedBroadcastReceiver(final Context context, final boolean receiveEventsWhileBeingStopped) {
        applicationContext = context.getApplicationContext();
        this.receiveEventsWhileBeingStopped = receiveEventsWhileBeingStopped;
    }

    protected abstract void onReceive(Context context, String geocode);

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String geocode = intent.getStringExtra(EXTRA_GEOCODE);
        onReceive(context, geocode);
    }

    @Override
    public void onStart(@NonNull final LifecycleOwner owner) {
        if (!receiveEventsWhileBeingStopped) {
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(this, new IntentFilter(ACTION_GEOCACHE_CHANGED));
        }
    }

    @Override
    public void onStop(@NonNull final LifecycleOwner owner) {
        if (!receiveEventsWhileBeingStopped) {
            LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(this);
        }
    }

    @Override
    public void onCreate(@NonNull final LifecycleOwner owner) {
        if (receiveEventsWhileBeingStopped) {
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(this, new IntentFilter(ACTION_GEOCACHE_CHANGED));
        }
    }

    @Override
    public void onDestroy(@NonNull final LifecycleOwner owner) {
        if (receiveEventsWhileBeingStopped) {
            LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(this);
        }
    }

    public static void sendBroadcast(final Context context, final String geocode) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_GEOCACHE_CHANGED).putExtra(EXTRA_GEOCODE, geocode));
    }
}
