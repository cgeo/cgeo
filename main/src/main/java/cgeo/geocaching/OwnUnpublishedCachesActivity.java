package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.unifiedmap.DefaultMap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Dedicated screen for {@link PseudoList#OWN_UNPUBLISHED_LIST}: the normal offline cache list, plus a shortcut
 * to start a new hide on geocaching.com and a shortcut to see these caches overlaid on the live map
 * (see {@link cgeo.geocaching.unifiedmap.layers.OwnUnpublishedCachesLayer}).
 */
public class OwnUnpublishedCachesActivity extends CacheListActivity {

    private static final int MENU_ITEM_NEW_DRAFT = 1;
    private static final int MENU_ITEM_SHOW_ON_LIVE_MAP = 2;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Intents.putListType(getIntent(), CacheListType.OFFLINE);
        getIntent().putExtra(Intents.EXTRA_LIST_ID, PseudoList.OWN_UNPUBLISHED_LIST.id);
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_ITEM_NEW_DRAFT, Menu.NONE, R.string.own_unpublished_new_draft)
                .setIcon(R.drawable.ic_menu_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(Menu.NONE, MENU_ITEM_SHOW_ON_LIVE_MAP, Menu.NONE, R.string.own_unpublished_show_on_live_map)
                .setIcon(R.drawable.ic_menu_mapmode)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == MENU_ITEM_NEW_DRAFT) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.geocaching.com/play/hide")));
            return true;
        }
        if (item.getItemId() == MENU_ITEM_SHOW_ON_LIVE_MAP) {
            DefaultMap.startActivityLive(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
