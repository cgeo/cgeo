package cgeo.geocaching.geopoint;

import cgeo.geocaching.Settings;

public class HumanDistance {
    public static String getHumanDistance(final Float distanceKilometers) {
        if (distanceKilometers == null) {
            return "?";
        }

        if (Settings.isUseMetricUnits()) {
            if (distanceKilometers > 100) {
                return String.format("%d", Math.round(distanceKilometers)) + " km";
            } else if (distanceKilometers > 10) {
                return String.format("%.1f", Double.valueOf(Math.round(distanceKilometers * 10.0) / 10.0)) + " km";
            } else if (distanceKilometers > 1) {
                return String.format("%.2f", Double.valueOf(Math.round(distanceKilometers * 100.0) / 100.0)) + " km";
            } else if (distanceKilometers > 0.1) {
                return String.format("%d", Math.round(distanceKilometers * 1000.0)) + " m";
            } else if (distanceKilometers > 0.01) {
                return String.format("%.1f", Double.valueOf(Math.round(distanceKilometers * 1000.0 * 10.0) / 10.0)) + " m";
            } else {
                return String.format("%.2f", Double.valueOf(Math.round(distanceKilometers * 1000.0 * 100.0) / 100.0)) + " m";
            }
        } else {
            final float miles = distanceKilometers / IConversion.MILES_TO_KILOMETER;
            if (miles > 100) {
                return String.format("%d", Math.round(miles)) + " mi";
            } else if (miles > 0.5) {
                return String.format("%.1f", Double.valueOf(Math.round(miles * 10.0) / 10.0)) + " mi";
            } else if (miles > 0.1) {
                return String.format("%.2f", Double.valueOf(Math.round(miles * 100.0) / 100.0)) + " mi";
            }

            final float feet = miles * 5280;
            if (feet >= 100) {
                return String.format("%d", Math.round(feet)) + " ft";
            } else if (feet >= 10) {
                return String.format("%.1f", Double.valueOf(Math.round(feet * 10.0) / 10.0)) + " ft";
            } else {
                return String.format("%.2f", Double.valueOf(Math.round(feet * 100.0) / 100.0)) + " ft";
            }
        }
    }
}
