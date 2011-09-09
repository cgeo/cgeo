package menion.android.locus.addon.publiclib.utils;

import java.util.ArrayList;

import menion.android.locus.addon.publiclib.geoData.PointsData;

public class DataStorage {

	private static ArrayList<PointsData> mData;
	
	public static ArrayList<PointsData> getData() {
		return mData;
	}
	
	public static void setData(ArrayList<PointsData> data) {
		DataStorage.mData = data;
	}
	
	public static void setData(PointsData data) {
		DataStorage.mData = new ArrayList<PointsData>();
		DataStorage.mData.add(data);
	}
	
	public static void clearData() {
		DataStorage.mData.clear();
		DataStorage.mData = null;
	}
}
