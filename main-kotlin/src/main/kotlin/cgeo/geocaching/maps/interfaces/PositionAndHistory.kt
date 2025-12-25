// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.maps.interfaces

import cgeo.geocaching.models.IndividualRoute
import cgeo.geocaching.models.TrailHistoryElement

import android.location.Location

import java.util.ArrayList

import com.google.android.gms.maps.model.LatLng

interface PositionAndHistory : IndividualRoute().UpdateIndividualRoute {


    Unit setCoordinates(Location coordinatesIn)

    Location getCoordinates()

    Unit setHeading(Float bearingNow)

    Float getHeading()

    Unit setLongTapLatLng(LatLng latLng)

    LatLng getLongTapLatLng()

    Unit resetLongTapLatLng()

    ArrayList<TrailHistoryElement> getHistory()

    Unit setHistory(ArrayList<TrailHistoryElement> history)

    Unit repaintRequired()

    Unit updateMapRotation()
}
