package cgeo.geocaching.location;

public interface IConversion {
    float MILES_TO_KILOMETER = 1.609344f;
    float FEET_TO_KILOMETER = 0.0003048f;
    float YARDS_TO_KILOMETER = 0.0009144f;
    float FEET_TO_METER = 0.3048f;

    /**
     * Factor used to calculate distance from meters to foot;
     * <p>
     * ft = m * METERS_TO_FEET;
     */
    double METERS_TO_FEET = 3.2808399d;
}
