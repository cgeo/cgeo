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

package cgeo.geocaching.apps.navi

import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.Log

import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.StringRes

abstract class GoogleNavigationApp : AbstractPointNavigationApp() {

    private final String mode

    private GoogleNavigationApp(@StringRes final Int nameResourceId, final String mode) {
        super(getString(nameResourceId), null)
        this.mode = mode
    }

    override     public Boolean isInstalled() {
        return true
    }

    override     public Unit navigate(final Context context, final Geopoint coords) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri
                    .parse("google.navigation:q=" + coords.getLatitude() + ","
                            + coords.getLongitude() + "&mode=" + mode)))

        } catch (final Exception e) {
            Log.i("GoogleNavigationApp.navigate: No navigation application available.", e)
        }
    }

    static class GoogleNavigationWalkingApp : GoogleNavigationApp() {
        GoogleNavigationWalkingApp() {
            super(R.string.cache_menu_navigation_walk, "w")
        }
    }

    static class GoogleNavigationTransitApp : GoogleNavigationApp() {
        GoogleNavigationTransitApp() {
            super(R.string.cache_menu_navigation_transit, "r")
        }
    }

    static class GoogleNavigationDrivingApp : GoogleNavigationApp() : TargetSelectorNavigationApp {
        GoogleNavigationDrivingApp() {
            super(R.string.cache_menu_navigation_drive, "d")
        }

        override         public Unit navigate(final Context context, final Geocache cache) {
            navigateWithTargetSelector(context, cache)
        }
    }

    static class GoogleNavigationBikeApp : GoogleNavigationApp() {
        GoogleNavigationBikeApp() {
            super(R.string.cache_menu_navigation_bike, "b")
        }
    }
}
