/*
 * This file is part of WhereYouGo.
 * 
 * WhereYouGo is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * WhereYouGo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with WhereYouGo. If not,
 * see <http://www.gnu.org/licenses/>.
 * 
 * Copyright (C) 2012 Menion <whereyougo@asamm.cz>
 */

package menion.android.whereyougo.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.util.DisplayMetrics;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.MessageDigest;
import java.util.List;

/**
 * @author menion
 * @since 25.1.2010 2010
 */
public class Utils {

    private static final String TAG = "Utils";


    private static float density = -1.0f;

    private static int densityDpi = 0;

    private static int screenCategory = -1;

    private static MessageDigest md;

    private static final char[] hexDigit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f'};

    /**
     * Adds '0' chars at the beginning of string of specified length Menion: this is one of most
     * memory eating function !!!
     *
     * @param s      original string
     * @param length desired length
     * @return string with zeros
     */
    public static String addZerosBefore(String s, int length) {
        String prefix = "";
        for (int i = 0; i < (length - s.length()); i++) {
            prefix = prefix.concat("0");
        }
        return prefix + s;
    }

    private static String bytesToHex(byte[] b) {
        int length = Math.min(b.length, 5);
        StringBuilder buf = new StringBuilder();
        for (int j = 0; j < length; j++) {
            buf.append(hexDigit[(b[j] >> 4) & 0x0f]);
            buf.append(hexDigit[b[j] & 0x0f]);
        }
        return buf.toString();
    }

    public static String streamToString(final InputStream is) {
        final char[] buffer = new char[1024];
        final StringBuilder out = new StringBuilder();
        Reader in;
        try {
            in = new InputStreamReader(is, "UTF-8");
            while (true) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0)
                    break;
                out.append(buffer, 0, rsz);
            }
        } catch (IOException ex) {
        }
        finally {
            closeStream(is);
        }
        return out.toString();
    }

    public static void closeStream(Closeable is) {
        try {
            if (is != null) {
                is.close();
                is = null;
            }
        } catch (Exception e) {
            Logger.e(TAG, "closeStream(" + is + ")", e);
        }
    }

    public static float getDensity() {
        return getDpPixels(1.0f);
    }

    public static float getDpPixels(Context context, float pixels) {
        try {
            if (density == -1.0f) {
                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                density = metrics.density;
                densityDpi = metrics.densityDpi;
                // Logger.i(TAG, "density:" + density);
            }
            return density * pixels;
        } catch (Exception e) {
            Logger.e(TAG, "getDpPixels(" + pixels + "), e:" + e);
            e.printStackTrace();
            return pixels;
        }
    }

    public static float getDpPixels(float pixels) {
        return getDpPixels(A.getApp(), pixels);
    }

    public static int getScreenCategory() {
        if (screenCategory == -1) {
            getDpPixels(1.0f);
            if (Const.SCREEN_WIDTH * Const.SCREEN_HEIGHT >= 960 * 720) {
                screenCategory = Const.SCREEN_SIZE_XLARGE;
            } else if (Const.SCREEN_WIDTH * Const.SCREEN_HEIGHT >= 640 * 480) {
                screenCategory = Const.SCREEN_SIZE_LARGE;
            } else if (Const.SCREEN_WIDTH * Const.SCREEN_HEIGHT >= 470 * 320) {
                screenCategory = Const.SCREEN_SIZE_MEDIUM;
            } else {
                screenCategory = Const.SCREEN_SIZE_SMALL;
            }
        }
        return screenCategory;
    }

    public static int getScreenDpi() {
        getDpPixels(1.0f);
        return densityDpi;
    }

    public synchronized static String hashString(String data) {
        try {
            if (data == null || data.length() == 0)
                return "";

            if (md == null) {
                md = MessageDigest.getInstance("SHA1");
            }
            md.update(data.getBytes());
            return bytesToHex(md.digest());
        } catch (Exception e) {
            Logger.e(TAG, "hashString(" + data + ")", e);
        }
        return "";
    }

    public static boolean isAndroid201OrMore() {
        return Utils.parseInt(Build.VERSION.SDK) >= 6;
    }

    public static boolean isAndroid21OrMore() {
        return Utils.parseInt(Build.VERSION.SDK) >= 7;
    }

    public static boolean isAndroid22OrMore() {
        return Utils.parseInt(Build.VERSION.SDK) >= 8;
    }

    public static boolean isAndroid23OrMore() {
        return Utils.parseInt(Build.VERSION.SDK) >= 9;
    }

    /* OTHERS SECTION */

    public static boolean isAndroid30OrMore() {
        return Utils.parseInt(Build.VERSION.SDK) >= 11;
    }

    public static boolean isAndroidTablet30OrMore() {
        return Utils.parseInt(Build.VERSION.SDK) >= 11
                && getScreenCategory() == Const.SCREEN_SIZE_XLARGE;
    }

    public static boolean isIntentAvailable(Intent intent) {
        final PackageManager packageManager = A.getApp().getPackageManager();
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        // Logger.d(TAG, "isIntentAvailable(" + intent + "), res:" + list.size());
        return list.size() > 0;
    }

    /* SECURITY PART */

    public static boolean isIntentAvailable(String action) {
        return isIntentAvailable(new Intent(action));
    }

    public static boolean isPermissionAllowed(String permission) {
        try {
            return A.getApp().checkPermission(permission, android.os.Binder.getCallingPid(),
                    android.os.Binder.getCallingUid()) == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            Logger.e(TAG, "isPermissionAllowed(" + permission + ")", e);
            return false;
        }
    }

    public static boolean parseBoolean(Object data) {
        return parseBoolean(String.valueOf(data));
    }

    public static boolean parseBoolean(String data) {
        try {
            return data.toLowerCase().contains("true") || data.contains("1");
        } catch (Exception e) {
            return false;
        }
    }

    /* PARSE SECTION */

    public static double parseDouble(Object data) {
        return parseDouble(String.valueOf(data));
    }

    public static double parseDouble(String data) {
        try {
            return Double.parseDouble(data.trim());
        } catch (Exception e) {
            // Logger.e("Utils", "parseDouble(" + data + ")", e);
            return 0.0;
        }
    }

    public static float parseFloat(Object data) {
        return parseFloat(String.valueOf(data));
    }

    public static float parseFloat(String data) {
        try {
            return Float.parseFloat(data.trim());
        } catch (Exception e) {
            return 0.0f;
        }
    }

    public static int parseInt(Object data) {
        return parseInt(String.valueOf(data));
    }

    public static int parseInt(String data) {
        try {
            return Integer.parseInt(data.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public static Integer parseInteger(Object data) {
        return parseInteger(String.valueOf(data));
    }

    public static Integer parseInteger(String data) {
        try {
            return Integer.valueOf(data.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public static long parseLong(Object data) {
        return parseLong(String.valueOf(data));
    }

    public static long parseLong(String data) {
        try {
            return Long.parseLong(data.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
