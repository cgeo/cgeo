package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.IGeoData;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.CacheDistance;
import cgeo.geocaching.geopoint.Geopoint;

import java.util.LinkedList;
import java.util.List;

class DistanceFilter extends AbstractFilter {
	private final CacheDistance cacheDistance;
    private final IGeoData geo;

    public DistanceFilter(final CacheDistance cacheDistance) {
        super(cacheDistance.id);
        this.cacheDistance = cacheDistance;
        geo = cgeoapplication.getInstance().currentGeo();
    }

    @Override
    public boolean accepts(Geocache cache) {
        Geopoint temp = new Geopoint(geo.getLocation());
        double distance = temp.distanceTo(cache.getCoords());

        return (distance > cacheDistance.minDistance) && (distance <= cacheDistance.maxDistance);
    }

    @Override
    public String getName() {
        return cacheDistance.getL10n();
    }

    public static class Factory implements IFilterFactory {

        @Override
        public List<IFilter> getFilters() {
            final CacheDistance[] cacheDistances = CacheDistance.values();
            final List<IFilter> filters = new LinkedList<IFilter>();
            for (CacheDistance cacheDistance : cacheDistances) {
                filters.add(new DistanceFilter(cacheDistance));
            }
            return filters;
        }

    }
}
