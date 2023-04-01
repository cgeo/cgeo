package cgeo.geocaching.utils;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class BundleUtils {

    private BundleUtils() {
        // utility class
    }

    @NonNull
    public static String getString(final Bundle bundle, @NonNull final String key, @NonNull final String defaultValue) {
        final String res = bundle.getString(key);
        if (res != null) {
            return res;
        }
        return defaultValue;
    }

    public static int getParcelSize(final Parcelable parcelable) {
        if (parcelable == null) {
            return 0;
        }
        final Parcel p = Parcel.obtain();
        try {
            parcelable.writeToParcel(p, 0);
            return p.dataSize();
        } finally {
            p.recycle();
        }
    }
}
