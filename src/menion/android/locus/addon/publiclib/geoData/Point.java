package menion.android.locus.addon.publiclib.geoData;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

public class Point implements Parcelable {
	
	private static final int VERSION = 0;
	
	/* mName of object */
	private String mName;
	/* mDesc of object */
	private String mDesc;
	/* mLoc of this point */
	private Location mLoc;
	/* extra intent data */
	private String mExtraData;
	/* additional geoCaching data */
	private PointGeocachingData mGeoCachingData;
	
	public Point(String name, Location loc) {
		this.mName = name;
		this.mDesc = null;
		this.mLoc = loc;
		this.mExtraData = null;
		this.mGeoCachingData = null;
	}

	public String getName() {
		return mName;
	}
	
	public String getDescription() {
		return mDesc;
	}
	
	public void setDescription(String desc) {
		this.mDesc = desc;
	}
	
	public Location getLocation() {
		return mLoc;
	}
	
	public String getExtra() {
		return mExtraData;
	}
	
	/**
	 * Simply allow set callback value on point
	 * @param btnName Name displayed on button
	 * @param packageName this value is used for creating intent that
	 *  will be called in callback (for example com.super.application)
	 * @param className the name of the class inside of com.super.application
	 *  that implements the component (for example com.super.application.Main)
	 * @param returnDataName String uder which data will be stored. Can be
	 *  retrieved by String data = getIntent.getStringExtra("returnData");
	 * @param returnDataValue String uder which data will be stored. Can be
	 *  retrieved by String data = getIntent.getStringExtra("returnData");
	 */
	public void setCallback(String btnName, String packageName, String className,
			String returnDataName, String returnDataValue) {
		StringBuffer buff = new StringBuffer();
		buff.append("intent").append(";");
		buff.append(btnName).append(";");
		buff.append(packageName).append(";");
		buff.append(className).append(";");
		buff.append(returnDataName).append(";");
		buff.append(returnDataValue).append(";");
		this.mExtraData = buff.toString();
	}
	
	public PointGeocachingData getGeocachingData() {
		return mGeoCachingData;
	}
	
	public void setGeocachingData(PointGeocachingData gcData) {
		this.mGeoCachingData = gcData;
	}
	
	/****************************/
	/*      PARCELABLE PART     */
	/****************************/
	
    public static final Parcelable.Creator<Point> CREATOR = new Parcelable.Creator<Point>() {
        public Point createFromParcel(Parcel in) {
            return new Point(in);
        }

        public Point[] newArray(int size) {
            return new Point[size];
        }
    };
    
    private Point(Parcel in) {
    	switch (in.readInt()) {
    	case 0:
    		// load name
    		mName = in.readString();
    		// load description
    		mDesc = in.readString();
    		// load separate location
    		mLoc = new Location(in.readString());
    		mLoc.setTime(in.readLong());
    		mLoc.setLatitude(in.readDouble());
    		mLoc.setLongitude(in.readDouble());
    		mLoc.setAltitude(in.readDouble());
    		mLoc.setAccuracy(in.readFloat());
    		mLoc.setBearing(in.readFloat());
    		mLoc.setSpeed(in.readFloat());
    		// load extra data
    		mExtraData = in.readString();
    		// load geocaching data
    		mGeoCachingData = in.readParcelable(PointGeocachingData.class.getClassLoader());//PointGeocachingData.CREATOR.createFromParcel(in);
			break;
    	}
    }
    
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(VERSION);
		// write name
		dest.writeString(mName);
		// write description
		dest.writeString(mDesc);
		// write location as separate values (due to some problems with 'magic number'
		dest.writeString(mLoc.getProvider());
		dest.writeLong(mLoc.getTime());
		dest.writeDouble(mLoc.getLatitude());
		dest.writeDouble(mLoc.getLongitude());
		dest.writeDouble(mLoc.getAltitude());
		dest.writeFloat(mLoc.getAccuracy());
		dest.writeFloat(mLoc.getBearing());
		dest.writeFloat(mLoc.getSpeed());
		// write extra
		dest.writeString(mExtraData);
		// write geocaching data
		dest.writeParcelable(mGeoCachingData, flags);
	}
}
