package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;

import android.os.Parcel;

abstract class AbstractRangeFilter extends AbstractFilter {

    protected final float rangeMin;
    protected final float rangeMax;

    protected AbstractRangeFilter(final int ressourceId, final int range) {
        super(CgeoApplication.getInstance().getResources().getString(ressourceId) + ' ' + (range == 5 ? '5' : range + " + " + String.format("%.1f", range + 0.5)));
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