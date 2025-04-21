package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.location.Geopoint;

public interface DialogCallback {
    void onDialogClosed(Geopoint input);
}
