package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;


abstract class AbstractRangeFilter extends AbstractFilter {

    private static final long serialVersionUID = -4252986778995114530L;
    protected final float rangeMin;
    protected final float rangeMax;

    protected AbstractRangeFilter(final int ressourceId, final int range) {
        super(CgeoApplication.getInstance().getResources().getString(ressourceId) + ' ' + (range == 5 ? '5' : range + " + " + String.format("%.1f", range + 0.5)));
        this.rangeMin = range;
        rangeMax = rangeMin + 1f;
    }
}