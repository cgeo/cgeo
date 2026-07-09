package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.IIgnoreCapability;
import cgeo.geocaching.connector.capability.IIgnoreListCapability;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class IgnoreListUtils {
    private IgnoreListUtils() {
        // utility class
    }

    public static void ignoreAll(final Context context, final List<Geocache> caches) {
        updateHandler(context, caches, true);
    }

    public static void unignoreAll(final Context context, final List<Geocache> caches) {
        updateHandler(context, caches, false);
    }

    private static void updateHandler(final Context context, final List<Geocache> caches, final boolean actionIsIgnore) {
        final Context appContext = context.getApplicationContext();
        ActivityMixin.showToast(appContext, LocalizationUtils.getString(R.string.ignorelist_background_started));
        final Predicate<Geocache> canAct = actionIsIgnore ? IgnoreListUtils::canIgnore : IgnoreListUtils::canUnignore;
        AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
            final List<Geocache> candidates = filterCandidates(caches, canAct);
            final List<Geocache> succeeded = actionIsIgnore ? ignoreOnlineBatch(candidates) : unignoreOnlineBatch(candidates);
            final List<String> failedCaches = getFailedGeocodes(candidates, succeeded);
            ActivityMixin.showToast(appContext, failedCaches.isEmpty() ? LocalizationUtils.getString(actionIsIgnore ? R.string.caches_ignore_all : R.string.caches_unignore_all) : LocalizationUtils.getString(R.string.err_ignorelist_failed_geocodes, String.join(",", failedCaches)));
        });
    }

    private static List<Geocache> filterCandidates(final List<Geocache> caches, final Predicate<Geocache> canAct) {
        final List<Geocache> candidates = new ArrayList<>();
        for (final Geocache cache : caches) {
            if (canAct.test(cache)) {
                candidates.add(cache);
            }
        }
        return candidates;
    }

    private static List<String> getFailedGeocodes(final List<Geocache> candidates, final List<Geocache> succeeded) {
        final Set<String> succeededGeocodes = new HashSet<>();
        for (final Geocache cache : succeeded) {
            succeededGeocodes.add(cache.getGeocode());
        }
        final List<String> failedCaches = new ArrayList<>();
        for (final Geocache cache : candidates) {
            if (!succeededGeocodes.contains(cache.getGeocode())) {
                failedCaches.add(cache.getGeocode());
            }
        }
        return failedCaches;
    }

    /**
     * Ignores a single cache online and mirrors it into the local ignore list. Must not be called on main thread.
     *
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    public static boolean ignoreOnline(final Geocache cache) {
        if (!callOnlineIgnoreAction(cache, true)) {
            return false;
        }
        cache.getLists().add(StoredList.IGNORE_LIST_ID);
        DataStore.saveCache(cache, LoadFlags.SAVE_ALL);
        return true;
    }

    /**
     * Removes a single cache from the online ignore list and from the local ignore list. Must not be called on
     * main thread.
     *
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    public static boolean unignoreOnline(final Geocache cache) {
        if (!callOnlineIgnoreAction(cache, false)) {
            return false;
        }
        DataStore.removeFromList(Collections.singletonList(cache), StoredList.IGNORE_LIST_ID);
        return true;
    }

    /**
     * Ignores multiple caches online and mirrors the change into the local ignore list, batching the local
     * database write into a single transaction instead of one per cache. Must not be called on main thread.
     *
     * @return the subset of {@code caches} that were successfully ignored online
     */
    public static List<Geocache> ignoreOnlineBatch(final Collection<Geocache> caches) {
        final List<Geocache> succeeded = new ArrayList<>();
        for (final Geocache cache : caches) {
            if (callOnlineIgnoreAction(cache, true)) {
                cache.getLists().add(StoredList.IGNORE_LIST_ID);
                succeeded.add(cache);
            }
        }
        if (!succeeded.isEmpty()) {
            DataStore.saveCaches(succeeded, LoadFlags.SAVE_ALL);
        }
        return succeeded;
    }

    /**
     * Removes multiple caches from the online ignore list and from the local ignore list, batching the local
     * database write into a single transaction instead of one per cache. Must not be called on main thread.
     *
     * @return the subset of {@code caches} that were successfully removed online
     */
    public static List<Geocache> unignoreOnlineBatch(final Collection<Geocache> caches) {
        final List<Geocache> succeeded = new ArrayList<>();
        for (final Geocache cache : caches) {
            if (callOnlineIgnoreAction(cache, false)) {
                succeeded.add(cache);
            }
        }
        if (!succeeded.isEmpty()) {
            DataStore.removeFromList(succeeded, StoredList.IGNORE_LIST_ID);
        }
        return succeeded;
    }

    private static boolean callOnlineIgnoreAction(final Geocache cache, final boolean actionIsIgnore) {
        final IConnector connector = ConnectorFactory.getConnector(cache);
        if (!(connector instanceof IIgnoreCapability)) {
            return false;
        }
        return actionIsIgnore ? ((IIgnoreCapability) connector).addToIgnorelist(cache) : ((IIgnoreCapability) connector).removeFromIgnorelist(cache);
    }

    /**
     * Snapshots which of the given caches are currently locally on the ignore list. Take this snapshot before a
     * generic list-membership operation (move/copy/multi-select "store in lists", "make list unique", ...) that
     * might affect {@link StoredList#IGNORE_LIST_ID}, then pass it to {@link #reflectMembershipChange} afterwards
     * so the online ignore list stays in sync regardless of which UI path changed the local list membership.
     */
    public static Set<String> snapshotIgnoreListMembership(final Collection<Geocache> caches) {
        final Set<String> onIgnoreList = new HashSet<>();
        for (final Geocache cache : caches) {
            if (cache.getLists().contains(StoredList.IGNORE_LIST_ID)) {
                onIgnoreList.add(cache.getGeocode());
            }
        }
        return onIgnoreList;
    }

    /**
     * Reflects any net add/remove of the given caches' local ignore-list membership (compared to a snapshot taken
     * with {@link #snapshotIgnoreListMembership} before a generic list-membership operation) to the online ignore
     * list. Must not be called on main thread.
     */
    public static void reflectMembershipChange(final Collection<Geocache> caches, final Set<String> wasOnIgnoreList) {
        for (final Geocache cache : caches) {
            final boolean was = wasOnIgnoreList.contains(cache.getGeocode());
            final boolean is = cache.getLists().contains(StoredList.IGNORE_LIST_ID);
            if (is != was) {
                callOnlineIgnoreAction(cache, is);
            }
        }
    }

    private static boolean canIgnore(final Geocache cache) {
        final IConnector connector = ConnectorFactory.getConnector(cache);
        return connector instanceof IIgnoreCapability && ((IIgnoreCapability) connector).canIgnoreCache(cache) && !cache.getLists().contains(StoredList.IGNORE_LIST_ID);
    }

    private static boolean canUnignore(final Geocache cache) {
        final IConnector connector = ConnectorFactory.getConnector(cache);
        return connector instanceof IIgnoreCapability && ((IIgnoreCapability) connector).canRemoveFromIgnoreCache(cache) && cache.getLists().contains(StoredList.IGNORE_LIST_ID);
    }

    public static boolean anySupportsIgnoreList(final List<Geocache> caches) {
        return caches.stream().anyMatch(cache -> ConnectorFactory.getConnector(cache) instanceof IIgnoreCapability);
    }

    public static boolean anySupportsIgnoring(final List<Geocache> caches) {
        return caches.stream().anyMatch(IgnoreListUtils::canIgnore);
    }

    public static boolean anySupportsUnignoring(final List<Geocache> caches) {
        return caches.stream().anyMatch(IgnoreListUtils::canUnignore);
    }

    /**
     * Populates the local ignore list with the online ignore list content on first use. Safe to call repeatedly,
     * as it is a no-op once the initial sync has happened. Must not be called on main thread.
     *
     * @param onSyncDone optional callback, invoked on a background thread once the sync attempt finished
     *                    (whether or not anything was actually synced)
     */
    public static void syncOnlineIgnoreListIfNeeded(final Context context, @Nullable final Runnable onSyncDone) {
        if (Settings.isIgnoreListSynced()) {
            return;
        }
        final Context appContext = context.getApplicationContext();
        AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
            final SyncOutcome outcome = syncAllIgnoreListConnectors();
            if (outcome == SyncOutcome.ALL_SUCCEEDED) {
                Settings.setIgnoreListSynced(true);
            } else if (outcome == SyncOutcome.SOME_FAILED) {
                ActivityMixin.showToast(appContext, LocalizationUtils.getString(R.string.ignorelist_sync_failed));
            }
            if (onSyncDone != null) {
                onSyncDone.run();
            }
        });
    }

    private enum SyncOutcome { NONE_ATTEMPTED, ALL_SUCCEEDED, SOME_FAILED }

    private static SyncOutcome syncAllIgnoreListConnectors() {
        boolean anyAttempt = false;
        boolean anyFailure = false;
        for (final IConnector connector : ConnectorFactory.getActiveConnectors()) {
            if (connector instanceof IIgnoreListCapability) {
                anyAttempt = true;
                if (!fetchAndMergeIgnoreList((IIgnoreListCapability) connector)) {
                    anyFailure = true;
                }
            }
        }
        if (!anyAttempt) {
            return SyncOutcome.NONE_ATTEMPTED;
        }
        return anyFailure ? SyncOutcome.SOME_FAILED : SyncOutcome.ALL_SUCCEEDED;
    }

    /**
     * @return {@code true} if the connector's online ignore list could be fetched (and merged locally)
     */
    private static boolean fetchAndMergeIgnoreList(final IIgnoreListCapability connector) {
        final List<Geocache> ignoredCaches = connector.fetchIgnoreList();
        if (ignoredCaches == null) {
            return false;
        }
        if (!ignoredCaches.isEmpty()) {
            DataStore.addToList(ignoredCaches, StoredList.IGNORE_LIST_ID);
        }
        return true;
    }
}
