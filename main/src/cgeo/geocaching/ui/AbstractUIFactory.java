package cgeo.geocaching.ui;

import cgeo.geocaching.CgeoApplication;

import android.content.res.Resources;

public class AbstractUIFactory {
    protected static final Resources res = CgeoApplication.getInstance().getResources();

    protected AbstractUIFactory() {
        // prevents calls from subclass throw new UnsupportedOperationException();
    }
}
