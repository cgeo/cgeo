package cgeo.geocaching.compatibility;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.reflect.MethodUtils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.text.InputType;
import android.widget.EditText;

public final class Compatibility {

    private final static int sdkVersion = Build.VERSION.SDK_INT;
    private final static boolean isLevel8 = sdkVersion >= 8;
    private final static boolean isLevel5 = sdkVersion >= 5;

    private final static AndroidLevel8Interface level8;
    private final static AndroidLevel11Interface level11;
    private final static AndroidLevel13Interface level13;

    static {
        if (isLevel8) {
            level8 = new AndroidLevel8();
        }
        else {
            level8 = new AndroidLevel8Emulation();
        }
        if (sdkVersion >= 11) {
            level11 = new AndroidLevel11();
        }
        else {
            level11 = new AndroidLevel11Emulation();
        }
        if (sdkVersion >= 13) {
            level13 = new AndroidLevel13();
        }
        else {
            level13 = new AndroidLevel13Emulation();
        }
    }

    /**
     * Add 90, 180 or 270 degrees to the given rotation.
     *
     * @param directionNowPre the direction in degrees before adjustment
     * @param activity the activity whose rotation is used to adjust the direction
     * @return the adjusted direction, in the [0, 360[ range
     */
    public static float getDirectionNow(final float directionNowPre, final Activity activity) {
        return AngleUtils.normalize(directionNowPre + level8.getRotationOffset(activity));
    }

    public static void dataChanged(final String name) {
        level8.dataChanged(name);
    }

    public static void disableSuggestions(EditText edit) {
        if (isLevel5) {
            edit.setInputType(edit.getInputType()
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    | InputType.TYPE_TEXT_VARIATION_FILTER);
        }
        else {
            edit.setInputType(edit.getInputType()
                    | InputType.TYPE_TEXT_VARIATION_FILTER);
        }
    }

    private static void overridePendingTransition(final Activity activity, int enterAnim, int exitAnim) {
        try {
            MethodUtils.invokeMethod(activity, "overridePendingTransition", enterAnim, exitAnim);
        } catch (Exception e) {
            Log.e("cannot call overridePendingTransition", e);
        }
    }

    public static void restartActivity(AbstractActivity activity) {
        final Intent intent = activity.getIntent();
        if (isLevel5) {
            overridePendingTransition(activity, 0, 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        activity.finish();
        if (isLevel5) {
            overridePendingTransition(activity, 0, 0);
        }
        activity.startActivity(intent);
    }

    public static void invalidateOptionsMenu(final Activity activity) {
        level11.invalidateOptionsMenu(activity);
    }

    public static int getDisplayWidth() {
        return level13.getDisplayWidth();
    }

    public static int getDisplayHeight() {
        return level13.getDisplayHeight();
    }

    public static Point getDisplaySize() {
        return level13.getDisplaySize();
    }

}
