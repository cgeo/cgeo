package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.NonNull;

abstract class AbstractExport implements Export {
    private final String name;
    private final String progressTitle;

    protected AbstractExport(final int name) {
        this.name = LocalizationUtils.getString(name);
        progressTitle = LocalizationUtils.getString(R.string.export_progress, this.name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public String toString() {
        // used in the array adapter of the dialog showing the exports
        return getName();
    }

    protected String getProgressTitle() {
        return progressTitle;
    }
}
