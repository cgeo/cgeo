/**
 *
 */
package cgeo.geocaching;

public interface ILogable {

    /**
     * @return Geocode like GCxxxx
     */
    public abstract String getGeocode();

    /**
     * @return Name
     */
    public abstract String getName();

}
