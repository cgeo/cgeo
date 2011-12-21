/**
 *
 */
package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;


/**
 * @author blafoo
 *
 */
public interface IBasicCache extends ILogable {

    public abstract String getGuid();

    /**
     * @return Tradi, multi etc.
     */
    public abstract CacheType getType();

    /**
     * @return Micro, small etc.
     */
    public abstract CacheSize getSize();

    /**
     * @return true if the user already found the cache
     *
     */
    public abstract boolean isFound();

    /**
     * @return true if the cache is disabled, false else
     */
    public abstract boolean isDisabled();

    /**
     * @return Difficulty assessment
     */
    public abstract Float getDifficulty();

    /**
     * @return Terrain assessment
     */
    public abstract Float getTerrain();



}
