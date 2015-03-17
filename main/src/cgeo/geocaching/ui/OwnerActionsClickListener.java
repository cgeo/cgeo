package cgeo.geocaching.ui;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.connector.ConnectorFactory;

import org.apache.commons.lang3.StringUtils;

import android.view.View;
import android.widget.TextView;

/**
 * Listener for clicks on owner name
 */
public class OwnerActionsClickListener extends AbstractUserClickListener {

    private final Geocache cache;

    public OwnerActionsClickListener(final Geocache cache) {
        super(ConnectorFactory.getConnector(cache).getUserActions());
        this.cache = cache;
    }

    @Override
    protected String getUserName(final View view) {
        // Use real owner name vice the one owner chose to display
        if (StringUtils.isNotBlank(cache.getOwnerUserId())) {
            return cache.getOwnerUserId();
        }
        return ((TextView) view).getText().toString();
    }
}

