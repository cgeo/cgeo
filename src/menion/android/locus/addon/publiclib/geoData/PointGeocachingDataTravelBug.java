package menion.android.locus.addon.publiclib.geoData;

import android.os.Parcel;
import android.os.Parcelable;

public class PointGeocachingDataTravelBug implements Parcelable {
	
	private static final int VERSION = 0;
	
	/* name of travel bug */
	public String name;
	/* image url to this travel bug */
	public String imgUrl;
	/* original page data */
	public String srcDetails;
	
	/* owner of TB */
	public String owner;
	/* String date of release */
	public String released;
	/* origin place */
	public String origin;
	/* goal of this TB */
	public String goal;
	/* details */
	public String details;
	
	public PointGeocachingDataTravelBug() {
		name = "";
		imgUrl = "";
		srcDetails = "";
		
		owner = "";
		released = "";
		origin = "";
		goal = "";
		details = "";
	}

	/****************************/
	/*      PARCELABLE PART     */
	/****************************/
	
    public static final Parcelable.Creator<PointGeocachingDataTravelBug> CREATOR = new Parcelable.Creator<PointGeocachingDataTravelBug>() {
        public PointGeocachingDataTravelBug createFromParcel(Parcel in) {
            return new PointGeocachingDataTravelBug(in);
        }

        public PointGeocachingDataTravelBug[] newArray(int size) {
            return new PointGeocachingDataTravelBug[size];
        }
    };
    
    public PointGeocachingDataTravelBug(Parcel in) {
    	switch (in.readInt()) {
    	case 0:
    		name = in.readString();
    		imgUrl = in.readString();
    		srcDetails = in.readString();
    		owner = in.readString();
    		released = in.readString();
    		origin = in.readString();
    		goal = in.readString();
    		details = in.readString();
    		break;
    	}
    }
    
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(VERSION);
		dest.writeString(name);
		dest.writeString(imgUrl);
		dest.writeString(srcDetails);
		dest.writeString(owner);
		dest.writeString(released);
		dest.writeString(origin);
		dest.writeString(goal);
		dest.writeString(details);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
}