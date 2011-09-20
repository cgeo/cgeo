package cgeo.geocaching.compatibility;

import android.app.Activity;
import android.view.Display;

public class AndroidLevel8 {

    public int getRotation(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        return display.getRotation();
    }

    }
}
