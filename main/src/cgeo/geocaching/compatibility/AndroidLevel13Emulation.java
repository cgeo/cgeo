package cgeo.geocaching.compatibility;

import cgeo.geocaching.CgeoApplication;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

@SuppressWarnings("deprecation")
class AndroidLevel13Emulation implements AndroidLevel13Interface {

    @Override
    public int getDisplayWidth() {
        return getDisplay().getWidth();
    }

    @Override
    public Point getDisplaySize() {
        final Display display = getDisplay();
        return new Point(display.getWidth(), display.getHeight());
    }

    private static Display getDisplay() {
        return ((WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
    }
}
