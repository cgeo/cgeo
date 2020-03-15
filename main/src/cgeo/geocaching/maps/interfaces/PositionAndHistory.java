package cgeo.geocaching.maps.interfaces;


import cgeo.geocaching.maps.routing.Route;

import android.location.Location;

import java.util.ArrayList;

public interface PositionAndHistory extends Route.RouteUpdater {


    void setCoordinates(Location coordinatesIn);

    Location getCoordinates();

    void setHeading(float bearingNow);

    float getHeading();

    ArrayList<Location> getHistory();

    void setHistory(ArrayList<Location> history);

    void repaintRequired();

    void updateMapAutoRotation();
}
