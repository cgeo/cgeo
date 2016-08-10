package cgeo.geocaching.maps.interfaces;


import android.location.Location;

import java.util.ArrayList;

public interface PositionAndHistory {


    void setCoordinates(Location coordinatesIn);

    Location getCoordinates();

    void setHeading(float bearingNow);

    float getHeading();

    ArrayList<Location> getHistory();

    void setHistory(ArrayList<Location> history);

    void repaintRequired();
}
