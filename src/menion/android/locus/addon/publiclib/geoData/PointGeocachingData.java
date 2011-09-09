package menion.android.locus.addon.publiclib.geoData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class PointGeocachingData implements Parcelable {
	
	private static final int VERSION = 2;
	
	public static final int CACHE_NUMBER_OF_TYPES = 14;
	
	public static final int CACHE_TYPE_TRADITIONAL = 0;
	public static final int CACHE_TYPE_MULTI = 1;
	public static final int CACHE_TYPE_MYSTERY = 2;
	public static final int CACHE_TYPE_VIRTUAL = 3;
	public static final int CACHE_TYPE_EARTH = 4;
	public static final int CACHE_TYPE_PROJECT_APE = 5;
	public static final int CACHE_TYPE_LETTERBOX = 6;
	public static final int CACHE_TYPE_WHERIGO = 7;
	public static final int CACHE_TYPE_EVENT = 8;
	public static final int CACHE_TYPE_MEGA_EVENT = 9;
	public static final int CACHE_TYPE_CACHE_IN_TRASH_OUT = 10;
	public static final int CACHE_TYPE_GPS_ADVENTURE = 11;
	public static final int CACHE_TYPE_WEBCAM = 12;
	public static final int CACHE_TYPE_LOCATIONLESS = 13;

	public static final int CACHE_LOG_TYPE_UNKNOWN = -1;
	public static final int CACHE_LOG_TYPE_FOUNDED = 0;
	public static final int CACHE_LOG_TYPE_NOT_FOUNDED = 1;
	public static final int CACHE_LOG_TYPE_WRITE_NOTE = 2;
	public static final int CACHE_LOG_TYPE_NEEDS_MAINTENANCE = 3;
	public static final int CACHE_LOG_TYPE_OWNER_MAINTENANCE = 4;
	public static final int CACHE_LOG_TYPE_PUBLISH_LISTING = 5;
	public static final int CACHE_LOG_TYPE_ENABLE_LISTING = 6;
	public static final int CACHE_LOG_TYPE_TEMPORARILY_DISABLE_LISTING = 7;
	public static final int CACHE_LOG_TYPE_UPDATE_COORDINATES = 8;
	public static final int CACHE_LOG_TYPE_ANNOUNCEMENT = 9;
	public static final int CACHE_LOG_TYPE_WILL_ATTEND = 10;
	public static final int CACHE_LOG_TYPE_ATTENDED = 11;
	public static final int CACHE_LOG_TYPE_POST_REVIEWER_NOTE = 12;
	public static final int CACHE_LOG_TYPE_NEEDS_ARCHIVED = 13;
	public static final int CACHE_LOG_TYPE_WEBCAM_PHOTO_TAKEN = 14;
	public static final int CACHE_LOG_TYPE_RETRACT_LISTING = 15;
	
	public static final int CACHE_SIZE_NOT_CHOSEN = 0;
	public static final int CACHE_SIZE_MICRO = 1;
	public static final int CACHE_SIZE_SMALL = 2;
	public static final int CACHE_SIZE_REGULAR = 3;
	public static final int CACHE_SIZE_LARGE = 4;
	public static final int CACHE_SIZE_HUGE = 5;
	public static final int CACHE_SIZE_OTHER = 6;
	
	public static final String CACHE_WAYPOINT_TYPE_QUESTION = "Question to Answer";
	public static final String CACHE_WAYPOINT_TYPE_FINAL = "Final Location";
	public static final String CACHE_WAYPOINT_TYPE_PARKING = "Parking Area";
	public static final String CACHE_WAYPOINT_TYPE_TRAILHEAD = "Trailhead";
	public static final String CACHE_WAYPOINT_TYPE_STAGES = "Stages of a Multicache";
	public static final String CACHE_WAYPOINT_TYPE_REFERENCE = "Reference Point";
	
	/* id of point - not needed as I remember */
	public int id;
	/* whole cache ID from gc.com - so GC... - !REQUIRED! */
	public String cacheID;
	/* is available or disable */
	public boolean available;
	/* cache already archived or not */
	public boolean archived;
	/* available only for premium members */
	public boolean premiumOnly;
	/* name of cache - !REQUIRED! */
	public String name;
	/* String with date of last update - groundspeak:lastUpdated*/
	public String lastUpdated;
	/* String with date of last exported - groundspeak:exported */
	public String exported;
	/* name of person who placed cache - groundspeak:placed_by */
	public String placedBy;
	/* name of cache owner - groundspeak:owner */
	public String owner;
	/* String with date of hidden - value from CachePrinter */
	public String hidden;
	/* cache type */
	public int type;
	/* container size */
	public int container;
	/* dificulty value - 1.0 - 5.0 (by 0.5) */
	public float difficulty;
	/* terrain value - 1.0 - 5.0 (by 0.5) */ 
	public float terrain;
	/* country name */
	public String country;
	/* state name */
	public String state;
	/* short description of cache */
	public String shortDescription;
	/* full description with complete (HTML) listing */
	public String longDescription;
	/* encoded hints */
	public String encodedHints;
	/* list of attributes */
	public ArrayList<PointGeocachingAttributes> attributes;
	/* list of logs */
	public ArrayList<PointGeocachingDataLog> logs;
	/* list of travel bugs */
	public ArrayList<PointGeocachingDataTravelBug> travelBugs;
	/* list of waypoints */
	public ArrayList<PointGeocachingDataWaypoint> waypoints;
	/* user notes */
	public String notes;
	/* if cache is already computed - have final waypoint and is placed on it's location */
	public boolean computed;
	/* if cache is already found */
	public boolean found;
	
	public PointGeocachingData() {
		id = 0;
		cacheID = "";
		available = true;
		archived = false;
		premiumOnly = false;
		name = "";
		lastUpdated = "";
		exported = "";
		placedBy = "";
		owner = "";
		hidden = "";
		type = CACHE_TYPE_TRADITIONAL;
		container = CACHE_SIZE_NOT_CHOSEN;
		difficulty = -1.0f;
		terrain = -1.0f;
		country = "";
		state = "";
		shortDescription = "";
		longDescription = "";
		encodedHints = "";
		attributes = new ArrayList<PointGeocachingAttributes>();
		logs = new ArrayList<PointGeocachingDataLog>();
		travelBugs = new ArrayList<PointGeocachingDataTravelBug>();
		waypoints = new ArrayList<PointGeocachingDataWaypoint>();
		notes = "";
		computed = false;
		found = false;
	}
	
	/****************************/
	/*      PARCELABLE PART     */
	/****************************/
	
    public static final Parcelable.Creator<PointGeocachingData> CREATOR = new Parcelable.Creator<PointGeocachingData>() {
        public PointGeocachingData createFromParcel(Parcel in) {
            return new PointGeocachingData(in);
        }

        public PointGeocachingData[] newArray(int size) {
            return new PointGeocachingData[size];
        }
    };
    
    @SuppressWarnings("unchecked")
	public PointGeocachingData(Parcel in) {
    	int version = in.readInt();
    	if (version == 0) {
    		 id = in.readInt();
    		 cacheID = in.readString();
    		 available = in.readInt() == 1;
    		 archived = in.readInt() == 1;
    		 premiumOnly = in.readInt() == 1;
    		 name = in.readString();
    		 lastUpdated = in.readString();
    		 exported = in.readString();
    		 placedBy = in.readString();
    		 owner = in.readString();
    		 hidden = in.readString();
    		 type = in.readInt();
    		 container = in.readInt();
    		 difficulty = in.readFloat();
    		 terrain = in.readFloat();
    		 country = in.readString();
    		 state = in.readString();
    		 shortDescription = in.readString();
    		 longDescription = in.readString();
    		 encodedHints = in.readString();
    		 attributes = in.readArrayList(PointGeocachingAttributes.class.getClassLoader());
    		 logs = in.readArrayList(PointGeocachingDataLog.class.getClassLoader());
    		 travelBugs = in.readArrayList(PointGeocachingDataTravelBug.class.getClassLoader());
    		 waypoints = in.readArrayList(PointGeocachingDataWaypoint.class.getClassLoader());
    		 notes = in.readString();
    		 computed = in.readInt() == 1;
    	} else if (version > 0) {
    		id = in.readInt();
   		 	cacheID = in.readString();
   		 	available = in.readInt() == 1;
   		 	archived = in.readInt() == 1;
   		 	premiumOnly = in.readInt() == 1;
   		 	name = in.readString();
   		 	lastUpdated = in.readString();
   		 	exported = in.readString();
   		 	placedBy = in.readString();
   		 	owner = in.readString();
   		 	hidden = in.readString();
   		 	type = in.readInt();
   		 	container = in.readInt();
   		 	difficulty = in.readFloat();
   		 	terrain = in.readFloat();
   		 	country = in.readString();
   		 	state = in.readString();
   		 	try {
   		 		int size = in.readInt();
   		 		int lengthSD = in.readInt();
   		 		
   		 		byte[] data = new byte[size];
   		 		in.readByteArray(data);

   		 		GZIPInputStream zis = new GZIPInputStream(new ByteArrayInputStream(data), 10240);
   		 		StringBuffer buffer = new StringBuffer();
   		 		
//   	   		    byte[] dataD = new byte[10240];
//   	   		    int bytesRead;
//   	   		    while ((bytesRead = zis.read(dataD)) != -1) {
//   	   		        buffer.append(new String(dataD, 0, bytesRead, "utf-8"));
//   	   		    }
//   		 		String result = buffer.toString();
//   		 		zis.close();
   		 		
   		 		InputStreamReader isr = new InputStreamReader(zis, "UTF-8");
   		 		char[] dataD = new char[1024];
   		 		int charsRead;
   		 		while ((charsRead = isr.read(dataD)) != -1) {
   		 			buffer.append(dataD, 0, charsRead);
   		 		}
   		 		String result = buffer.toString();
   		 		isr.close();
   		 		
   		 		// read short description
   		 		if (lengthSD > 0)
   		 			shortDescription = result.substring(0, lengthSD);
   		 		
   		 		// read long description
		 		longDescription = result.substring(lengthSD);
   		 	} catch (Exception e) {
   		 		Log.e("PointGeocachingData", "Problem in ZIP compression - read", e);
   		 	}
   		 	encodedHints = in.readString();
   		 	attributes = in.readArrayList(PointGeocachingAttributes.class.getClassLoader());
   		 	logs = in.readArrayList(PointGeocachingDataLog.class.getClassLoader());
   		 	travelBugs = in.readArrayList(PointGeocachingDataTravelBug.class.getClassLoader());
   		 	waypoints = in.readArrayList(PointGeocachingDataWaypoint.class.getClassLoader());
   		 	notes = in.readString();
   		 	computed = in.readInt() == 1;
   		 	if (version == 2) {
   		 		found = in.readInt() == 1;
   		 	}
    	}
    }

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(VERSION);
		dest.writeInt(id);
		dest.writeString(cacheID);
		dest.writeInt(available ? 1 : 0);
		dest.writeInt(archived ? 1 : 0);
		dest.writeInt(premiumOnly ? 1 : 0);
		dest.writeString(name);
		dest.writeString(lastUpdated);
		dest.writeString(exported);
		dest.writeString(placedBy);
		dest.writeString(owner);
		dest.writeString(hidden);
		dest.writeInt(type);
		dest.writeInt(container);
		dest.writeFloat(difficulty);
		dest.writeFloat(terrain);
		dest.writeString(country);
		dest.writeString(state);
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream zos = new GZIPOutputStream(baos);
			
			zos.write(shortDescription.getBytes("utf-8"));
			zos.write(longDescription.getBytes("utf-8"));
			zos.close();
			
			byte[] data = baos.toByteArray();
			baos.close();
			
			dest.writeInt(data.length);
			dest.writeInt(shortDescription.length());
			dest.writeByteArray(data);
		} catch (Exception e) {
			Log.e("PointGeocachingData", "Problem in ZIP compression - write", e);
		}

		dest.writeString(encodedHints);
		dest.writeList(attributes);
		dest.writeList(logs);
		dest.writeList(travelBugs);
		dest.writeList(waypoints);
		dest.writeString(notes);
		dest.writeInt(computed ? 1 : 0);
		dest.writeInt(found ? 1 : 0);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
}
