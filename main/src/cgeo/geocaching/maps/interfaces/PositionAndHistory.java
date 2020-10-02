package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.models.ManualRoute;
import cgeo.geocaching.models.TrailHistoryElement;

import android.location.Location;

import java.util.ArrayList;

public interface PositionAndHistory extends ManualRoute.UpdateManualRoute {


    void setCoordinates(Location coordinatesIn);

    Location getCoordinates();

    void setHeading(float bearingNow);

    float getHeading();

    ArrayList<TrailHistoryElement> getHistory();

    void setHistory(ArrayList<TrailHistoryElement> history);

    void repaintRequired();

    void updateMapRotation();
}
