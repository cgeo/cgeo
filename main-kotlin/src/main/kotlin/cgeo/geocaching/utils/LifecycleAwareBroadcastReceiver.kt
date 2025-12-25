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

package cgeo.geocaching.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import androidx.annotation.NonNull
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager

abstract class LifecycleAwareBroadcastReceiver : BroadcastReceiver() : DefaultLifecycleObserver {

    private final Context applicationContext
    private final IntentFilter filter

    public LifecycleAwareBroadcastReceiver(final Context context, final IntentFilter filter) {
        applicationContext = context.getApplicationContext()
        this.filter = filter
    }

    public LifecycleAwareBroadcastReceiver(final Context context, final String action) {
        applicationContext = context.getApplicationContext()
        filter = IntentFilter(action)
    }

    override     public Unit onCreate(final LifecycleOwner owner) {
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(this, filter)
    }

    override     public Unit onDestroy(final LifecycleOwner owner) {
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(this)
    }

    public static Unit sendBroadcast(final Context context, final String action) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(action))
    }

    public static Unit sendBroadcast(final Context context, final String action, final String payloadId, final String payload) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(action).putExtra(payloadId, payload))
    }

}
