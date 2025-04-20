package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.CoordinateType;

public interface INamedGeoCoordinate extends IGeoObject, ICoordinate {

    CoordinateType getCoordType();

}
