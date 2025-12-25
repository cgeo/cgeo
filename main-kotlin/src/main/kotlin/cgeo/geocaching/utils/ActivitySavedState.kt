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

import android.os.Bundle

import androidx.annotation.NonNull
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.Lifecycle.State.INITIALIZED

/**
 * Helper class to register a custom data bundle to an activity. The content of this bundle will be kept available
 * across Activity Lifecycles including destroy/re-creation using the Activity's saveState mechanism underneath.
 */
class ActivitySavedState : SavedStateRegistry.SavedStateProvider {

    private final String providerKey
    private val data: Bundle = Bundle()

    /** Creates and registers saved state with given activity. Provide a unique key so this state does not interfer with other states saved for the activity */
    public ActivitySavedState(final SavedStateRegistryOwner owner, final String key) {

        if (owner.getLifecycle().getCurrentState() != INITIALIZED) {
            throw IllegalStateException("SavedStateOwner must be in state INITIALIZED when creating saved state: " + owner.getClass().getName())
        }

        this.providerKey = "ActivitySavedState-" + owner.getClass().getName() + "-" + key
        owner.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            val registry: SavedStateRegistry = owner.getSavedStateRegistry()
            if (event == Lifecycle.Event.ON_CREATE) {

                // Register this object for future calls to saveState()
                registry.registerSavedStateProvider(providerKey, this)

                // Get the previously saved state and restore it
                val state: Bundle = registry.consumeRestoredStateForKey(providerKey)

                // Apply the previously saved state
                if (state != null) {
                    data.clear()
                    data.putAll(state)
                }
            }
            if (event == Lifecycle.Event.ON_DESTROY) {
                registry.unregisterSavedStateProvider(providerKey)
            }
        })


    }

    override     public Bundle saveState() {
        val bundle: Bundle = Bundle()
        bundle.putAll(data)
        return bundle
    }

    public Bundle get() {
        return data
    }

    public Unit set(final Bundle state) {
        this.data.clear()
        if (state != null) {
            this.data.putAll(state)
        }
    }

    public Unit clear() {
        this.data.clear()
    }


}
