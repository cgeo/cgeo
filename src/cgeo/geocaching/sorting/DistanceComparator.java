package cgeo.geocaching.sorting;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.geopoint.Geopoint;

/**
 * sorts caches by distance to current position
 *
 */
public class DistanceComparator extends AbstractCacheComparator {
	private Geopoint coords = null;

	public DistanceComparator() {
		// nothing
	}

	public DistanceComparator(final Geopoint coords) {
		setCoords(coords);
	}

	public void setCoords(final Geopoint coords) {
		this.coords = coords;
	}

	@Override
	protected boolean canCompare(cgCache cache1, cgCache cache2) {
		return true;
	}

	@Override
	protected int compareCaches(final cgCache cache1, final cgCache cache2) {
		if ((cache1.coords == null || cache2.coords == null)
				&& cache1.distance != null && cache2.distance != null) {
			if (cache1.distance < cache2.distance) {
				return -1;
			} else if (cache1.distance > cache2.distance) {
				return 1;
			} else {
				return 0;
			}
		} else {
			if (cache1.coords == null) {
				return 1;
			}
			if (cache2.coords == null) {
				return -1;
			}

			Double distance1 = cgBase.getDistance(coords, cache1.coords);
			Double distance2 = cgBase.getDistance(coords, cache2.coords);

			if (distance1 < distance2) {
				return -1;
			} else if (distance1 > distance2) {
				return 1;
			}
		}
		return 0;
	}
}
