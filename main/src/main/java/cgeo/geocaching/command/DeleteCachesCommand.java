package cgeo.geocaching.command;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.LocalizationUtils;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class DeleteCachesCommand extends AbstractCachesCommand {

    @Nullable
    private final Runnable onFinishedCallback;

    private final DeleteFromDeviceAction deleteFromDeviceAction;
    @Nullable
    private final RemoveFromListAction removeFromListAction;

    public DeleteCachesCommand(@NonNull final Activity context,
                               final Collection<Geocache> caches,
                               @Nullable final Runnable onFinishedCallback) {
        super(context, caches, R.string.command_delete_caches_progress);
        this.onFinishedCallback = onFinishedCallback;

        deleteFromDeviceAction = new DeleteFromDeviceAction(caches);
        removeFromListAction = null;
    }

    public DeleteCachesCommand(@NonNull final Activity context,
                               final Collection<Geocache> caches,
                               final int listId,
                               @Nullable final Runnable onFinishedCallback) {
        super(context, caches, R.string.command_remove_caches_from_list_progress);
        this.onFinishedCallback = onFinishedCallback;

        final List<Geocache> toDelete = new ArrayList<>();
        final List<Geocache> toRemoveFromList = new ArrayList<>();
        for (final Geocache cache : getCaches()) {
            if (cache.getLists().size() == 1) {
                toDelete.add(cache);
            } else {
                toRemoveFromList.add(cache);
            }
        }

        deleteFromDeviceAction = new DeleteFromDeviceAction(toDelete);
        removeFromListAction = new RemoveFromListAction(toRemoveFromList, listId);
    }

    public void showAllDialogsAndExecute() {
        if (removeFromListAction != null) {
            showRemoveFromListDialogsAndExecute();
        } else {
            showDeleteAllDialogsAndExecute();
        }
    }

    private void showDeleteAllDialogsAndExecute() {
        final int cacheCount = deleteFromDeviceAction.getCacheCount();
        if (cacheCount > 0 && (cacheCount == PseudoList.ALL_LIST.getNumberOfCaches())) {
            final String title = LocalizationUtils.getString(R.string.command_delete_caches_progress);
            final String warningMessage = LocalizationUtils.getString(R.string.caches_warning_delete_all_caches, cacheCount);

            SimpleDialog.of(getContext())
                    .setTitle(TextParam.text(title))
                    .setMessage(TextParam.text(warningMessage))
                    .setButtons(SimpleDialog.ButtonTextSet.OK_CANCEL)
                    .confirm(this::showUserDataDialogAndExecute);
        } else {
            showUserDataDialogAndExecute();
        }
    }

    private void showRemoveFromListDialogsAndExecute() {
        final int cacheCount = deleteFromDeviceAction.getCacheCount();
        if (cacheCount > 0) {
            final String title = LocalizationUtils.getString(R.string.command_remove_caches_from_list_progress);
            final String warningMessage = LocalizationUtils.getString(R.string.caches_warning_remove_caches_from_single_list, cacheCount, getCaches().size());
            final String othersButtonText = LocalizationUtils.getString(R.string.caches_warning_remove_from_list_user_data_others);
            Dialogs.advancedOneTimeMessage(getContext(), OneTimeDialogs.DialogType.REMOVE_CACHES_FROM_LIST_WARNING,
                    title, warningMessage, true, this::showUserDataDialogAndExecute,
                    othersButtonText,
                    () -> {
                        deleteFromDeviceAction.removeAllCaches();
                        execute();
                    }
            );
        } else {
            execute();
        }
    }


    public void showUserDataDialogAndExecute() {
        final List<Geocache> cachesWithUserData = deleteFromDeviceAction.getCachesWithUserData();
        final int count = cachesWithUserData.size();
        if (count > 0) {
            final String title = LocalizationUtils.getString(R.string.command_delete_caches_progress);
            final String warningMessage = LocalizationUtils.getPlural(R.plurals.caches_warning_delete_user_data, count);
            final String othersButtonText = LocalizationUtils.getString(R.string.caches_warning_delete_user_data_others);

            Dialogs.advancedOneTimeMessage(getContext(), OneTimeDialogs.DialogType.DELETE_CACHES_USER_DATA_WARNING,
                    title, warningMessage, true, this::execute,
                    othersButtonText,
                    () -> {
                        deleteFromDeviceAction.removeCaches(cachesWithUserData);
                        execute();
                    }
            );
        } else {
            execute();
        }
    }

    @Override
    protected boolean canExecute() {
        return !getCaches().isEmpty();
    }

    @Override
    protected void doCommand() {
        if (removeFromListAction != null) {
            removeFromListAction.executeAction();
        }
        deleteFromDeviceAction.executeAction();
    }

    @Override
    @SuppressWarnings("unused")
    protected void undoCommand() {
        if (removeFromListAction != null) {
            removeFromListAction.undoAction();
        }
        deleteFromDeviceAction.undoAction();
    }

    @Override
    protected void onFinished() {
        if (onFinishedCallback != null) {
            onFinishedCallback.run();
        }
    }

    @Override
    protected void onFinishedUndo() {
        if (onFinishedCallback != null) {
            onFinishedCallback.run();
        }
    }

    @Override
    @NonNull
    protected String getResultMessage() {
        final int deletedSize = deleteFromDeviceAction.getCacheCount();
        final int removedSize = removeFromListAction != null ? removeFromListAction.getCacheCount() : 0;
        return LocalizationUtils.getPlural(removeFromListAction != null ? R.plurals.command_remove_caches_result : R.plurals.command_delete_caches_result, deletedSize + removedSize);
    }

    // ---- inner action classes ----

    /**
     * Encapsulates fully deleting caches from the device, including undo data.
     */
    private static final class DeleteFromDeviceAction {

        private final Collection<Geocache> caches;
        private final Map<String, Set<Integer>> oldCachesLists = new HashMap<>();
        private final Map<String, OfflineLogEntry> oldOfflineLogs = new HashMap<>();
        private final Map<String, Long> oldVisitedDate = new HashMap<>();

        private DeleteFromDeviceAction(final Collection<Geocache> caches) {
            this.caches = caches;
        }

        private List<Geocache> getCachesWithUserData() {
            return caches.stream().filter(this::hasUserData).toList();
        }

        private int getCacheCount() {
            return caches.size();
        }

        private void removeAllCaches() {
            caches.clear();
        }

        private void removeCaches(final Collection<Geocache> cachesToRemove) {
            caches.removeAll(cachesToRemove);
        }

        private void executeAction() {
            if (caches.isEmpty()) {
                return;
            }

            // save dropped infos for undo:
            // PersonalNote, Waypoint are saved with the cache and hence restored with it
            // Variables are not deleted yet, so no need to save
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
            }

            oldCachesLists.putAll(DataStore.markDropped(caches));

            // remove from cache-cache
            final Set<String> geocodes = Geocache.getGeocodes(caches);
            DataStore.removeCaches(geocodes, EnumSet.of(LoadFlags.RemoveFlag.CACHE));
        }

        private void undoAction() {
            if (caches.isEmpty()) {
                return;
            }

            DataStore.addToLists(caches, oldCachesLists);

            // restore dropped infos:
            // PersonalNote, Waypoint are saved with the cache and hence restored with it
            // Variables are not deleted yet, so no need to restore
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
            }
        }

        private boolean hasUserData(final Geocache cache) {
            return cache.hasLogOffline()
                    || !StringUtils.isEmpty(cache.getPersonalNote())
                    || cache.hasUserdefinedWaypoints()
                    || cache.hasUserModifiedCoords()
                    || !cache.getVariables().isEmpty()
                    || cache.getWaypoints().stream().anyMatch(Waypoint::isUserModified);
        }
    }

    /**
     * Encapsulates removing caches from a specific list (they remain on other lists).
     */
    private static final class RemoveFromListAction {

        private final Collection<Geocache> caches;
        private final int listId;

        private RemoveFromListAction(final Collection<Geocache> caches, final int listId) {
            this.caches = caches;
            this.listId = listId;
        }

        private int getCacheCount() {
            return caches.size();
        }

        private void executeAction() {
            if (!caches.isEmpty()) {
                DataStore.removeFromList(caches, listId);
            }
        }

        private void undoAction() {
            if (!caches.isEmpty()) {
                DataStore.addToList(caches, listId);
            }
        }
    }
}
