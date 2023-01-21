package cgeo.geocaching.utils;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryOwner;
import static androidx.lifecycle.Lifecycle.State.INITIALIZED;

/**
 * Helper class to register a custom data bundle to an activity. The content of this bundle will be kept available
 * across Activity Lifecycles including destroy/re-creation using the Activity's saveState mechanism underneath.
 */
public class ActivitySavedState implements SavedStateRegistry.SavedStateProvider {

    private final String providerKey;
    private final Bundle data = new Bundle();

    /** Creates and registers saved state with given activity. Provide a unique key so this state does not interfer with other states saved for the activity */
    public ActivitySavedState(final SavedStateRegistryOwner owner, final String key) {

        if (owner.getLifecycle().getCurrentState() != INITIALIZED) {
            throw new IllegalStateException("SavedStateOwner must be in state INITIALIZED when creating saved state: " + owner.getClass().getName());
        }

        this.providerKey = "ActivitySavedState-" + owner.getClass().getName() + "-" + key;
        owner.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            final SavedStateRegistry registry = owner.getSavedStateRegistry();
            if (event == Lifecycle.Event.ON_CREATE) {

                // Register this object for future calls to saveState()
                registry.registerSavedStateProvider(providerKey, this);

                // Get the previously saved state and restore it
                final Bundle state = registry.consumeRestoredStateForKey(providerKey);

                // Apply the previously saved state
                if (state != null) {
                    data.clear();
                    data.putAll(state);
                }
            }
            if (event == Lifecycle.Event.ON_DESTROY) {
                registry.unregisterSavedStateProvider(providerKey);
            }
        });


    }

    @NonNull
    @Override
    public Bundle saveState() {
        final Bundle bundle = new Bundle();
        bundle.putAll(data);
        return bundle;
    }

    public Bundle get() {
        return data;
    }

    public void set(final Bundle state) {
        this.data.clear();
        if (state != null) {
            this.data.putAll(state);
        }
    }

    public void clear() {
        this.data.clear();
    }


}
