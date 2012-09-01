package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;

public enum GpxScanChoice {
    FORCE_SCAN(R.string.gpx_import_force_scan),
    DEFAULT_DIR(R.string.gpx_import_only_default);

    private int labelId;

    GpxScanChoice(int labelId) {
        this.labelId = labelId;
    }

    public int getLabelId() {
        return this.labelId;
    }

    @Override
    public String toString() {
        return cgeoapplication.getInstance().getString(this.labelId);
    }
}
