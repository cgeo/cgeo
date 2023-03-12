package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.location.Geopoint;

public interface ToScreenProjector {

    int[] project(Geopoint t);
}
