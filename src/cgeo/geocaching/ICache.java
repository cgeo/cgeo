/**
 * 
 */
package cgeo.geocaching;


/**
 * Basic interface for caches
 * @author blafoo
 *
 */
public interface ICache {
	
	/**
	 * @return The data returned by the HTTP-GET request for this cache. Only for testing purposes. 
	 */
	public String getData();
	
	/**
	 * @return Geocode like GCxxxx
	 */
	public String getGeocode();
	/**
	 * @return
	 */
	public String getType();
	/**
	 * @return
	 */
	public String getOwner();
	/**
	 * @return
	 */
	public String getSize();
	/**
	 * @return
	 */
	public Float getDifficulty();
	/**
	 * @return
	 */
	public Float getTerrain();
	/**
	 * @return
	 */
	public String getLatitute();
	/**
	 * @return
	 */
	public String getLongitude();
	/**
	 * @return
	 */
	public boolean isDisabled();
	/**
	 * @return
	 */
	public boolean isOwn();
	/**
	 * @return
	 */
	public boolean isArchived();
	/**
	 * @return
	 */
	public boolean isMembersOnly();

}
