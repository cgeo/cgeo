package cgeo.geocaching.models;

import cgeo.geocaching.R;

import android.support.annotation.DrawableRes;

public final class PocketQuery {

    private final String guid;
    private final int maxCaches;
    private final String name;
    private final boolean downloadable;

    public PocketQuery(final String guid, final String name, final int maxCaches, final boolean downloadable) {
        this.guid = guid;
        this.name = name;
        this.maxCaches = maxCaches;
        this.downloadable = downloadable;
    }

    public boolean isDownloadable() {
        return downloadable;
    }

    public String getGuid() {
        return guid;
    }

    public int getMaxCaches() {
        return maxCaches;
    }

    public String getName() {
        return name;
    }

    @DrawableRes
    public int getIcon() {
        if (isDownloadable()) {
            return R.drawable.ic_menu_save;
        }
        return R.drawable.ic_menu_info_details;
    }

}
