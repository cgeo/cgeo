package cgeo.geocaching.models;

public interface ILogable {

    /**
     * @return Geocode like GCxxxx
     */
    String getGeocode();

    /**
     * @return Name
     */
    String getName();

}
