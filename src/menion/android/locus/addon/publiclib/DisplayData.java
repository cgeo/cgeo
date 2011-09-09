package menion.android.locus.addon.publiclib;

import java.util.ArrayList;

import menion.android.locus.addon.publiclib.geoData.PointsData;
import menion.android.locus.addon.publiclib.utils.DataStorage;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DisplayData {

	private static final String TAG = "TAG";
	
	/**
	 * Simple way how to send data over intent to Locus. Count that intent in
	 * Android have some size limits so for larger data, use another method
	 * @param context actual context
	 * @param data PointsData object that should be send to Locus
	 * @param callImport wheather import with this data should be called after Locus starts
	 * @return true if success
	 */
	public static boolean sendData(Context context, PointsData data, boolean callImport) {
		if (data == null)
			return false;
		Intent intent = new Intent();
		intent.putExtra(LocusConst.EXTRA_POINTS_DATA, data);
		return sendData(context, intent, callImport);
	}
	
	/**
	 * Simple way how to send ArrayList<PointsData> object over intent to Locus. Count that
	 * intent in Android have some size limits so for larger data, use another method
	 * @param context actual context
	 * @param data ArrayList of data that should be send to Locus
	 * @return true if success
	 */
	public static boolean sendData(Context context, ArrayList<PointsData> data, boolean callImport) {
		if (data == null)
			return false;
		Intent intent = new Intent();
		intent.putParcelableArrayListExtra(LocusConst.EXTRA_POINTS_DATA_ARRAY, data);
		return sendData(context, intent, callImport);
	}
	
	/**
	 * Way how to send ArrayList<PointsData> object over intent to Locus. Data are
	 * stored in ContentProvider so don't forget to register it in manifest. More in
	 * sample application. This is recommended way how to send huge data to Locus 
	 * @param context actual context
	 * @param data ArrayList of data that should be send to Locus
	 * @param callImport wheather import with this data should be called after Locus starts
	 * @return true if success
	 */
	public static boolean sendDataCursor(Context context, PointsData data, String uri, boolean callImport) {
		if (data == null)
			return false;
		// set data
		DataStorage.setData(data);
		Intent intent = new Intent();
		intent.putExtra(LocusConst.EXTRA_POINTS_CURSOR_URI, uri);
		return sendData(context, intent, callImport);
	}
	
	/**
	 * Way how to send ArrayList<PointsData> object over intent to Locus. Data are
	 * stored in ContentProvider so don't forget to register it in manifest. More in
	 * sample application. This is recommended way how to send huge data to Locus 
	 * @param context actual context
	 * @param data ArrayList of data that should be send to Locus
	 * @return true if success
	 */
	public static boolean sendDataCursor(Context context, ArrayList<PointsData> data,
			String uri, boolean callImport) {
		if (data == null)
			return false;
		// set data
		DataStorage.setData(data);
		Intent intent = new Intent();
		intent.putExtra(LocusConst.EXTRA_POINTS_CURSOR_URI, uri);
		return sendData(context, intent, callImport);
	}

	// private final call to Locus
	private static boolean sendData(Context context, Intent intent, boolean callImport) {
		// really exist locus?
		if (!LocusUtils.isLocusAvailable(context)) {
			Log.w(TAG, "Locus is not installed");
			return false;
		}
		
		// check intent firstly
		if (intent == null || (intent.getParcelableArrayListExtra(LocusConst.EXTRA_POINTS_DATA_ARRAY) == null && 
				intent.getParcelableExtra(LocusConst.EXTRA_POINTS_DATA) == null &&
				intent.getStringExtra(LocusConst.EXTRA_POINTS_CURSOR_URI) == null)) {
			Log.w(TAG, "Intent 'null' or not contain any data");
			return false;
		}
		
		intent.putExtra(LocusConst.EXTRA_CALL_IMPORT, callImport);
		
		// create intent with right calling method
		intent.setAction(LocusConst.INTENT_DISPLAY_DATA);
		// finally start activity
		context.startActivity(intent);
		return true;
	}
}
