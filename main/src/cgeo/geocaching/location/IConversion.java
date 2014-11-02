package cgeo.geocaching.location;

public interface IConversion {
    public static final float MILES_TO_KILOMETER = 1.609344f;
    public static final float FEET_TO_KILOMETER = 0.0003048f;
    public static final float YARDS_TO_KILOMETER = 0.0009144f;
    /**
     * Factor used to calculate distance from meters to foot;
     * <p>
     * ft = m * METERS_TO_FEET;
     */
    public static final double METERS_TO_FEET = 3.2808399d;
}
