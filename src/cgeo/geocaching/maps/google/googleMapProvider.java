package cgeo.geocaching.maps.google;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.res.Resources;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.interfaces.MapFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;

public class googleMapProvider implements MapProvider {

	private static final int GOOGLE_MAP_MAP = 0;
	private static final int GOOGLE_MAP_SATELLITE = 1;
	
	private static googleMapFactory mapFactory = new googleMapFactory();
	private static String[] names = null;
	private static int baseId = 0;
	
	public googleMapProvider(int baseIdIn) {
		baseId = baseIdIn;
	}
	
	@Override
	public MapFactory getMapFactory() {
		return mapFactory;
	}

	@Override
	public String getName(int sourceId, Resources res) {

		if (ArrayUtils.isEmpty(names)) {
			names = res.getStringArray(R.array.map_sources_google);
		}
		
		if (hasSourceId(sourceId)) {
			return names[sourceId-baseId];
		} else {
			return StringUtils.EMPTY;
		}
	}

	@Override
	public boolean hasSourceId(int Id) {
		int offSet = Id - baseId;
		return GOOGLE_MAP_MAP <= offSet && GOOGLE_MAP_SATELLITE >= offSet;
	}

	@Override
	public int[] getSourceIds() {
		return new int[]{baseId + GOOGLE_MAP_MAP, baseId + GOOGLE_MAP_SATELLITE};
	}

	@Override
	public Class<? extends Activity> getMapClass() {
		return googleMapActivity.class;
	}

	public static boolean isSatelliteSourceId(int mapSourceId) {
		return GOOGLE_MAP_SATELLITE == mapSourceId-baseId;
	}

}
