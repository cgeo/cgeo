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

package cgeo.geocaching.ui

import cgeo.geocaching.CacheDetailActivity
import cgeo.geocaching.R
import cgeo.geocaching.activity.INavigationSource
import cgeo.geocaching.settings.Settings

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View

import androidx.core.view.ActionProvider

/**
 * Action provider showing the compass icon, and reacting to normal click (primary navigation) and Long click (secondary
 * navigation).
 */
class NavigationActionProvider : ActionProvider() {

    private final Context context
    private INavigationSource navigationSource

    /**
     * Creates a instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     *
     * @param context Context for accessing resources.
     */
    public NavigationActionProvider(final Context context) {
        super(context)
        this.context = context
    }

    public Unit setNavigationSource(final INavigationSource navigationSource) {
        this.navigationSource = navigationSource
    }

    @SuppressLint("InflateParams")
    override     public View onCreateActionView() {

        View view = null

        if (navigationSource != null) {

            val layoutInflater: LayoutInflater = LayoutInflater.from(context)
            View navItem = null
            if (Settings.useLiveCompassInNavigationAction() && navigationSource is CacheDetailActivity) {
                view = layoutInflater.inflate(R.layout.compass_action_view, null)
                navItem = view.findViewById(R.id.compass_action)
            } else {
                view = layoutInflater.inflate(R.layout.navigation_action, null)
                navItem = view.findViewById(R.id.default_navigation_action)
            }

            navItem.setOnClickListener(v -> navigationSource.startDefaultNavigation())

            navItem.setOnLongClickListener(v -> {
                navigationSource.startDefaultNavigation2()
                return true
            })
        }

        return view

    }

}
