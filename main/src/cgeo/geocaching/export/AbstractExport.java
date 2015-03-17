package cgeo.geocaching.export;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

abstract class AbstractExport implements Export {
    private final String name;

    protected AbstractExport(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Generates a localized string from a resource id.
     *
     * @param resourceId
     *            the resource id of the string
     * @return localized string
     */
    protected static String getString(final int resourceId) {
        return CgeoApplication.getInstance().getString(resourceId);
    }

    /**
     * Generates a localized string from a resource id.
     *
     * @param resourceId
     *            the resource id of the string
     * @param params
     *            The parameter
     * @return localized string
     */
    protected static String getString(final int resourceId, final Object... params) {
        return CgeoApplication.getInstance().getString(resourceId, params);
    }

    @Override
    public String toString() {
        // used in the array adapter of the dialog showing the exports
        return getName();
    }

    protected String getProgressTitle() {
        return getString(R.string.export) + ": " + getName();
    }
}
