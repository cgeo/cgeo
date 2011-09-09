/****************************************************************************
 *
 * Copyright (C) 2009-2010 Menion. All rights reserved.
 *
 * This file is part of the LocA & SmartMaps software.
 *
 * Email menion@asamm.cz for more information.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 *
 ***************************************************************************/
package menion.android.locus.addon.publiclib.geoData;

import java.util.Hashtable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author menion
 * @since 25.1.2010 2010
 */
public class PointGeocachingAttributes implements Parcelable {

	private static final int VERSION = 0;
	
	public int id;

	public PointGeocachingAttributes() {
		id = 0;
	}
	
	private static Hashtable<String, Integer> attrIds = new Hashtable<String, Integer>();
	static {
		attrIds.put("http://www.geocaching.com/images/attributes/dogs", 1);
		attrIds.put("http://www.geocaching.com/images/attributes/fee", 2);
		attrIds.put("http://www.geocaching.com/images/attributes/rappelling", 3);
		attrIds.put("http://www.geocaching.com/images/attributes/boat", 4);
		attrIds.put("http://www.geocaching.com/images/attributes/scuba", 5);
		attrIds.put("http://www.geocaching.com/images/attributes/kids", 6);
		attrIds.put("http://www.geocaching.com/images/attributes/onehour", 7);
		attrIds.put("http://www.geocaching.com/images/attributes/scenic", 8);
		attrIds.put("http://www.geocaching.com/images/attributes/hiking", 9);
		attrIds.put("http://www.geocaching.com/images/attributes/climbing", 10);
		attrIds.put("http://www.geocaching.com/images/attributes/wading", 11);
		attrIds.put("http://www.geocaching.com/images/attributes/swimming", 12);
		attrIds.put("http://www.geocaching.com/images/attributes/available", 13);
		attrIds.put("http://www.geocaching.com/images/attributes/night", 14);
		attrIds.put("http://www.geocaching.com/images/attributes/winter", 15);
		attrIds.put("http://www.geocaching.com/images/attributes/camping", 16);
		attrIds.put("http://www.geocaching.com/images/attributes/poisonoak", 17);
		attrIds.put("http://www.geocaching.com/images/attributes/snakes", 18);
		attrIds.put("http://www.geocaching.com/images/attributes/ticks", 19);
		attrIds.put("http://www.geocaching.com/images/attributes/mine", 20);
		attrIds.put("http://www.geocaching.com/images/attributes/cliff", 21);
		attrIds.put("http://www.geocaching.com/images/attributes/hunting", 22);
		attrIds.put("http://www.geocaching.com/images/attributes/danger", 23);
		attrIds.put("http://www.geocaching.com/images/attributes/wheelchair", 24);
		attrIds.put("http://www.geocaching.com/images/attributes/parking", 25);
		attrIds.put("http://www.geocaching.com/images/attributes/public", 26);
		attrIds.put("http://www.geocaching.com/images/attributes/water", 27);
		attrIds.put("http://www.geocaching.com/images/attributes/restrooms", 28);
		attrIds.put("http://www.geocaching.com/images/attributes/phone", 29);
		attrIds.put("http://www.geocaching.com/images/attributes/picnic", 30);
		attrIds.put("http://www.geocaching.com/images/attributes/camping", 31);
		attrIds.put("http://www.geocaching.com/images/attributes/bicycles", 32);
		attrIds.put("http://www.geocaching.com/images/attributes/motorcycles", 33);
		attrIds.put("http://www.geocaching.com/images/attributes/quads", 34);
		attrIds.put("http://www.geocaching.com/images/attributes/jeeps", 35);
		attrIds.put("http://www.geocaching.com/images/attributes/snowmobiles", 36);
		attrIds.put("http://www.geocaching.com/images/attributes/horses", 37);
		attrIds.put("http://www.geocaching.com/images/attributes/campfires", 38);
		attrIds.put("http://www.geocaching.com/images/attributes/thorn",39 );
		attrIds.put("http://www.geocaching.com/images/attributes/stealth", 40); 
		attrIds.put("http://www.geocaching.com/images/attributes/stroller", 41);
		attrIds.put("http://www.geocaching.com/images/attributes/firstaid", 42);
		attrIds.put("http://www.geocaching.com/images/attributes/cow", 43);
		attrIds.put("http://www.geocaching.com/images/attributes/flashlight", 44);
	}
	
	public void setByImgUrl(String url) {
		if (url != null && url.length() > 0) {
			id = attrIds.get(url.substring(0, url.lastIndexOf("-")));
			if (url.contains("-yes."))
				id += 100;
		}
	}
	
	/****************************/
	/*      PARCELABLE PART     */
	/****************************/
	
    public static final Parcelable.Creator<PointGeocachingAttributes> CREATOR = new Parcelable.Creator<PointGeocachingAttributes>() {
        public PointGeocachingAttributes createFromParcel(Parcel in) {
            return new PointGeocachingAttributes(in);
        }

        public PointGeocachingAttributes[] newArray(int size) {
            return new PointGeocachingAttributes[size];
        }
    };
    
    public PointGeocachingAttributes(Parcel in) {
    	switch (in.readInt()) {
    	case 0:
    		id = in.readInt();
    		break;
    	}
    }
    
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(VERSION);
		dest.writeInt(id);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
}
