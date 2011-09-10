package cgeo.geocaching;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.text.Spannable;

public class cgTrackable {
	static final public int SPOTTED_UNSET = 0;
	static final public int SPOTTED_CACHE = 1;
	static final public int SPOTTED_USER = 2;
	static final public int SPOTTED_UNKNOWN = 3;
	static final public int SPOTTED_OWNER = 4;
	
	public int errorRetrieve = 0;
	public String error = "";
	public String guid = "";
	public String geocode = "";
	public String iconUrl = "";
	public String name = "";
	public String nameString = null;
	public Spannable nameSp = null;
	public String type = null;
	public Date released = null;
	public Double distance = null;
	public String origin = null;
	public String owner = null;
	public String ownerGuid = null;
	public String spottedName = null;
	public int spottedType = SPOTTED_UNSET;
	public String spottedGuid = null;
	public String goal = null;
	public String details = null;
	public String image = null;
	public List<cgLog> logs = new ArrayList<cgLog>();
}
