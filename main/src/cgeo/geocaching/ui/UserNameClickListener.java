package cgeo.geocaching.ui;

import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.connector.ConnectorFactory;

import android.view.View;

public class UserNameClickListener extends AbstractUserClickListener {

    private final String name;

    public UserNameClickListener(final Trackable trackable, final String name) {
        super(ConnectorFactory.getConnector(trackable).getUserActions());
        this.name = name;
    }

    @Override
    protected String getUserName(final View view) {
        return name;
    }
}
