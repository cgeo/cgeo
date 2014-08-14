package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.INavigationSource;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;

public class NavigationActionProvider extends ActionProvider {

    private final Context context;
    private INavigationSource navigationSource;

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

            navItem.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(final View v) {
                    navigationSource.startDefaultNavigation();
                }
            });

            navItem.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(final View v) {
                    navigationSource.startDefaultNavigation2();
                    return true;
                }
            });
        }

        return view;

    }

}
