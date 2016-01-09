package cgeo.geocaching.models;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.Dialogs.ItemWithIcon;

import org.apache.commons.collections4.CollectionUtils;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.support.annotation.DrawableRes;

import java.util.List;

import rx.android.app.AppObservable;
import rx.functions.Action1;

public final class PocketQueryList implements ItemWithIcon {

    private final String guid;
    private final int maxCaches;
    private final String name;
    private final boolean downloadable;

    public PocketQueryList(final String guid, final String name, final int maxCaches, final boolean downloadable) {
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

    public static void promptForListSelection(final Activity activity, final Action1<PocketQueryList> runAfterwards) {
        final Dialog waitDialog = ProgressDialog.show(activity, activity.getString(R.string.search_pocket_title), activity.getString(R.string.search_pocket_loading), true, true);

        AppObservable.bindActivity(activity, GCParser.searchPocketQueryListObservable).subscribe(new Action1<List<PocketQueryList>>() {
            @Override
            public void call(final List<PocketQueryList> pocketQueryLists) {
                waitDialog.dismiss();
                selectFromPocketQueries(activity, pocketQueryLists, runAfterwards);
            }
        });
    }

    private static void selectFromPocketQueries(final Activity activity, final List<PocketQueryList> pocketQueryList, final Action1<PocketQueryList> runAfterwards) {
        if (CollectionUtils.isEmpty(pocketQueryList)) {
            ActivityMixin.showToast(activity, activity.getString(R.string.warn_no_pocket_query_found));
            return;
        }

        Dialogs.select(activity, activity.getString(R.string.search_pocket_select), pocketQueryList, runAfterwards);
    }

    @Override
    @DrawableRes
    public int getIcon() {
        if (isDownloadable()) {
            return R.drawable.ic_menu_save;
        }
        return R.drawable.ic_menu_info_details;
    }

    @Override
    public String toString() {
        // used by AlertBuilder
        return getName();
    }

}
