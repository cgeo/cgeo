package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.INavigationSource;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.view.ActionProvider;

/**
 * Action provider showing the compass icon, and reacting to normal click (primary navigation) and long click (secondary
 * navigation).
 */
public class NavigationActionProvider extends ActionProvider {

    private final Context context;
    private INavigationSource navigationSource;

    /**
     * Creates a new instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     *
     * @param context Context for accessing resources.
     */
    public NavigationActionProvider(final Context context) {
        super(context);
        this.context = context;
    }

    public void setNavigationSource(final INavigationSource navigationSource) {
        this.navigationSource = navigationSource;
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateActionView() {

        View view = null;

        if (navigationSource != null) {

            final LayoutInflater layoutInflater = LayoutInflater.from(context);
            view = layoutInflater.inflate(R.layout.navigation_action, null);

            final View navItem = view.findViewById(R.id.default_navigation_action);

            navItem.setOnClickListener(v -> navigationSource.startDefaultNavigation());

            navItem.setOnLongClickListener(v -> {
                navigationSource.startDefaultNavigation2();
                return true;
            });
        }

        return view;

    }

}
