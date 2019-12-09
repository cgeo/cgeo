package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;

import android.os.Parcel;

import androidx.annotation.StringRes;

import java.util.Locale;

abstract class AbstractRangeFilter extends AbstractFilter {

    protected final float rangeMin;
    protected final float rangeMax;

    protected AbstractRangeFilter(@StringRes final int resourceId, final int range, final int upperBound) {
        super(CgeoApplication.getInstance().getString(resourceId) + ' ' +
                (range == upperBound ? Integer.toString(upperBound) : range + " + " + String.format(Locale.getDefault(), "%.1f", range + 0.5)));
        rangeMin = range;
        rangeMax = rangeMin + 1f;
    }

    protected AbstractRangeFilter(final Parcel in) {
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
