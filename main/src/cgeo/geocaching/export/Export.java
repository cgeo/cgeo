package cgeo.geocaching.export;

import cgeo.geocaching.Geocache;

import android.app.Activity;

import java.util.List;

/**
 * Represents an exporter to export a {@link List} of {@link cgeo.geocaching.Geocache} to various formats.
 */
interface Export {
    /**
     * Export a {@link List} of {@link cgeo.geocaching.Geocache} to various formats.
     *
     * @param caches
     *            The {@link List} of {@link cgeo.geocaching.Geocache} to be exported
     * @param activity
     *            optional: Some exporters might have an UI which requires an {@link Activity}
     */
    public void export(List<Geocache> caches, Activity activity);

    /**
     * Get the localized name of this exporter.
     *
     * @return localized name
     */
    public String getName();
}
