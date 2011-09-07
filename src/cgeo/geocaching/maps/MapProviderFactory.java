package cgeo.geocaching.maps;

import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;

import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuItem;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.maps.google.googleMapProvider;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.mapsforge.mfMapProvider;

public class MapProviderFactory {
	
	private static final int GOOGLE_MAP_IDBASE = 100;
	private static final int MAPSFORGE_MAP_ID_BASE = 200;

	private static MapProvider[] mapProviders = new MapProvider[] {};

	private static MapProvider[] getMapProviders() {
		if (ArrayUtils.isEmpty(mapProviders)) {
			mapProviders = new MapProvider[] { new googleMapProvider(GOOGLE_MAP_IDBASE),
					new mfMapProvider(MAPSFORGE_MAP_ID_BASE) };
		}
		return mapProviders;
	}
	
	public static int getDefaultMapSourceId() {
		
		int result = 0;
		
		MapProvider[] mapProvs = getMapProviders();
		if (ArrayUtils.isNotEmpty(mapProvs)) {
			MapProvider mapProvider = mapProvs[0];
			int[] sourceIds = mapProvider.getSourceIds();
			if (ArrayUtils.isNotEmpty(sourceIds)) {
				result = sourceIds[0];
			}
		}
		
		return result;
	}
	
	public static String[] getNames(Resources res) {
		
		ArrayList<String> names = new ArrayList<String>();
		
		for (MapProvider mapProvider : getMapProviders()) {
			for (int sourceId : mapProvider.getSourceIds()) {
				names.add(mapProvider.getName(sourceId, res));
			}
		}
		return names.toArray(new String[]{});
	}
	
	public static int getIdFromOrdinal(int ordinal) {

		int count = 0;
		
		for (MapProvider mapProvider : getMapProviders()) {
			for (int sourceId : mapProvider.getSourceIds()) {
				if (count == ordinal) {
					return sourceId;
				} else {
					count++;
				}
			}
		}
		return 0;
	}
	
	public static int getOrdinalFromId(int sourceIdIn) {

		int count = 0;
		
		for (MapProvider mapProvider : getMapProviders()) {
			for (int sourceId : mapProvider.getSourceIds()) {
				if (sourceIdIn == sourceId) {
					return count;
				} else {
					count++;
				}
			}
		}
		return 0;
	}
	
	public static void addMenuItems(Menu menu, Resources res, int selectedSourceId) {
		for (MapProvider mapProvider : getMapProviders()) {
			for (int sourceId : mapProvider.getSourceIds()) {
				menu.add(1, sourceId, 0, mapProvider.getName(sourceId, res)).setCheckable(true).setChecked(selectedSourceId == sourceId);
			}
		}
		menu.setGroupCheckable(1, true, true);
	}

	public static boolean setMapProviderFromMenuItem(
			cgSettings settings,
			MapViewImpl mapView,
			MenuItem item) {

		int sourceId = item.getItemId();

		MapProvider mapProvider = getMapProviderFromId(sourceId);
		
		if (null != mapProvider) {
			return selectMapProvider(settings, mapView, mapProvider, sourceId);
		}
		return false;
	}

	public static boolean isMapSourceMenuItem(MenuItem item) {

		return isMapSourceId(item.getItemId());
	}

	public static boolean isMapSourceId(int sourceId) {

		return null != getMapProviderFromId(sourceId);
	}

	public static MapProvider getMapProviderFromId(int id) {
		for (MapProvider mapProvider : getMapProviders()) {
			if (mapProvider.hasSourceId(id)) {
				return mapProvider;
			}
		}
		return null;
	}

	private static boolean selectMapProvider(cgSettings settings, MapViewImpl mapView,
			MapProvider mapProvider, int sourceId) {

		int oldId = settings.getMapSourceId();
		
		boolean mapRestartRequired = !mapProvider.hasSourceId(oldId);
		
		settings.setMapSourceId(sourceId);

		if (!mapRestartRequired) {
			mapView.setMapSource(settings);
		}

		return mapRestartRequired;
	}	
}
