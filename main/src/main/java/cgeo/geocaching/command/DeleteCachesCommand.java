package cgeo.geocaching.command;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DeleteCachesCommand extends AbstractCachesCommand {

    final private Handler handler;
    private final Map<String, Set<Integer>> oldCachesLists = new HashMap<>();


    public DeleteCachesCommand(@NonNull final Activity context, final Collection<Geocache> caches, @Nullable final Handler handler) {
        super(context, caches, R.string.command_delete_caches_progress);
        this.handler = handler;
    }

    public void showDeleteAllDialogsAndExecute() {
        final Collection<Geocache> caches = getCaches();

        // Check if deleting all caches
        if (caches.size() == PseudoList.ALL_LIST.getNumberOfCaches()) {
            SimpleDialog.of(getContext())
                    .setTitle(R.string.command_delete_caches_progress)
                    .setMessage(R.string.caches_warning_delete_all_caches)
                    .setButtons(SimpleDialog.ButtonTextSet.YES_NO)
                    .confirm(this::execute);
        } else {
            execute();
        }
    }

    @Override
    protected void doCommand() {
        final Collection<Geocache> caches = getCaches();

        oldCachesLists.putAll(DataStore.markDropped(caches));

        final Set<String> geocodes = Geocache.getGeocodes(caches);
        DataStore.removeCaches(geocodes, EnumSet.of(LoadFlags.RemoveFlag.CACHE));
    }

    @Override
    @SuppressWarnings("unused")
    protected void undoCommand() {
        final Collection<Geocache> caches = getCaches();

        DataStore.addToLists(caches, oldCachesLists);
    }

    @Override
    protected void onFinished() {
        if (null != handler) {
            handler.sendMessage(Message.obtain());
        }
    }

    @Override
    protected void onFinishedUndo() {
        if (null != handler) {
            handler.sendMessage(Message.obtain());
        }
    }

    @Override
    @NonNull
    protected String getResultMessage() {
        final int size = getCaches().size();
        return getContext().getResources().getQuantityString(R.plurals.command_delete_caches_result, size, size);
    }
}
