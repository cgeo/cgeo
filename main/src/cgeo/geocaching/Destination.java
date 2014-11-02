package cgeo.geocaching;

import cgeo.geocaching.location.Geopoint;

public final class Destination implements ICoordinates {

    final private long id;
    final private long date;
    final private Geopoint coords;

    public Destination(long id, long date, final Geopoint coords) {
        this.id = id;
        this.date = date;
        this.coords = coords;
    }

    public Destination(final Geopoint coords) {
        this(0, System.currentTimeMillis(), coords);
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
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Destination)) {
            return false;
        }
        return ((Destination) obj).coords.equals(coords);
    }

    public long getId() {
        return id;
    }

}
