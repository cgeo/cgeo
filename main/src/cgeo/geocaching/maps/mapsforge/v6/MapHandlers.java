package cgeo.geocaching.maps.mapsforge.v6;

import android.os.Handler;

public class MapHandlers {

    private final TapHandler tapHandler;
    private final Handler displayHandler;
    private final Handler showProgressHandler;

    public MapHandlers(final TapHandler tapHandler, final Handler displayHandler, final Handler showProgressHandler) {
        this.tapHandler = tapHandler;
        this.displayHandler = displayHandler;
        this.showProgressHandler = showProgressHandler;
    }

    public TapHandler getTapHandler() {
        return tapHandler;
    }

    public void sendEmptyProgressMessage(final int progressMessage) {
        showProgressHandler.sendEmptyMessage(progressMessage);
    }

    public void sendEmptyDisplayMessage(final int displayMessage) {
        displayHandler.sendEmptyMessage(displayMessage);
    }
}
