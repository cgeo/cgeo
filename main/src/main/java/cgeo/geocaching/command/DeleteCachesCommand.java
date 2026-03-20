package cgeo.geocaching.command;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.LocalizationUtils;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class DeleteCachesCommand extends AbstractCachesCommand {

    private final Handler handler;
    private final Map<String, Set<Integer>> oldCachesLists = new HashMap<>();
    private final Map<String, OfflineLogEntry> oldOfflineLogs = new HashMap<>();
    private final Map<String, Long> oldVisitedDate = new HashMap<>();

    public DeleteCachesCommand(@NonNull final Activity context, final Collection<Geocache> caches, @Nullable final Handler handler) {
        super(context, caches, R.string.command_delete_caches_progress);
        this.handler = handler;
    }

    public void showDeleteAllDialogsAndExecute() {
        final Collection<Geocache> caches = getCaches();

        // Check if deleting all caches
        final int cacheCount = caches.size();
        if (cacheCount == PseudoList.ALL_LIST.getNumberOfCaches()) {
            SimpleDialog.of(getContext())
                    .setTitle(R.string.command_delete_caches_progress)
                    .setMessage(R.string.caches_warning_delete_all_caches, cacheCount)
                    .setButtons(SimpleDialog.ButtonTextSet.OK_CANCEL)
                    .confirm(this::showUserDataDialogAndExecute);
        } else {
            showUserDataDialogAndExecute();
        }
    }

    public void showUserDataDialogAndExecute() {
        final Collection<Geocache> caches = getCaches();

        final List<Geocache> cachesWithUserData = caches.stream().filter(this::hasUserData).toList();
        final int count = cachesWithUserData.size();
        if (count > 0) {
            Dialogs.advancedOneTimeMessage(getContext(), OneTimeDialogs.DialogType.DELETE_CACHES_USER_DATA_WARNING,
                    LocalizationUtils.getString(R.string.command_delete_caches_progress), LocalizationUtils.getString(R.string.caches_warning_delete_user_data, count),
                    true, this::execute,
                    LocalizationUtils.getString(R.string.caches_warning_delete_user_data_others),
                    () -> {
                        caches.removeAll(cachesWithUserData);
                        execute();
                    }
            );
        } else {
            execute();
        }
    }

    @Override
    protected void doCommand() {
        final Collection<Geocache> caches = getCaches();

        saveDroppedInfos(caches);

        oldCachesLists.putAll(DataStore.markDropped(caches));

        final Set<String> geocodes = Geocache.getGeocodes(caches);
        DataStore.removeCaches(geocodes, EnumSet.of(LoadFlags.RemoveFlag.CACHE));
    }

    @Override
    @SuppressWarnings("unused")
    protected void undoCommand() {
        final Collection<Geocache> caches = getCaches();

        DataStore.addToLists(caches, oldCachesLists);

        restoreDroppedInfos(caches);
    }

    private boolean hasUserData(final Geocache cache) {
        final boolean hasOfflineLog = cache.hasLogOffline();
        final boolean hasPersonalNote = !StringUtils.isEmpty(cache.getPersonalNote());
        final boolean hasUserdefinedWaypoints = cache.hasUserdefinedWaypoints();
        final boolean hasUserModifiedCoords = cache.hasUserModifiedCoords();
        final boolean hasVariables = !cache.getVariables().isEmpty();
        final boolean hasUserModifiedWaypoints = cache.getWaypoints().stream().anyMatch(Waypoint::isUserModified);
        return hasOfflineLog || hasPersonalNote || hasVariables
                || hasUserdefinedWaypoints || hasUserModifiedWaypoints || hasUserModifiedCoords;
    }

    private void saveDroppedInfos(final Collection<Geocache> caches) {
        for (final Geocache cache : caches) {
            final String geocode = cache.getGeocode();
            if (cache.hasLogOffline()) {
                oldOfflineLogs.put(geocode, DataStore.loadLogOffline(geocode));
            }

            // because visited date is reset on dropping cache
            final long visitedDate = cache.getVisitedDate();
            if (0 < visitedDate) {
                oldVisitedDate.put(geocode, visitedDate);
            }

            // PersonalNote, Waypoint are saved with the cache and hence restored with it
            // Variables are not deleted yet, so no need to save
        }
    }

    private void restoreDroppedInfos(final Collection<Geocache> caches) {
        for (final Geocache cache : caches) {
            final String geocode = cache.getGeocode();

            final OfflineLogEntry offlineLog = oldOfflineLogs.get(geocode);
            if (offlineLog != null) {
                DataStore.saveLogOffline(geocode, offlineLog);
            }

            final Long visitedDate = oldVisitedDate.get(geocode);
            if (visitedDate != null) {
                DataStore.saveVisitDate(geocode, visitedDate);
            }

            // PersonalNote, Waypoint are saved with the cache and hence restored with it
            // Variables are not deleted yet, so no need to restore
        }
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
