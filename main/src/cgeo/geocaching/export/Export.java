package cgeo.geocaching.export;

import cgeo.geocaching.cgCache;

import android.app.Activity;

import java.util.List;

/**
 * Represents an exporter to export a {@link List} of {@link cgCache} to various formats.
 */
interface Export {
    /**
     * Export a {@link List} of {@link cgCache} to various formats.
     *
     * @param caches
     *            The {@link List} of {@link cgCache} to be exported
     * @param activity
     *            optional: Some exporters might have an UI which requires an {@link Activity}
     */
    public void export(List<cgCache> caches, Activity activity);

    /**
     * Get the localized name of this exporter.
     *
     * @return localized name
     */
    public String getName();
}
