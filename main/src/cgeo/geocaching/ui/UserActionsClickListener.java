package cgeo.geocaching.ui;

import cgeo.geocaching.Geocache;

import android.view.View;
import android.widget.TextView;

/**
 * Listener for clicks on user name
 */
public class UserActionsClickListener extends AbstractUserClickListener {

    public UserActionsClickListener(Geocache cache) {
        super(cache.supportsUserActions());
    }

    public UserActionsClickListener() {
        super(true);
    }

    @Override
    protected CharSequence getUserName(View view) {
        return ((TextView) view).getText().toString();
    }
}

