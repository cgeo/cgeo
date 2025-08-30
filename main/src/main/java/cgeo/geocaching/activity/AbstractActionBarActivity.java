package cgeo.geocaching.activity;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.ViewUtils;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Classes actually having an ActionBar (as opposed to the Dialog activities)
 */
public class AbstractActionBarActivity extends AbstractActivity {

    protected int calculateInsets = WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout();
    boolean skipActionBarInsetCalculation = false; // for edge to edge configuration, see onCreate()

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUpAction();

        // edge2edge configuration
        final Window currentWindow = getWindow();
        final WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(currentWindow, currentWindow.getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        ViewCompat.setOnApplyWindowInsetsListener(currentWindow.getDecorView(), new OnApplyWindowInsetsListener() {
            @Override
            public @NonNull WindowInsetsCompat onApplyWindowInsets(final @NonNull View v, final @NonNull WindowInsetsCompat insets) {
                // only apply padding calculations when edge2edge is currently active
                final Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                final boolean isEdgeToEdge = systemBars.top == systemBars.bottom; // seems to be no official API for this
                if (isEdgeToEdge) {
                    // make room for action bar on top + whatever other insets are requested with calculateInsets variable
                    final ViewGroup activityContent = v.findViewById(R.id.activity_content);
                    if (activityContent != null && !skipActionBarInsetCalculation) {
                        final float actionBarHeight = getResources().getDimension(R.dimen.actionbar_height);
                        final Insets innerPadding = insets.getInsets(calculateInsets);
                        activityContent.setPadding(innerPadding.left, (int) (innerPadding.top + ViewUtils.dpToPixelFloat(10f) + actionBarHeight), innerPadding.right, innerPadding.bottom);
                    }
                }
                return insets;
            }
        });
    }

    private void initUpAction() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // don't display a back button it the activity is running as top-level activity with bottom navigation attached
            actionBar.setDisplayHomeAsUpEnabled(!isTaskRoot() || !(this instanceof AbstractNavigationBarActivity));
        }
    }

    @Override
    public void setTitle(final CharSequence title) {
        super.setTitle(title);
        // reflect the title in the actionbar
        ActivityMixin.setTitle(this, title);
    }
}
