package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class HealthScoreGeocacheFilter extends NumberRangeGeocacheFilter<Integer> {

    public HealthScoreGeocacheFilter() {
        super(Integer::valueOf, Math::round);
        setIncludeUnscored(false);
    }

    public boolean isIncludeUnscored() {
        return getIncludeSpecialNumber();
    }

    public void setIncludeUnscored(final boolean includeUnscored) {
        this.setSpecialNumber(Geocache.HEALTH_SCORE_UNKNOWN, includeUnscored);
        this.setIncludeNull(includeUnscored);
    }

    @Override
    public Integer getValue(final Geocache cache) {
        return cache.getHealthScore();
    }

    @Override
    protected String getSqlColumnName() {
        return "health_score";
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }

        final Integer score = cache.getHealthScore();
        return isInRange(score);
    }

    @Override
    protected String getUserDisplayableConfig() {
        return super.getUserDisplayableConfig(v -> v == null ? null : v + "%");
    }
}
