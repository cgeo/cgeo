package cgeo.geocaching.utils;

import android.content.res.Resources;

import cgeo.geocaching.CacheDetailActivity.Page;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

public class ZtnfnyUtils {
    public static int getTabIconFromTitle(final long pageId) {
        final Resources res = CgeoApplication.getInstance().getApplicationContext().getResources();
        if (pageId == Page.VARIABLES.id) {
            return R.drawable.cachedetails_variables;
        } else if (pageId == Page.WAYPOINTS.id) {
            return R.drawable.cachedetails_waypoints;
        } else if (pageId == Page.DETAILS.id) {
            return R.drawable.cachedetails_details;
        } else if (pageId == Page.DESCRIPTION.id) {
            return R.drawable.cachedetails_description;
        } else if (pageId == Page.LOGS.id) {
            return R.drawable.cachedetails_logbook;
        } else if (pageId == Page.LOGSFRIENDS.id) {
            return R.drawable.cachedetails_friends;
        } else if (pageId == Page.IMAGES.id) {
            return R.drawable.cachedetails_images;
        } else if (pageId == Page.INVENTORY.id) {
            return R.drawable.trackable_travelbug;
        } else {
            return R.drawable.cachedetails_details;
        }
    }

    public static int getTabIconBadge(final long pageId, final String tabTitle) {
        if (pageId == Page.WAYPOINTS.id || pageId == Page.VARIABLES.id) {
            return Integer.parseInt(tabTitle.replaceAll("\\w+ \\(([^<]*)\\)", "$1"));
        } else {
            return 0;
        }
    }

}
