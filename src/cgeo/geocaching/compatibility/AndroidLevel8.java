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

    private AndroidLevel8Internal internal;

    public static void check() {
        // nothing
    }

    public AndroidLevel8() {
        internal = new AndroidLevel8Internal();
    }

    public int getRotation(Activity activity) {
        return internal.getRotation(activity);
    }
}
