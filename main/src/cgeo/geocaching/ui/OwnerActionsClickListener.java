package cgeo.geocaching.ui;

import cgeo.geocaching.Geocache;

import org.apache.commons.lang3.StringUtils;

import android.view.View;
import android.widget.TextView;

/**
 * Listener for clicks on owner name
 */
public class OwnerActionsClickListener extends AbstractUserClickListener {

    public OwnerActionsClickListener(Geocache cache) {
        super(cache);
    }

    @Override
    protected String getUserName(View view) {
        // Use real owner name vice the one owner chose to display
        if (StringUtils.isNotBlank(cache.getOwnerUserId())) {
            return cache.getOwnerUserId();
        }
        return ((TextView) view).getText().toString();
    }
}

