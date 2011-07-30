package cgeo.geocaching.sorting;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;

/**
 * sorts caches by distance to current position
 *
 */
public class DistanceComparator extends AbstractCacheComparator {
	private Double latitude = null;
	private Double longitude = null;

	public DistanceComparator() {
		// nothing
	}

	public DistanceComparator(Double latitudeIn, Double longitudeIn) {
		setCoords(latitudeIn, longitudeIn);
	}

	public void setCoords(Double latitudeIn, Double longitudeIn) {
		latitude = latitudeIn;
		longitude = longitudeIn;
	}

	@Override
	protected boolean canCompare(cgCache cache1, cgCache cache2) {
		return true;
	}

	@Override
	protected int compareCaches(final cgCache cache1, final cgCache cache2) {
		if ((cache1.latitude == null || cache1.longitude == null
				|| cache2.latitude == null || cache2.longitude == null)
				&& cache1.distance != null && cache2.distance != null) {
			if (cache1.distance < cache2.distance) {
				return -1;
			} else if (cache1.distance > cache2.distance) {
				return 1;
			} else {
				return 0;
			}
		} else {
			if (cache1.latitude == null || cache1.longitude == null) {
				return 1;
			}
			if (cache2.latitude == null || cache2.longitude == null) {
				return -1;
			}

			Double distance1 = cgBase.getDistance(latitude, longitude,
					cache1.latitude, cache1.longitude);
			Double distance2 = cgBase.getDistance(latitude, longitude,
					cache2.latitude, cache2.longitude);

			if (distance1 < distance2) {
				return -1;
			} else if (distance1 > distance2) {
				return 1;
			}
		}
		return 0;
	}
}
