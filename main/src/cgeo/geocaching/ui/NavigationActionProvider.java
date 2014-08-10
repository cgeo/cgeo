package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.INavigationSource;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.ActionProvider;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

public class NavigationActionProvider extends ActionProvider {

    private final Context context;
    private INavigationSource navigationSource;
    private final GestureDetector gestureDestector;

    public NavigationActionProvider(final Context context) {
        super(context);
        this.context = context;
        gestureDestector = new GestureDetector(context, new GestureListener());
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

            navItem.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(final View v, final MotionEvent event) {
                    gestureDestector.onTouchEvent(event);
                    return v.onTouchEvent(event);
                }
            });
        }

        return view;

    }

    private class GestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onDoubleTapEvent(final MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                navigationSource.startDefaultNavigation2();
                return true;
            }
            return super.onDoubleTapEvent(e);
        }

        @Override
        public boolean onDown(final MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            navigationSource.startDefaultNavigation();
            return true;
        }

    }
}
