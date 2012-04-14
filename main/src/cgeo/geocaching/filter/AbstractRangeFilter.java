package cgeo.geocaching.filter;

import cgeo.geocaching.cgeoapplication;


abstract class AbstractRangeFilter extends AbstractFilter {

    protected final float rangeMin;
    protected final float rangeMax;

    public AbstractRangeFilter(int ressourceId, int range) {
        super(cgeoapplication.getInstance().getResources().getString(ressourceId) + ' ' + (range == 5 ? '5' : String.valueOf(range) + " + " + String.format("%.1f", range + 0.5)));
        this.rangeMin = range;
        rangeMax = rangeMin + 1f;
    }
}