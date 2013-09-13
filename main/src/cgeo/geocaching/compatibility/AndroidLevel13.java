package cgeo.geocaching.compatibility;

import cgeo.geocaching.CgeoApplication;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.view.WindowManager;

@TargetApi(value = 13)
public class AndroidLevel13 implements AndroidLevel13Interface {

    @Override
    public int getDisplayWidth() {
        return getDisplaySize().x;
    }

    @Override
    public Point getDisplaySize() {
        Point dimensions = new Point();
        ((WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getSize(dimensions);
        return dimensions;
    }

}
