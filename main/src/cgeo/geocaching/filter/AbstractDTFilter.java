package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;

import android.annotation.SuppressLint;
import android.os.Parcel;

import androidx.annotation.StringRes;

import java.util.Locale;

abstract class AbstractDTFilter extends AbstractFilter {

    protected final float rangeMin;
    protected final float rangeMax;

    @SuppressLint("SetTextI18n")
    protected AbstractDTFilter(@StringRes final int resourceId, final float value) {
        super(CgeoApplication.getInstance().getString(resourceId) + ' ' + String.format(Locale.getDefault(), "%.1f", value));
        // range to be chosen so that different D/T values do not overlap
        rangeMin = value - 0.15f;
        rangeMax = value + 0.15f;
    }

    protected AbstractDTFilter(final Parcel in) {
        super(in);
        rangeMin = in.readFloat();
        rangeMax = in.readFloat();
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
        dest.writeFloat(rangeMin);
        dest.writeFloat(rangeMax);
    }
}
