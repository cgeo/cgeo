package cgeo.geocaching;

import cgeo.geocaching.geopoint.Geopoint;

public class cgDestination {

    private long id;

    private long date;

    private Geopoint coords;

    public cgDestination() {
    }

    public cgDestination(long id, long date, final Geopoint coords) {
        super();
        this.id = id;
        this.date = date;
        this.coords = coords;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public Geopoint getCoords() {
        return coords;
    }

    public void setCoords(final Geopoint coords) {
        this.coords = coords;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(coords.getLatitude());
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(coords.getLongitude());
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof cgDestination)) {
            return false;
        }
        cgDestination other = (cgDestination) obj;
        return coords.isEqualTo(other.coords);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

}
