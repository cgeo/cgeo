package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.Dialogs.ItemWithIcon;

import rx.functions.Action1;

import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class CreateShortcutActivity extends AbstractActionBarActivity {

    private static class Shortcut implements ItemWithIcon {

        private final int titleResourceId;
        private final int drawableResourceId;
        private final Intent intent;

        /**
         * shortcut with a separate icon
         */
        public Shortcut(final int titleResourceId, final int drawableResourceId, final Intent intent) {
            this.titleResourceId = titleResourceId;
            this.drawableResourceId = drawableResourceId;
            this.intent = intent;
        }

        /**
         * shortcut with generic c:geo icon
         */
        public Shortcut(final int titleResourceId, final Intent intent) {
            this(titleResourceId, R.drawable.cgeo, intent);
        }

        @Override
        public int getIcon() {
            return drawableResourceId;
        }

        @Override
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

        // TODO: The shortcut invokes the Google/Mapsforge selection which was active at time of shortcut creation.
        // We need a real LiveMap activity to be able to dynamically select the used renderer
        // shortcuts.add(new Shortcut(R.string.live_map_button, R.drawable.main_live, CGeoMap.getLiveMapIntent(this)));
        shortcuts.add(new Shortcut(R.string.caches_nearby_button, R.drawable.main_nearby, CacheListActivity.getNearestIntent(this)));

        // TODO: make logging activities ask for cache/trackable when being invoked externally
        // shortcuts.add(new Shortcut(R.string.cache_menu_visit, new Intent(this, LogCacheActivity.class)));
        // shortcuts.add(new Shortcut(R.string.trackable_log_touch, new Intent(this, LogTrackableActivity.class)));

        final Shortcut offlineShortcut = new Shortcut(R.string.stored_caches_button, R.drawable.main_stored, null);
        shortcuts.add(offlineShortcut);
        shortcuts.add(new Shortcut(R.string.advanced_search_button, R.drawable.main_search, new Intent(this, SearchActivity.class)));
        shortcuts.add(new Shortcut(R.string.any_button, R.drawable.main_any, new Intent(this, NavigateAnyPointActivity.class)));
        shortcuts.add(new Shortcut(R.string.menu_history, CacheListActivity.getHistoryIntent(this)));

        Dialogs.select(this, getString(R.string.create_shortcut), shortcuts, new Action1<Shortcut>() {

            @Override
            public void call(final Shortcut shortcut) {
                if (shortcut == offlineShortcut) {
                    promptForListShortcut();
                }
                else {
                    createShortcutAndFinish(shortcut.toString(), shortcut.intent, shortcut.drawableResourceId);
                }
            }
        });
    }

    protected void promptForListShortcut() {
        new StoredList.UserInterface(this).promptForListSelection(R.string.create_shortcut, new Action1<Integer>() {

            @Override
            public void call(final Integer listId) {
                createOfflineListShortcut(listId.intValue());
            }
        }, false, -1);
    }

    protected void createOfflineListShortcut(final int listId) {
        final StoredList list = DataStore.getList(listId);
        if (list == null) {
            return;
        }
        // target to be executed by the shortcut
        final Intent targetIntent = new Intent(this, CacheListActivity.class);
        targetIntent.putExtra(Intents.EXTRA_LIST_ID, list.id);

        // shortcut to be returned
        createShortcutAndFinish(list.title, targetIntent, R.drawable.cgeo);
    }

    private void createShortcutAndFinish(final String title, final Intent targetIntent, final int iconResourceId) {
        final ShortcutIconResource iconResource = Intent.ShortcutIconResource.fromContext(this, iconResourceId);
        final Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, targetIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        setResult(RESULT_OK, intent);

        // finish activity to return the shortcut
        finish();
    }

}
