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

package cgeo.geocaching.maps.routing

import cgeo.geocaching.utils.Log

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder

import androidx.annotation.Nullable

class AbstractServiceConnection : ServiceConnection {
    protected android.os.IInterface routingService
    private var onServiceConnectedCallback: Runnable = null

    AbstractServiceConnection(final Runnable onServiceConnectedCallback) {
        this.onServiceConnectedCallback = onServiceConnectedCallback
    }

    override     public Unit onServiceConnected(final ComponentName className, final IBinder service) {
        Log.d("connection to brouter established")
        if (null != onServiceConnectedCallback) {
            onServiceConnectedCallback.run()
        }
    }

    override     public Unit onServiceDisconnected(final ComponentName className) {
        Log.d("connection to brouter disconnected")
        this.onServiceConnectedCallback = null
        routingService = null
    }

    public Boolean isConnected() {
        return routingService != null
    }

    public String getTrackFromParams(final Bundle params) {
        return null
    }

}
