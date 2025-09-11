package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.models.GCList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.TextUtils;

import android.app.ProgressDialog;
import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookmarkUtils {
    private static final String NEW_LIST_GUID = "FakeList";

    private BookmarkUtils() {
        // this class shall not have instances
    }

    public static void askAndUploadCachesToBookmarkList(final Context context, final List<Geocache> geocaches) {
        final ProgressDialog waitDialog = ProgressDialog.show(context, context.getString(R.string.search_bookmark_list), context.getString(R.string.search_bookmark_loading), true, true);
        waitDialog.setCancelable(true);
        loadAndAskForSelection(context, geocaches, waitDialog);
    }

    private static void loadAndAskForSelection(final Context context, final List<Geocache> geocaches, final ProgressDialog waitDialog) {
        final List<GCList> lists = new ArrayList<>();

        // add "<create new list>" PseudoList
        lists.add(0, new GCList(NEW_LIST_GUID, PseudoList.NEW_LIST.getTitleAndCount(), 0, false, 0, 0, false, null, null));

        AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, () -> {

            final List<GCList> bmLists = GCParser.searchBookmarkLists();
            if (bmLists == null) {
                ActivityMixin.showToast(context, context.getString(R.string.err_read_bookmark_list));
                return;
            }

            // bookmarks aren't sorted by the webAPI
            Collections.sort(bmLists, (left, right) -> TextUtils.COLLATOR.compare(left.getName(), right.getName()));
            lists.addAll(bmLists);

        }, () -> {

            waitDialog.dismiss();

            final SimpleDialog.ItemSelectModel<GCList> model = new SimpleDialog.ItemSelectModel<>();
            model
                .setItems(lists)
                .setDisplayMapper((l) -> TextParam.text(l.getName()))
                .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

            SimpleDialog.ofContext(context).setTitle(R.string.search_bookmark_select)
                    .selectSingle(model, l -> processSelection(context, geocaches, l));

        });
    }

    private static void processSelection(final Context context, final List<Geocache> geocaches, final GCList selection) {
        if (selection.getGuid().equals(NEW_LIST_GUID)) {
            SimpleDialog.ofContext(context).setTitle(R.string.search_bookmark_new).input(null,
                    name -> AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
                        final String guid = GCParser.createBookmarkList(name, geocaches.get(0));
                        if (guid == null) {
                            ActivityMixin.showToast(context, context.getString(R.string.search_bookmark_create_new_failed));
                            return;
                        }
                        showResult(context, GCParser.addCachesToBookmarkList(guid, geocaches).blockingGet());
                    }));
        } else {
            AndroidRxUtils.networkScheduler.scheduleDirect(
                    () -> showResult(context, GCParser.addCachesToBookmarkList(selection.getGuid(), geocaches).blockingGet()));
        }
    }

    private static void showResult(final Context context, final boolean success) {
        ActivityMixin.showToast(context, context.getString(success ? R.string.search_bookmark_adding_caches_success : R.string.search_bookmark_adding_caches_failed));
    }
}
