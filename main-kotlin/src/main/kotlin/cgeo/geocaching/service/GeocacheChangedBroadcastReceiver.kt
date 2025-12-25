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

package cgeo.geocaching.service

import cgeo.geocaching.Intents.ACTION_GEOCACHE_CHANGED
import cgeo.geocaching.Intents.EXTRA_GEOCODE

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import androidx.annotation.NonNull
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager

abstract class GeocacheChangedBroadcastReceiver : BroadcastReceiver() : DefaultLifecycleObserver {

    private final Context applicationContext
    private final Boolean receiveEventsWhileBeingStopped

    public GeocacheChangedBroadcastReceiver(final Context context) {
        this(context, false)
    }

    public GeocacheChangedBroadcastReceiver(final Context context, final Boolean receiveEventsWhileBeingStopped) {
        applicationContext = context.getApplicationContext()
        this.receiveEventsWhileBeingStopped = receiveEventsWhileBeingStopped
    }

    protected abstract Unit onReceive(Context context, String geocode)

    override     public Unit onReceive(final Context context, final Intent intent) {
        val geocode: String = intent.getStringExtra(EXTRA_GEOCODE)
        onReceive(context, geocode)
    }

    override     public Unit onStart(final LifecycleOwner owner) {
        if (!receiveEventsWhileBeingStopped) {
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(this, IntentFilter(ACTION_GEOCACHE_CHANGED))
        }
    }

    override     public Unit onStop(final LifecycleOwner owner) {
        if (!receiveEventsWhileBeingStopped) {
            LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(this)
        }
    }

    override     public Unit onCreate(final LifecycleOwner owner) {
        if (receiveEventsWhileBeingStopped) {
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(this, IntentFilter(ACTION_GEOCACHE_CHANGED))
        }
    }

    override     public Unit onDestroy(final LifecycleOwner owner) {
        if (receiveEventsWhileBeingStopped) {
            LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(this)
        }
    }

    public static Unit sendBroadcast(final Context context, final String geocode) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(ACTION_GEOCACHE_CHANGED).putExtra(EXTRA_GEOCODE, geocode))
    }
}
