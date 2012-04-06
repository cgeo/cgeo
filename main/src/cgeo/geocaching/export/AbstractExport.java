package cgeo.geocaching.export;

import cgeo.geocaching.cgeoapplication;

public abstract class AbstractExport implements Export {
    private String name;

    protected AbstractExport(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    protected static String getString(int ressourceId) {
        return cgeoapplication.getInstance().getString(ressourceId);
    }
}
