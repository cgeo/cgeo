package cgeo.geocaching.maps.mapsforge;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapsforge.android.maps.MapViewMode;

import android.app.Activity;
import android.content.res.Resources;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.interfaces.MapFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;

public class mfMapProvider implements MapProvider {
	
	private static final int MF_MAP_MAPNIK = 0;
	private static final int MF_MAP_OSMARENDER = 1;
	private static final int MF_MAP_CYCLEMAP = 2;
	private static final int MF_MAP_OFFLINE = 3;

	private static mfMapFactory mapFactory = new mfMapFactory();
	private static String[] names = null;
	private static int baseId = 0;
	
	public mfMapProvider(int baseIdIn) {
		baseId = baseIdIn;
	}

	@Override
	public int[] getSourceIds() {
		return new int[]{baseId + MF_MAP_MAPNIK, baseId + MF_MAP_OSMARENDER,
				baseId + MF_MAP_CYCLEMAP, baseId + MF_MAP_OFFLINE};
	}

	@Override
	public Class<? extends Activity> getMapClass() {
		return mfMapActivity.class;
	}

	@Override
	public MapFactory getMapFactory() {
		return mapFactory;
	}

	@Override
	public String getName(int sourceId, Resources res) {
		if (ArrayUtils.isEmpty(names)) {
			names = res.getStringArray(R.array.map_sources_mapsforge);
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
		return MF_MAP_MAPNIK <= offSet && MF_MAP_OFFLINE >= offSet;
	}

	public static MapViewMode getMapViewMode(int sourceId) {
		switch(sourceId) {
			case MF_MAP_OSMARENDER:
				return MapViewMode.OSMARENDER_TILE_DOWNLOAD;
			case MF_MAP_CYCLEMAP:
				return MapViewMode.OPENCYCLEMAP_TILE_DOWNLOAD;
			case MF_MAP_OFFLINE:
				return MapViewMode.CANVAS_RENDERER;
			default:
				return MapViewMode.MAPNIK_TILE_DOWNLOAD;
		}
	}
}
