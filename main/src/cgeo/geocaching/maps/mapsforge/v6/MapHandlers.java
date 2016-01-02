package cgeo.geocaching.maps.mapsforge.v6;

import android.os.Handler;

/**
 * Created by rainer on 13.12.15.
 */
public class MapHandlers {

    private final TapHandler tapHandler;
    private final Handler displayHandler;
    private final Handler showProgressHandler;

    public MapHandlers(TapHandler tapHandler, Handler displayHandler, Handler showProgressHandler)
    {
        this.tapHandler = tapHandler;
        this.displayHandler = displayHandler;
        this.showProgressHandler = showProgressHandler;
    }

    public TapHandler getTapHandler() {
        return tapHandler;
    }

    public void sendEmptyProgressMessage(int progressMessage) {
        showProgressHandler.sendEmptyMessage(progressMessage);
    }

    public void sendEmptyDisplayMessage(int displayMessage) {
        displayHandler.sendEmptyMessage(displayMessage);
    }
}
