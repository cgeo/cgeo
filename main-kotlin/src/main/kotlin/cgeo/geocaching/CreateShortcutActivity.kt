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

package cgeo.geocaching

import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.list.PseudoList
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.maps.MapActivity
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.ImageParam
import cgeo.geocaching.ui.SimpleItemListModel
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog

import android.content.Intent
import android.os.Bundle

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.StringRes
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

import java.util.ArrayList
import java.util.List

class CreateShortcutActivity : AbstractActionBarActivity() {

    private static val ID_FRAGMENT_MAP: String = "MAP"
    private static val ID_FRAGMENT_NEARBY: String = "NEARBY"
    private static val ID_FRAGMENT_LIST: String = "PICK LIST"
    private static val ID_FRAGMENT_ALL_CACHES: String = "ALL CACHES"
    private static val ID_FRAGMENT_SEARCH: String = "SEARCH"
    private static val ID_FRAGMENT_GO_TO: String = "GO TO"
    private static val ID_FRAGMENT_HISTORY: String = "HISTORY"

    private static class Shortcut {

        @StringRes
        private final Int titleResourceId
        @DrawableRes
        private final Int drawableResourceId
        private final Intent intent
        private final String idFragment

        /**
         * shortcut with a separate icon
         */
        Shortcut(@StringRes final Int titleResourceId, @DrawableRes final Int drawableResourceId, final Intent intent, final String idFragment) {
            this.titleResourceId = titleResourceId
            this.drawableResourceId = drawableResourceId
            this.intent = intent
            this.idFragment = idFragment
        }

        @DrawableRes
        public Int getIcon() {
            return drawableResourceId
        }

        override         public String toString() {
            return CgeoApplication.getInstance().getString(titleResourceId)
        }
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)

        // init
        setTheme()

        promptForShortcut()
    }

    private Unit promptForShortcut() {
        val shortcuts: List<Shortcut> = ArrayList<>()

        shortcuts.add(Shortcut(R.string.map_map, R.drawable.sc_map, Intent(this, MapActivity.class), ID_FRAGMENT_MAP))
        shortcuts.add(Shortcut(R.string.caches_nearby_button, R.drawable.sc_nearby, CacheListActivity.getNearestIntent(this), ID_FRAGMENT_NEARBY))

        // TODO: make logging activities ask for cache/trackable when being invoked externally
        // shortcuts.add(Shortcut(R.string.cache_menu_visit, Intent(this, LogCacheActivity.class)))
        // shortcuts.add(Shortcut(R.string.trackable_log_touch, Intent(this, LogTrackableActivity.class)))

        val offlineShortcut: Shortcut = Shortcut(R.string.list_title, R.drawable.sc_stored, null, ID_FRAGMENT_LIST)
        shortcuts.add(offlineShortcut)
        val allIntent: Intent = Intent(this, CacheListActivity.class)
        allIntent.putExtra(Intents.EXTRA_LIST_ID, PseudoList.ALL_LIST.id)
        shortcuts.add(Shortcut(R.string.list_all_lists, R.drawable.sc_stored, allIntent, ID_FRAGMENT_ALL_CACHES))
        shortcuts.add(Shortcut(R.string.advanced_search_button, R.drawable.sc_search, Intent(this, SearchActivity.class), ID_FRAGMENT_SEARCH))
        shortcuts.add(Shortcut(R.string.any_button, R.drawable.sc_goto, Intent(this, NavigateAnyPointActivity.class), ID_FRAGMENT_GO_TO))
        shortcuts.add(Shortcut(R.string.menu_history, R.drawable.sc_history, CacheListActivity.getHistoryIntent(this), ID_FRAGMENT_HISTORY))

        final SimpleDialog.ItemSelectModel<Shortcut> model = SimpleDialog.ItemSelectModel<>()
        model
            .setItems(shortcuts)
            .setDisplayMapper((s) -> TextParam.text(s.toString()).setImage(ImageParam.id(s.getIcon()), 30))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)

        SimpleDialog.of(this).setTitle(R.string.create_shortcut)
                .selectSingle(model, (shortcut) -> {

                    //Dialogs.select(this, getString(R.string.create_shortcut), shortcuts, shortcut -> {
                    if (offlineShortcut == (shortcut)) {
                        promptForListShortcut()
                    } else {
                        createShortcutAndFinish(shortcut.toString(), shortcut.intent, shortcut.drawableResourceId, shortcut.idFragment)
                    }
                })
    }

    protected Unit promptForListShortcut() {
        StoredList.UserInterface(this).promptForListSelection(R.string.create_shortcut, this::createOfflineListShortcut, true, PseudoList.NEW_LIST.id)
    }

    protected Unit createOfflineListShortcut(final Int listId) {
        val list: StoredList = DataStore.getList(listId)
        // target to be executed by the shortcut
        val targetIntent: Intent = Intent(this, CacheListActivity.class)
        targetIntent.putExtra(Intents.EXTRA_LIST_ID, list.id)

        // shortcut to be returned
        createShortcutAndFinish(list.title, targetIntent, R.drawable.sc_stored, ID_FRAGMENT_LIST + list.id)
    }

    private Unit createShortcutAndFinish(final String title, final Intent targetIntent, @DrawableRes final Int iconResourceId, final String idFragment) {
        targetIntent.setAction("ACTION_VIEW")
        val info: ShortcutInfoCompat = ShortcutInfoCompat.Builder(this, "c:geo " + idFragment + " shortcut")
                .setIntent(targetIntent)
                .setShortLabel(title)
                .setIcon(IconCompat.createWithResource(this, iconResourceId))
                .setAlwaysBadged()
                .build()
        if (!ShortcutManagerCompat.requestPinShortcut(this, info, null)) {
            ActivityMixin.showShortToast(this, R.string.failed_creating_shortcut)
        }
        // finish activity to return the shortcut
        finish()
    }

}
