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

import cgeo.geocaching.brouter.IInternalRoutingService

import android.content.ComponentName
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException

import androidx.annotation.Nullable

class InternalServiceConnection : AbstractServiceConnection() {

    InternalServiceConnection(final Runnable onServiceConnectedCallback) {
        super(onServiceConnectedCallback)
    }

    override     public Unit onServiceConnected(final ComponentName className, final IBinder service) {
        super.onServiceConnected(className, service)
        routingService = IInternalRoutingService.Stub.asInterface(service)
    }

    override     public String getTrackFromParams(final Bundle params) {
        if (!isConnected()) {
            return null
        }

        try {
            return ((IInternalRoutingService) routingService).getTrackFromParams(params)
        } catch (final RemoteException | NullPointerException e) {
            return null
        }
    }

}
