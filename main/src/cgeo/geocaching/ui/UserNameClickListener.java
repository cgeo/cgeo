package cgeo.geocaching.ui;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.connector.ConnectorFactory;

import android.view.View;

public class UserNameClickListener extends AbstractUserClickListener {

    final private String name;

    public UserNameClickListener(final Trackable trackable, final String name) {
        super(ConnectorFactory.getConnector(trackable).getUserActions());
        this.name = name;
    }

    @Override
    protected String getUserName(final View view) {
        return name;
    }
}
