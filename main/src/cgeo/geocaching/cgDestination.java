package cgeo.geocaching;

import cgeo.geocaching.geopoint.Geopoint;

public class cgDestination implements ICoordinates {

    final private long id;
    final private long date;
    final private Geopoint coords;

    public cgDestination(long id, long date, final Geopoint coords) {
        this.id = id;
        this.date = date;
        this.coords = coords;
    }

    public cgDestination withDate(final long date) {
        return new cgDestination(id, date, coords);
    }

    public long getDate() {
        return date;
    }

    @Override
    public Geopoint getCoords() {
        return coords;
    }

    @Override
    public int hashCode() {
        return coords.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof cgDestination && ((cgDestination) obj).coords.equals(coords);
    }

    public long getId() {
        return id;
    }

}
