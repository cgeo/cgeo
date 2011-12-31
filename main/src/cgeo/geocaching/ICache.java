/**
 *
 */
package cgeo.geocaching;

import cgeo.geocaching.enumerations.LogType;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Basic interface for caches
 *
 * @author blafoo
 *
 */
public interface ICache extends IBasicCache {

    /**
     * @return Displayed owner, might differ from the real owner
     */
    public String getOwner();

    /**
     * @return GC username of the owner
     */
    public String getOwnerReal();

    /**
     * @return Latitude, e.g. N 52° 12.345
     */
    public String getLatitude();

    /**
     * @return Longitude, e.g. E 9° 34.567
     */
    public String getLongitude();

    /**
     * @return true if the user is the owner of the cache, false else
     */
    public boolean isOwn();

    /**
     * @return true is the cache is archived, false else
     */
    public boolean isArchived();

    /**
     * @return true is the cache is a Premium Member cache only, false else
     */
    public boolean isMembersOnly();

    /**
     * @return Decrypted hint
     */
    public String getHint();

    /**
     * @return Description
     */
    public String getDescription();

    /**
     * @return Short Description
     */
    public String getShortDescription();


    /**
     * @return Id
     */
    public String getCacheId();

    /**
     * @return Guid
     */
    public String getGuid();

    /**
     * @return Location
     */
    public String getLocation();

    /**
     * @return Personal note
     */
    public String getPersonalNote();


    /**
     * @return true if the user gave a favorite point to the cache
     *
     */
    public boolean isFavorite();

    /**
     * @return number of favorite points
     *
     */
    public int getFavoritePoints();

    /**
     * @return true if the cache is on the watchlist of the user
     *
     */
    public boolean isWatchlist();

    /**
     * @return The date the cache has been hidden
     *
     */
    public Date getHiddenDate();

    /**
     * immutable list of attributes, never <code>null</code>
     * 
     * @return the list of attributes for this cache
     */
    public List<String> getAttributes();

    /**
     * @return the list of trackables in this cache
     */
    public List<cgTrackable> getInventory();

    /**
     * @return the list of spoiler images
     */
    public List<cgImage> getSpoilers();

    /**
     * @return a statistic how often the caches has been found, disabled, archived etc.
     */
    public Map<LogType, Integer> getLogCounts();

    /**
     * get the name for lexicographical sorting.
     *
     * @return normalized, cached name which sort also correct for numerical parts in the name
     */
    public String getNameForSorting();
}
