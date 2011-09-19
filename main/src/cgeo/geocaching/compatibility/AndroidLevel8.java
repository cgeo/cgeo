package cgeo.geocaching.compatibility;

import android.app.Activity;

public class AndroidLevel8 {
    static {
        try {
            Class.forName("cgeo.geocaching.compatibility.AndroidLevel8Internal");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void check() {
        // nothing
    }

    public AndroidLevel8() {
    }

    public int getRotation(Activity activity) {
        return AndroidLevel8Internal.getRotation(activity);
    }
}
