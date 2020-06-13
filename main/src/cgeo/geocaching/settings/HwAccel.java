package cgeo.geocaching.settings;

import android.os.Build;

import org.apache.commons.lang3.StringUtils;

public class HwAccel {

    private HwAccel() {
        // Utility class, do not instantiate
    }

    public static boolean hwAccelShouldBeEnabled() {
        return !hwAccelShouldBeDisabled();
    }

    private static boolean hwAccelShouldBeDisabled() {
        return  StringUtils.equals(Build.MODEL, "HTC One X") || // HTC One X
                StringUtils.equals(Build.MODEL, "HTC One S") || // HTC One S
                StringUtils.equals(Build.MODEL, "GT-I8190") || // Samsung S3 mini
                StringUtils.equals(Build.MODEL, "GT-S6310L") || // Samsung Galaxy Young
                StringUtils.equals(Build.MODEL, "GT-P5210") || // Samsung Galaxy Tab 3
                StringUtils.equals(Build.MODEL, "GT-S7580") || // Samsung Galaxy Trend Plus
                StringUtils.equals(Build.MODEL, "GT-I9105P") || // Samsung Galaxy SII Plus
                StringUtils.equals(Build.MODEL, "ST25i") || // Sony Xperia U
                StringUtils.equals(Build.MODEL, "bq Aquaris 5") || // bq Aquaris 5
                StringUtils.equals(Build.MODEL, "A1-810") || // Unknown A1-810
                StringUtils.equals(Build.MODEL, "GT-I9195") || // Samsung S4 mini
                StringUtils.equals(Build.MODEL, "GT-I8200N") || // Samsung S3 mini
                StringUtils.equals(Build.MODEL, "Q800") || // XOLO Q800
                StringUtils.equals(Build.MODEL, "P5_Quad");        // Allview P5 Quad
    }
}
