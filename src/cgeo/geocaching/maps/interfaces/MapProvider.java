package cgeo.geocaching.maps.interfaces;

import android.app.Activity;
import android.content.res.Resources;

public interface MapProvider {
	
	public int[] getSourceIds();
	
	public String getName(int sourceId, Resources res);

	public boolean hasSourceId(int Id);

	public Class<?extends Activity> getMapClass();
	
	public MapFactory getMapFactory();
}
