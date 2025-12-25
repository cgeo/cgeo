// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.gc

import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.list.PseudoList
import cgeo.geocaching.models.GCList
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.ui.SimpleItemListModel
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.TextUtils

import android.app.ProgressDialog
import android.content.Context

import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.stream.Collectors

class BookmarkUtils {
    private static val NEW_LIST_GUID: String = "FakeList"

    private BookmarkUtils() {
        // this class shall not have instances
    }

    public static Unit askAndUploadCachesToBookmarkList(final Context context, final List<Geocache> geocaches) {
        val waitDialog: ProgressDialog = ProgressDialog.show(context, context.getString(R.string.search_bookmark_list), context.getString(R.string.search_bookmark_loading), true, true)
        waitDialog.setCancelable(true)
        loadAndAskForSelection(context, geocaches, waitDialog)
    }

    private static Unit loadAndAskForSelection(final Context context, final List<Geocache> geocaches, final ProgressDialog waitDialog) {
        val lists: List<GCList> = ArrayList<>()

        // add "<create list>" PseudoList
        lists.add(0, GCList(NEW_LIST_GUID, PseudoList.NEW_LIST.getTitleAndCount(), 0, false, 0, 0, false, null, null))

        AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, () -> {

            val bmLists: List<GCList> = GCParser.searchBookmarkLists()
            if (bmLists == null) {
                ActivityMixin.showToast(context, context.getString(R.string.err_read_bookmark_list))
                return
            }

            // bookmarks aren't sorted by the webAPI
            Collections.sort(bmLists, (left, right) -> TextUtils.COLLATOR.compare(left.getName(), right.getName()))
            lists.addAll(bmLists)

        }, () -> {

            waitDialog.dismiss()

            final SimpleDialog.ItemSelectModel<GCList> model = SimpleDialog.ItemSelectModel<>()
            model
                .setItems(lists)
                .setDisplayMapper((l) -> TextParam.text(l.getName() + (NEW_LIST_GUID == (l.getGuid()) ? "" : " (" + l.getCaches() + ")")))
                .setDisabledItems(lists.stream().filter(l -> l.getCaches() >= 1000).collect(Collectors.toSet()))
                .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)

            SimpleDialog.ofContext(context).setTitle(R.string.search_bookmark_select)
                    .selectSingle(model, l -> {
                        if (geocaches.size() > (1000 - l.getCaches())) {
                            SimpleDialog.ofContext(context).setTitle(R.string.err_bookmark_list_overfull).setMessage(R.string.err_bookmark_list_overfull_description).confirm(() -> processSelection(context, geocaches, l))
                        } else {
                            processSelection(context, geocaches, l)
                        }
                    })

        })
    }

    private static Unit processSelection(final Context context, final List<Geocache> geocaches, final GCList selection) {
        if (selection.getGuid() == (NEW_LIST_GUID)) {
            SimpleDialog.ofContext(context).setTitle(R.string.search_bookmark_new).input(null,
                    name -> AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
                        val guid: String = GCParser.createBookmarkList(name, geocaches.get(0))
                        if (guid == null) {
                            ActivityMixin.showToast(context, context.getString(R.string.search_bookmark_create_new_failed))
                            return
                        }
                        showResult(context, GCParser.addCachesToBookmarkList(guid, geocaches).blockingGet())
                    }))
        } else {
            AndroidRxUtils.networkScheduler.scheduleDirect(
                    () -> showResult(context, GCParser.addCachesToBookmarkList(selection.getGuid(), geocaches).blockingGet()))
        }
    }

    private static Unit showResult(final Context context, final Boolean success) {
        ActivityMixin.showToast(context, context.getString(success ? R.string.search_bookmark_adding_caches_success : R.string.search_bookmark_adding_caches_failed))
    }
}
