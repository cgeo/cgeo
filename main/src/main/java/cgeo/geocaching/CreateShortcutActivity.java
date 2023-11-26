package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.maps.MapActivity;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.ImageUtils;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;
import java.util.List;

public class CreateShortcutActivity extends AbstractActionBarActivity {

    private static final String ID_FRAGMENT_MAP = "MAP";
    private static final String ID_FRAGMENT_NEARBY = "NEARBY";
    private static final String ID_FRAGMENT_LIST = "PICK LIST";
    private static final String ID_FRAGMENT_ALL_CACHES = "ALL CACHES";
    private static final String ID_FRAGMENT_SEARCH = "SEARCH";
    private static final String ID_FRAGMENT_GO_TO = "GO TO";
    private static final String ID_FRAGMENT_HISTORY = "HISTORY";

    private static class Shortcut {

        @StringRes
        private final int titleResourceId;
        @DrawableRes
        private final int drawableResourceId;
        private final Intent intent;
        private final String idFragment;

        /**
         * shortcut with a separate icon
         */
        Shortcut(@StringRes final int titleResourceId, @DrawableRes final int drawableResourceId, final Intent intent, final String idFragment) {
            this.titleResourceId = titleResourceId;
            this.drawableResourceId = drawableResourceId;
            this.intent = intent;
            this.idFragment = idFragment;
        }

        @DrawableRes
        public int getIcon() {
            return drawableResourceId;
        }

        @Override
        @NonNull
        public String toString() {
            return CgeoApplication.getInstance().getString(titleResourceId);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init
        setTheme();

        promptForShortcut();
    }

    private void promptForShortcut() {
        final List<Shortcut> shortcuts = new ArrayList<>();

        shortcuts.add(new Shortcut(R.string.map_map, R.drawable.sc_map, new Intent(this, MapActivity.class), ID_FRAGMENT_MAP));
        shortcuts.add(new Shortcut(R.string.caches_nearby_button, R.drawable.sc_nearby, CacheListActivity.getNearestIntent(this), ID_FRAGMENT_NEARBY));

        // TODO: make logging activities ask for cache/trackable when being invoked externally
        // shortcuts.add(new Shortcut(R.string.cache_menu_visit, new Intent(this, LogCacheActivity.class)));
        // shortcuts.add(new Shortcut(R.string.trackable_log_touch, new Intent(this, LogTrackableActivity.class)));

        final Shortcut offlineShortcut = new Shortcut(R.string.list_title, R.drawable.sc_stored, null, ID_FRAGMENT_LIST);
        shortcuts.add(offlineShortcut);
        final Intent allIntent = new Intent(this, CacheListActivity.class);
        allIntent.putExtra(Intents.EXTRA_LIST_ID, PseudoList.ALL_LIST.id);
        shortcuts.add(new Shortcut(R.string.list_all_lists, R.drawable.sc_stored, allIntent, ID_FRAGMENT_ALL_CACHES));
        shortcuts.add(new Shortcut(R.string.advanced_search_button, R.drawable.sc_search, new Intent(this, SearchActivity.class), ID_FRAGMENT_SEARCH));
        shortcuts.add(new Shortcut(R.string.any_button, R.drawable.sc_goto, new Intent(this, NavigateAnyPointActivity.class), ID_FRAGMENT_GO_TO));
        shortcuts.add(new Shortcut(R.string.menu_history, R.drawable.sc_history, CacheListActivity.getHistoryIntent(this), ID_FRAGMENT_HISTORY));

        final SimpleDialog.ItemSelectModel<Shortcut> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(shortcuts)
            .setDisplayMapper((s) -> TextParam.text(s.toString()).setImage(ImageParam.id(s.getIcon()), 30))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

        SimpleDialog.of(this).setTitle(R.string.create_shortcut)
                .selectSingle(model, (shortcut) -> {

                    //Dialogs.select(this, getString(R.string.create_shortcut), shortcuts, shortcut -> {
                    if (offlineShortcut.equals(shortcut)) {
                        promptForListShortcut();
                    } else {
                        createShortcutAndFinish(shortcut.toString(), shortcut.intent, shortcut.drawableResourceId, shortcut.idFragment);
                    }
                });
    }

    protected void promptForListShortcut() {
        new StoredList.UserInterface(this).promptForListSelection(R.string.create_shortcut, this::createOfflineListShortcut, true, PseudoList.NEW_LIST.id);
    }

    protected void createOfflineListShortcut(final int listId) {
        final StoredList list = DataStore.getList(listId);
        // target to be executed by the shortcut
        final Intent targetIntent = new Intent(this, CacheListActivity.class);
        targetIntent.putExtra(Intents.EXTRA_LIST_ID, list.id);

        // shortcut to be returned
        createShortcutAndFinish(list.title, targetIntent, R.drawable.sc_stored, ID_FRAGMENT_LIST + list.id);
    }

    private void createShortcutAndFinish(final String title, final Intent targetIntent, @DrawableRes final int iconResourceId, final String idFragment) {
        targetIntent.setAction("ACTION_VIEW");
        final ShortcutInfoCompat info = new ShortcutInfoCompat.Builder(this, "c:geo " + idFragment + " shortcut")
                .setIntent(targetIntent)
                .setShortLabel(title)
                .setIcon(IconCompat.createWithAdaptiveBitmap(createOverlay(iconResourceId)))
                .build();
        if (!ShortcutManagerCompat.requestPinShortcut(this, info, null)) {
            ActivityMixin.showShortToast(this, R.string.failed_creating_shortcut);
        }
        // finish activity to return the shortcut
        finish();
    }

    private Bitmap createOverlay(@DrawableRes final int drawableResourceId) {
        final LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{
                ResourcesCompat.getDrawable(res, drawableResourceId, null),
                ResourcesCompat.getDrawable(res, R.drawable.cgeo_borderless, null)
        });
        layerDrawable.setLayerInset(1, 140, 140, 0, 0);
        return ImageUtils.convertToBitmap(layerDrawable);
    }

}
