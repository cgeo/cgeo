package cgeo.geocaching.export;

import cgeo.geocaching.cgeoapplication;

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
    protected static String getString(int resourceId) {
        return cgeoapplication.getInstance().getString(resourceId);
    }

    @Override
    public String toString() {
        // used in the array adapter of the dialog showing the exports
        return getName();
    }
}
