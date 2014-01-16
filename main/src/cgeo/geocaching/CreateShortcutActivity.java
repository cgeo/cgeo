package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;

import rx.util.functions.Action1;

import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;

public class CreateShortcutActivity extends AbstractActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init
        setTheme();

        promptForShortcut();
    }

    private void promptForShortcut() {
        new StoredList.UserInterface(this).promptForListSelection(R.string.create_shortcut, new Action1<Integer>() {

            @Override
            public void call(final Integer listId) {
                final Intent shortcut = createShortcut(listId.intValue());
                setResult(RESULT_OK, shortcut);

                // finish activity to return the shortcut
                finish();
            }
        }, false, PseudoList.HISTORY_LIST.id);
    }

    protected Intent createShortcut(int listId) {
        final StoredList list = DataStore.getList(listId);
        if (list == null) {
            return null;
        }
        // target to be executed by the shortcut
        final Intent targetIntent = new Intent(this, CacheListActivity.class);
        targetIntent.putExtra(Intents.EXTRA_LIST_ID, list.id);
        final ShortcutIconResource iconResource = Intent.ShortcutIconResource.fromContext(this, R.drawable.cgeo);

        // shortcut to be returned
        final Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, targetIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, list.title);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
        return intent;
    }

}
