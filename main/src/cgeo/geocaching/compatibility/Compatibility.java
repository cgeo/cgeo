package cgeo.geocaching.compatibility;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.reflect.MethodUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.text.InputType;
import android.view.Display;
import android.view.Surface;
import android.widget.EditText;

public final class Compatibility {

    private final static int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
    private final static boolean isLevel8 = sdkVersion >= 8;
    private final static boolean isLevel5 = sdkVersion >= 5;

    private final static AndroidLevel8Interface level8;
    private final static AndroidLevel11Interface level11;

    static {
        if (isLevel8) {
            level8 = new AndroidLevel8();
        }
        else {
            level8 = new AndroidLevel8Dummy();
        }
        if (sdkVersion >= 11) {
            level11 = new AndroidLevel11();
        }
        else {
            level11 = new AndroidLevel11Dummy();
        }
    }

    public static float getDirectionNow(final float directionNowPre,
            final Activity activity) {
        if (isLevel8) {
            try {
                final int rotation = level8.getRotation(activity);
                if (rotation == Surface.ROTATION_90) {
                    return directionNowPre + 90;
                } else if (rotation == Surface.ROTATION_180) {
                    return directionNowPre + 180;
                } else if (rotation == Surface.ROTATION_270) {
                    return directionNowPre + 270;
                }
            } catch (final Exception e) {
                // This should never happen: IllegalArgumentException, IllegalAccessException or InvocationTargetException
                Log.e("Cannot call getRotation()", e);
            }
        } else {
            final Display display = activity.getWindowManager()
                    .getDefaultDisplay();
            final int rotation = display.getOrientation();
            if (rotation == Configuration.ORIENTATION_LANDSCAPE) {
                return directionNowPre + 90;
            }
        }
        return directionNowPre;
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

}
