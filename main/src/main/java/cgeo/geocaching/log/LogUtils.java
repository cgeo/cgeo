package cgeo.geocaching.log;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.StatusResult;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.IVotingCapability;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.connector.trackable.TrackableLoggingManager;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.extension.CreateLogPostStore;
import cgeo.geocaching.storage.extension.LastTrackableAction;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;

public final class LogUtils {

    private static final String LOCAL_IMAGE_PROCESSING_FAILURE_PREFIX = "LOCAL_IMAGE_PROCESSING_FAILURE:";

    private LogUtils() {
        //no instance
    }

    public static final Comparator<LogEntry> LOG_ENTRY_DATE_COMPARATOR = (a, b) -> {
        return Long.compare(getDaysSinceEpochZoneCorrected(b.date), getDaysSinceEpochZoneCorrected(a.date));
    };

    /**
     * Gets timezone-corrected days passed since Unix epoch for given time
     */
    public static long getDaysSinceEpochZoneCorrected(final long date) {
        final long offsetMillis = TimeZone.getDefault().getOffset(System.currentTimeMillis());

        // move time
        final long localMillis = date + offsetMillis;

        // get days since epoch
        return localMillis / 86_400_000L;
    }

    /** get title for a log image in the list */
    public static String getLogImageTitle(final Image image, final int imagePos, final int imageCount) {
        if (StringUtils.isNotBlank(image.getTitle())) {
            return image.getTitle();
        }
        final String imageTitlePrafix = Settings.getLogImageCaptionDefaultPraefix();
        if (imageCount <= 1) {
            return imageTitlePrafix; // number is unnecessary if only one image is posted
        }
        return imageTitlePrafix + " " + (imagePos + 1);
    }

    /**
     * Check if the LogEntry is owned by the current configured user of cache-connector.
     *
     * @return {@code true} if LogEntry is from current user
     */
    public static boolean isOwnLog(@NonNull final LogEntry logEntry, @NonNull final Geocache cache) {
        final IConnector connector = ConnectorFactory.getConnector(cache.getGeocode());
        if (!(connector instanceof ILogin)) {
            return false;
        }

        final String ownName = ((ILogin) connector).getUserName();
        return logEntry.author.equalsIgnoreCase(ownName);
    }

    public static boolean isOwnerLog(@NonNull final LogEntry logEntry, @NonNull final Geocache cache) {
        final String cacheOwnerGuid = cache.getOwnerGuid();
        if (!StringUtils.isBlank(cacheOwnerGuid) && !StringUtils.isBlank(logEntry.authorGuid) && logEntry.authorGuid.equals(cacheOwnerGuid)) {
            return true;
        }

        final String cacheOwnerUserId = cache.getOwnerUserId();
        if (!StringUtils.isBlank(cacheOwnerUserId) && !StringUtils.isBlank(logEntry.authorGuid) && logEntry.authorGuid.equals(cacheOwnerUserId)) {
            return true;
        }

        final String ownerName = cache.getOwnerDisplayName();
        if (!StringUtils.isBlank(ownerName) && !StringUtils.isBlank(logEntry.author) && logEntry.author.equalsIgnoreCase(ownerName)) {
            return true;
        }

        return false;
    }

    public static boolean canDeleteLog(final Geocache cache, final LogEntry logEntry) {
        return cache != null && cache.supportsDeleteLog(logEntry);
    }

    public static boolean canEditLog(final Geocache cache, final LogEntry logEntry) {
        return cache != null && cache.supportsEditLog(logEntry);
    }

    public static void startEditLog(final Context activity, final Geocache cache, final LogEntry entry) {
        LogCacheActivity.startForEdit(activity, cache.getGeocode(), entry);
    }

    // Offline log drafts are stored verbatim; trim leading/trailing whitespace only here, right before a log is uploaded or exported (#6631).
    @NonNull
    public static String trimForPublishing(@Nullable final String log) {
        return StringUtils.trimToEmpty(log);
    }

    @WorkerThread
    @SuppressWarnings("PMD.NPathComplexity") // multi-step state is kept explicit for safe resumption
    public static LogResult createLogTaskLogic(final Geocache cache, final OfflineLogEntry logEntry, final Map<String, Trackable> inventory, final Consumer<String> progress) {
        final ILoggingManager loggingManager = cache.getLoggingManager();
        final boolean separateProblemRequired = logEntry.reportProblem != ReportProblemType.NO_PROBLEM
                && !loggingManager.canLogReportType(logEntry.reportProblem);
        final String problemText = separateProblemRequired ? LocalizationUtils.getString(logEntry.reportProblem.textId) : "";
        final CreateLogPostState state = CreateLogPostState.create(cache.getGeocode(), logEntry, inventory,
                separateProblemRequired, problemText);
        if (!CreateLogPostStore.save(state)) {
            return LogResult.error(StatusCode.LOG_POST_ERROR,
                    "Could not save log upload before sending. Nothing was posted.", null);
        }
        return createLogTaskLogic(cache, state.postId, progress);
    }

    /** Resume a persisted create attempt by id. State is always reloaded from storage. */
    @WorkerThread
    @SuppressWarnings("PMD.NPathComplexity")
    public static LogResult createLogTaskLogic(final Geocache cache, final String postId, final Consumer<String> progress) {
        final CreateLogPostStore.LoadResult loaded = CreateLogPostStore.loadResult(postId);
        if (loaded.status == CreateLogPostStore.LoadStatus.NOT_FOUND) {
            return LogResult.error(StatusCode.LOG_POST_ERROR, "This saved log upload no longer exists", null);
        }
        if (loaded.status == CreateLogPostStore.LoadStatus.CORRUPT || loaded.state == null) {
            return LogResult.error(StatusCode.LOG_POST_ERROR,
                    "The saved log upload cannot be read. It was not sent again.", null);
        }
        final CreateLogPostState state = loaded.state;
        try (ContextLogger cLog = new ContextLogger("LogUtils.createLogTaskLogic(%s, %s)", cache.getGeocode(), postId)) {
            try {
                if (!StringUtils.equalsIgnoreCase(cache.getGeocode(), state.geocode)) {
                    return LogResult.error(StatusCode.LOG_POST_ERROR, "The saved log belongs to another cache", null);
                }
                if (state.completionStatus != CreateLogPostState.CompletionStatus.ACTIVE) {
                    return LogResult.error(StatusCode.LOG_POST_ERROR, "The saved log is no longer active", null);
                }
                if (hasUncertainRemoteStep(state)) {
                    return LogResult.error(StatusCode.LOG_POST_ERROR,
                            "A previous request may already have reached the website. Check the website before continuing.", null);
                }

                final ILoggingManager loggingManager = cache.getLoggingManager();
                final IConnector cacheConnector = loggingManager.getConnector();
                final OfflineLogEntry logEntry = state.toOfflineLogEntry();
                final Map<String, Trackable> inventory = state.toInventory();

                if (state.mainStatus == CreateLogPostState.StepStatus.PENDING) {
                    progress.accept(LocalizationUtils.getString(R.string.log_posting_log));
                    state.mainStatus = CreateLogPostState.StepStatus.IN_FLIGHT;
                    if (!CreateLogPostStore.save(state)) {
                        return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not save upload progress", null);
                    }
                    final LogResult result = loggingManager.createLog(logEntry.buildUponOfflineLogEntry()
                            .setLog(trimForPublishing(logEntry.log)).build(), inventory);
                    if (!result.isOk()) {
                        if (isDefinitiveFailure(result)) {
                            state.mainStatus = CreateLogPostState.StepStatus.PENDING;
                            if (!CreateLogPostStore.save(state)) {
                                state.mainStatus = CreateLogPostState.StepStatus.IN_FLIGHT;
                            }
                        }
                        return result;
                    }
                    state.mainServiceLogId = result.getServiceLogId();
                    state.mainStatus = CreateLogPostState.StepStatus.CONFIRMED;
                    if (!CreateLogPostStore.save(state)) {
                        state.mainStatus = CreateLogPostState.StepStatus.IN_FLIGHT;
                        return LogResult.error(StatusCode.LOG_POST_ERROR,
                                "The log was posted but its confirmation could not be saved. It will not be sent again.", null);
                    }
                }

                if (state.problemStatus == CreateLogPostState.StepStatus.PENDING) {
                    progress.accept(LocalizationUtils.getString(R.string.log_posting_problemreport));
                    state.problemStatus = CreateLogPostState.StepStatus.IN_FLIGHT;
                    if (!CreateLogPostStore.save(state)) {
                        return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not save upload progress", null);
                    }
                    final OfflineLogEntry problemLog = new OfflineLogEntry.Builder()
                            .setLogType(LogType.getById(state.problemLogTypeId)).setDate(state.logDate)
                            .setLog(state.problemLogText).setPassword(state.password).build();
                    final LogResult result = loggingManager.createLog(problemLog, null);
                    if (!result.isOk()) {
                        if (isDefinitiveFailure(result)) {
                            state.problemStatus = CreateLogPostState.StepStatus.PENDING;
                            CreateLogPostStore.save(state);
                        }
                        return result;
                    }
                    state.problemServiceLogId = result.getServiceLogId();
                    state.problemStatus = CreateLogPostState.StepStatus.CONFIRMED;
                    if (!CreateLogPostStore.save(state)) {
                        state.problemStatus = CreateLogPostState.StepStatus.IN_FLIGHT;
                        return LogResult.error(StatusCode.LOG_POST_ERROR,
                                "The problem report was posted but its confirmation could not be saved. It will not be sent again.", null);
                    }
                }

                for (CreateLogPostState.ImageState image : state.images) {
                    if (image.status != CreateLogPostState.StepStatus.PENDING) {
                        continue;
                    }
                    progress.accept(LocalizationUtils.getString(R.string.log_posting_image_create,
                            String.valueOf(image.position + 1), String.valueOf(state.images.size())));
                    image.status = CreateLogPostState.StepStatus.IN_FLIGHT;
                    if (!CreateLogPostStore.save(state)) {
                        return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not save upload progress", null);
                    }
                    final Image localImage = new Image.Builder().setUrl(Uri.parse(image.localUri))
                            .setTitle(image.uploadTitle).setDescription(image.description)
                            .setTargetScale(image.targetScale).build();
                    final ImageResult result = uploadNewImage(loggingManager, state.mainServiceLogId,
                            localImage, image.position, state.images.size());
                    if (!result.isOk()) {
                        if (isDefinitiveFailure(result)) {
                            image.status = CreateLogPostState.StepStatus.PENDING;
                            CreateLogPostStore.save(state);
                        }
                        return LogResult.error(result.getStatusCode(), result.getPostServerMessage(), null);
                    }
                    image.serviceImageId = result.getServiceImageId();
                    image.remoteUri = result.getImageUri();
                    image.status = CreateLogPostState.StepStatus.CONFIRMED;
                    if (!CreateLogPostStore.save(state)) {
                        image.status = CreateLogPostState.StepStatus.IN_FLIGHT;
                        return LogResult.error(StatusCode.LOG_POST_ERROR,
                                "The image was uploaded but its confirmation could not be saved. It will not be sent again.", null);
                    }
                }

                if (state.remoteCompletedAt == 0 && state.isRemoteComplete()) {
                    state.remoteCompletedAt = System.currentTimeMillis();
                    if (!CreateLogPostStore.save(state)) {
                        return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not save upload progress", null);
                    }
                }
                final LogResult tailResult = finishCreateLocally(cache, state, loggingManager,
                        cacheConnector, logEntry, inventory, progress);
                if (!tailResult.isOk()) {
                    return tailResult;
                }
                state.completionStatus = CreateLogPostState.CompletionStatus.COMPLETE;
                if (!CreateLogPostStore.save(state)) {
                    return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not save completion state", null);
                }
                CreateLogPostStore.remove(postId);
                cache.notifyChange();
                return LogResult.ok(state.mainServiceLogId);
            } catch (final RuntimeException e) {
                cLog.setException(e);
                Log.e("LogUtils.createLogTaskLogic", e);
                return LogResult.error(StatusCode.LOG_POST_ERROR, "LogUtils.createLogTaskLogic", e);
            } finally {
                cLog.endLog();
            }
        }
    }

    private static LogResult finishCreateLocally(final Geocache cache, final CreateLogPostState state,
            final ILoggingManager loggingManager, final IConnector cacheConnector,
            final OfflineLogEntry logEntry, final Map<String, Trackable> inventory,
            final Consumer<String> progress) {
        progress.accept(LocalizationUtils.getString(R.string.log_posting_save_internal));
        final LogEntry adaptedMain = applyCommonsToOwnLog(logEntry.buildUpon(), cacheConnector)
                .setServiceLogId(state.mainServiceLogId).setLogImages(state.getConfirmedImages()).build();
        if (!runLocalStep(state, () -> state.localLogsStatus, status -> state.localLogsStatus = status, () -> {
            changeLogsInDatabase(cache, logs -> {
                logs.removeIf(existing -> StringUtils.equals(state.mainServiceLogId, existing.serviceLogId)
                        || StringUtils.equals(state.problemServiceLogId, existing.serviceLogId));
                logs.add(0, adaptedMain);
                if (state.problemStatus == CreateLogPostState.StepStatus.CONFIRMED) {
                    logs.add(0, applyCommonsToOwnLog(new LogEntry.Builder()
                            .setLogType(LogType.getById(state.problemLogTypeId)).setDate(state.logDate)
                            .setLog(state.problemLogText), cacheConnector)
                            .setServiceLogId(state.problemServiceLogId).build());
                }
            });
            return true;
        })) {
            return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not save local log state", null);
        }
        if (!runLocalStep(state, () -> state.cacheStateStatus, status -> state.cacheStateStatus = status, () -> {
            final boolean wasFound = cache.isFound();
            adaptCacheCommonsIdempotently(cache, adaptedMain);
            if (adaptedMain.logType.isFoundLog() && !wasFound && loggingManager.getConnector() instanceof ILogin) {
                ((ILogin) loggingManager.getConnector()).increaseCachesFound(1);
            }
            DataStore.saveChangedCache(cache);
            return true;
        })) {
            return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not update local cache state", null);
        }
        if (!runLocalStep(state, () -> state.favoriteStateStatus, status -> state.favoriteStateStatus = status, () -> {
            if (loggingManager.supportsLogWithFavorite() && logEntry.favorite && !cache.isFavorite()) {
                cache.setFavorite(true);
                cache.setFavoritePoints(cache.getFavoritePoints() + 1);
                DataStore.saveChangedCache(cache);
            }
            return true;
        })) {
            return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not update favorite state", null);
        }
        if (!runLocalStep(state, () -> state.trackableStateStatus, status -> state.trackableStateStatus = status, () -> {
            if (loggingManager.supportsLogWithTrackables()) {
                for (Map.Entry<String, LogTypeTrackable> entry : logEntry.inventoryActions.entrySet()) {
                    LastTrackableAction.setAction(entry.getKey(), entry.getValue());
                }
            }
            return true;
        })) {
            return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not save trackable actions", null);
        }
        if (!runOptionalRemoteStep(state, () -> state.ratingStatus, status -> state.ratingStatus = status,
                () -> postCacheRating(cacheConnector, cache, logEntry.rating, progress))) {
            return LogResult.error(StatusCode.LOG_POST_ERROR,
                    "The rating request has an unknown result. Check the website before retrying.", null);
        }
        if (state.trackables.isEmpty()) {
            if (!markSkipped(state, () -> state.genericTrackablesStatus,
                    status -> state.genericTrackablesStatus = status)) {
                return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not save upload progress", null);
            }
        } else if (!runOptionalRemoteStep(state, () -> state.genericTrackablesStatus,
                status -> state.genericTrackablesStatus = status,
                () -> postGenericInventoryLogs(cache, logEntry, inventory.values(), progress))) {
            return LogResult.error(StatusCode.LOG_POST_ERROR,
                    "A trackable request has an unknown result. Check the website before retrying.", null);
        }
        if (!runLocalStep(state, () -> state.localImageCleanupStatus,
                status -> state.localImageCleanupStatus = status, () -> cleanupLocalImages(state))) {
            Log.w("Some uploaded log image files could not be removed; cleanup will be retried later");
        }
        clearOfflineDraftIfUnchanged(cache, state);
        return LogResult.ok(state.mainServiceLogId);
    }

    private static boolean hasUncertainRemoteStep(final CreateLogPostState state) {
        if (state.mainStatus == CreateLogPostState.StepStatus.IN_FLIGHT
                || state.problemStatus == CreateLogPostState.StepStatus.IN_FLIGHT
                || state.ratingStatus == CreateLogPostState.StepStatus.IN_FLIGHT
                || state.genericTrackablesStatus == CreateLogPostState.StepStatus.IN_FLIGHT) {
            return true;
        }
        return state.images.stream().anyMatch(image -> image.status == CreateLogPostState.StepStatus.IN_FLIGHT);
    }

    private interface StatusGetter {
        CreateLogPostState.StepStatus get();
    }

    private interface StatusSetter {
        void set(CreateLogPostState.StepStatus status);
    }

    private static boolean markSkipped(final CreateLogPostState state, final StatusGetter getter,
            final StatusSetter setter) {
        if (getter.get() == CreateLogPostState.StepStatus.SKIPPED
                || getter.get() == CreateLogPostState.StepStatus.CONFIRMED) {
            return true;
        }
        setter.set(CreateLogPostState.StepStatus.SKIPPED);
        if (!CreateLogPostStore.save(state)) {
            setter.set(CreateLogPostState.StepStatus.PENDING);
            return false;
        }
        return true;
    }

    private static boolean runLocalStep(final CreateLogPostState state, final StatusGetter getter,
            final StatusSetter setter, final Supplier<Boolean> action) {
        if (getter.get() == CreateLogPostState.StepStatus.CONFIRMED
                || getter.get() == CreateLogPostState.StepStatus.SKIPPED) {
            return true;
        }
        if (getter.get() == CreateLogPostState.StepStatus.IN_FLIGHT) {
            setter.set(CreateLogPostState.StepStatus.PENDING);
            if (!CreateLogPostStore.save(state)) {
                setter.set(CreateLogPostState.StepStatus.IN_FLIGHT);
                return false;
            }
        }
        setter.set(CreateLogPostState.StepStatus.IN_FLIGHT);
        if (!CreateLogPostStore.save(state)) {
            setter.set(CreateLogPostState.StepStatus.PENDING);
            return false;
        }
        if (!action.get()) {
            setter.set(CreateLogPostState.StepStatus.PENDING);
            CreateLogPostStore.save(state);
            return false;
        }
        setter.set(CreateLogPostState.StepStatus.CONFIRMED);
        if (!CreateLogPostStore.save(state)) {
            setter.set(CreateLogPostState.StepStatus.IN_FLIGHT);
            return false;
        }
        return true;
    }

    private static boolean runOptionalRemoteStep(final CreateLogPostState state, final StatusGetter getter,
            final StatusSetter setter, final Supplier<Boolean> action) {
        if (getter.get() == CreateLogPostState.StepStatus.CONFIRMED
                || getter.get() == CreateLogPostState.StepStatus.SKIPPED) {
            return true;
        }
        if (getter.get() == CreateLogPostState.StepStatus.IN_FLIGHT) {
            return false;
        }
        setter.set(CreateLogPostState.StepStatus.IN_FLIGHT);
        if (!CreateLogPostStore.save(state)) {
            setter.set(CreateLogPostState.StepStatus.PENDING);
            return false;
        }
        if (!action.get()) {
            return false;
        }
        setter.set(CreateLogPostState.StepStatus.CONFIRMED);
        if (!CreateLogPostStore.save(state)) {
            setter.set(CreateLogPostState.StepStatus.IN_FLIGHT);
            return false;
        }
        return true;
    }

    private static void adaptCacheCommonsIdempotently(final Geocache cache, final LogEntry newEntry) {
        switch (newEntry.logType) {
            case WEBCAM_PHOTO_TAKEN:
            case ATTENDED:
            case FOUND_IT:
                cache.setFound(true);
                cache.setVisitedDate(Math.max(cache.getVisitedDate(), newEntry.date));
                break;
            case DIDNT_FIND_IT:
                cache.setDNF(true);
                cache.setVisitedDate(Math.max(cache.getVisitedDate(), newEntry.date));
                break;
            case ARCHIVE:
                cache.setArchived(true);
                break;
            case TEMP_DISABLE_LISTING:
                cache.setDisabled(true);
                break;
            case ENABLE_LISTING:
                cache.setDisabled(false);
                break;
            case NEEDS_MAINTENANCE:
                if (!cache.getAttributes().contains("firstaid_yes")) {
                    cache.getAttributes().add("firstaid_yes");
                    cache.setAttributes(cache.getAttributes());
                }
                break;
            case OWNER_MAINTENANCE:
                cache.getAttributes().remove("firstaid_yes");
                cache.setAttributes(cache.getAttributes());
                break;
        }
    }

    private static boolean cleanupLocalImages(final CreateLogPostState state) {
        boolean complete = true;
        for (CreateLogPostState.ImageState image : state.images) {
            if (image.status != CreateLogPostState.StepStatus.CONFIRMED) {
                continue;
            }
            final Uri uri = Uri.parse(image.localUri);
            if (ContentStorage.get().getFileInfo(uri) != null && !ImageUtils.deleteImage(uri)) {
                complete = false;
            }
        }
        return complete;
    }

    private static void clearOfflineDraftIfUnchanged(final Geocache cache, final CreateLogPostState state) {
        final OfflineLogEntry currentDraft = DataStore.loadLogOffline(cache.getGeocode());
        if (state.matchesDraft(currentDraft)) {
            cache.clearOfflineLog(null);
        }
    }

    private static boolean isDefinitiveFailure(final StatusResult result) {
        if (StringUtils.startsWith(result.getPostServerMessage(), LOCAL_IMAGE_PROCESSING_FAILURE_PREFIX)) {
            return true;
        }
        switch (result.getStatusCode()) {
            case NO_LOG_TEXT:
            case NO_LOGIN_INFO_STORED:
            case WRONG_LOGIN_DATA:
            case UNAPPROVED_LICENSE:
            case UNVALIDATED_ACCOUNT:
            case UNPUBLISHED_CACHE:
            case CACHE_NOT_FOUND:
            case PREMIUM_ONLY:
            case NOT_LOGGED_IN:
                return true;
            default:
                return false;
        }
    }

    private static LogEntry.Builder applyCommonsToOwnLog(final LogEntry.Builder logBuilder, final IConnector cacheConnector) {
        logBuilder.setFriend(true);
        // login credentials may vary from actual username
        // Get correct author name from connector (if applicable)
        if (cacheConnector instanceof ILogin) {
            final String username = ((ILogin) cacheConnector).getUserName();
            if (StringUtils.isNotBlank(username)) {
                logBuilder.setAuthor(username);
            }
        }
        return logBuilder;
    }

    private static boolean postCacheRating(final IConnector cacheConnector, final Geocache cache, final float rating, final Consumer<String> progress) {
        if (!(cacheConnector instanceof IVotingCapability)) {
            return true;
        }
        final IVotingCapability votingConnector = (IVotingCapability) cacheConnector;
        if (!votingConnector.supportsVoting(cache) || !votingConnector.isValidRating(rating)) {
            return true;
        }
        progress.accept(LocalizationUtils.getString(R.string.log_posting_vote));
        if (!votingConnector.postVote(cache, rating)) {
            ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.err_vote_send_rating));
            return false;
        }
        cache.setMyVote(rating);
        DataStore.saveChangedCache(cache);
        return true;
    }

    private static boolean postGenericInventoryLogs(final Geocache cache, final OfflineLogEntry logEntry,
            final Collection<Trackable> inventory, final Consumer<String> progress) {
        boolean successful = true;
        for (final TrackableConnector connector : ConnectorFactory.getLoggableGenericTrackablesConnectors()) {
            final Set<Trackable> trackablesMoved = new HashSet<>();
            for (final Trackable trackable : inventory) {
                final LogTypeTrackable action = logEntry.inventoryActions.get(trackable.getGeocode());
                if (action != null && action != LogTypeTrackable.DO_NOTHING && trackable.getBrand() == connector.getBrand()) {
                    trackablesMoved.add(trackable);
                }
            }
            int trackableLogcounter = 1;
            for (final Trackable trackable : trackablesMoved) {
                final TrackableLoggingManager manager = connector.getTrackableLoggingManager(trackable.getGeocode());
                if (manager != null) {
                    progress.accept(LocalizationUtils.getString(R.string.log_posting_generic_trackable,
                            trackable.getBrand().getLabel(), trackableLogcounter, trackablesMoved.size()));
                    final TrackableLogEntry tle = TrackableLogEntry.of(trackable);
                    tle.setAction(logEntry.inventoryActions.get(trackable.getGeocode()))
                            .setLog(logEntry.log)
                            .setDate(logEntry.getDate());
                    successful &= manager.postLog(cache, tle).isOk();
                    trackableLogcounter++;
                }
            }
        }
        return successful;
    }

    @WorkerThread
    public static LogResult editLogTaskLogic(final Geocache cache, final LogEntry oldEntry, final LogEntry newEntry, final Consumer<String> progress) {
        final ILoggingManager loggingManager = cache.getLoggingManager();
        final IConnector cacheConnector = loggingManager.getConnector();

        //Upload changed log entry itself
        progress.accept(LocalizationUtils.getString(R.string.log_posting_log));
        final LogResult result = cache.getLoggingManager().editLog(newEntry);
        if (!result.isOk()) {
            return result;
        }

        //Upload images
        final ImmutableTriple<ImageResult, List<Image>, Runnable> imgResult = updateImages(loggingManager, newEntry.serviceLogId, oldEntry.logImages, newEntry.logImages, progress);
        if (!imgResult.left.isOk()) {
            return LogResult.error(imgResult.left.getStatusCode(), imgResult.left.getPostServerMessage(), null);
        }

        progress.accept(LocalizationUtils.getString(R.string.log_posting_save_internal));

        //adapt logs in database
        final LogEntry adaptedNewLogEntry = applyCommonsToOwnLog(newEntry.buildUpon(), cacheConnector)
            .setLogImages(imgResult.middle)
            .build();
        changeLogsInDatabase(cache, logs -> {
            final ListIterator<LogEntry> it = logs.listIterator();
            while (it.hasNext()) {
                final LogEntry log = it.next();
                if (Objects.requireNonNull(adaptedNewLogEntry.serviceLogId).equals(log.serviceLogId)) {
                    it.set(adaptedNewLogEntry.buildUpon()
                        .setAuthor(log.author)
                        .build());
                    break;
                }
            }
        });
        //adapt commons in database
        adaptCacheCommonsAfterLogging(cache, loggingManager, oldEntry, adaptedNewLogEntry);
        // adapt cache facvorite status and stored favorite points
        if (loggingManager.supportsLogWithFavorite() && newEntry instanceof OfflineLogEntry) {
            final boolean oldWasFavorite = cache.isFavorite();
            final boolean isFavorite = ((OfflineLogEntry) newEntry).favorite;
            if (oldWasFavorite != isFavorite) {
                cache.setFavorite(isFavorite);
                cache.setFavoritePoints(cache.getFavoritePoints() + (isFavorite ? 1 : -1));
            }
        }
        //store
        DataStore.saveChangedCache(cache);

        //Cleanup
        imgResult.right.run();

        cache.notifyChange();

        return result;

    }

    private static void adaptCacheCommonsAfterLogging(final Geocache cache, final ILoggingManager loggingManager, @Nullable final LogEntry oldLogEntry, final LogEntry newLogEntry) {

        final IConnector cacheConnector = loggingManager.getConnector();

        //update "found" counter
        final boolean oldIsFound = oldLogEntry != null && oldLogEntry.logType.isFoundLog();
        final boolean newIsFound = newLogEntry.logType.isFoundLog();
        if (cacheConnector instanceof ILogin && oldIsFound != newIsFound) {
            ((ILogin) cacheConnector).increaseCachesFound(newIsFound ? 1 : -1);
        }
        final List<String> attributes = cache.getAttributes();

        // update cache state
        switch (newLogEntry.logType) {
            case WEBCAM_PHOTO_TAKEN:
            case ATTENDED:
            case FOUND_IT:
                cache.setFound(true);
                cache.setVisitedDate(newLogEntry.date);
                break;
            case DIDNT_FIND_IT:
                cache.setDNF(true);
                cache.setVisitedDate(newLogEntry.date);
                break;
            case ARCHIVE:
                cache.setArchived(true);
                break;
            case TEMP_DISABLE_LISTING:
                cache.setDisabled(true);
                break;
            case ENABLE_LISTING:
                cache.setDisabled(false);
                break;
            case NEEDS_MAINTENANCE:
                attributes.add("firstaid_yes");
                cache.setAttributes(attributes);
                break;
            case OWNER_MAINTENANCE:
                attributes.remove("firstaid_yes");
                cache.setAttributes(attributes);
                break;
        }
    }

    @WorkerThread
    static LogResult deleteLogTaskLogic(final Geocache cache, final LogEntry logEntry, final String reason, final Consumer<String> progress) {

        progress.accept(LocalizationUtils.getString(R.string.log_posting_deletelog));

        final LogResult result = cache.getLoggingManager().deleteLog(logEntry, reason);
        if (!result.isOk()) {
            return result;
        }
        progress.accept(LocalizationUtils.getString(R.string.log_posting_save_internal));
        changeLogsInDatabase(cache, logs -> {
            final Iterator<LogEntry> it = logs.listIterator();
            while (it.hasNext()) {
                final LogEntry log = it.next();
                if (Objects.requireNonNull(logEntry.serviceLogId).equals(log.serviceLogId)) {
                    it.remove();
                    break;
                }
            }
        });
        cache.notifyChange();
        return result;
    }

    @WorkerThread
    static LogResult createLogTrackableTaskLogic(final Geocache cache, final TrackableLogEntry logEntry, final TrackableConnector connector, final Consumer<String> progress) {

        progress.accept(LocalizationUtils.getString(R.string.log_saving));

        final TrackableLoggingManager loggingManager = connector.getTrackableLoggingManager(logEntry.trackingCode);
        if (loggingManager == null) {
            return LogResult.error(StatusCode.LOG_POST_ERROR, "No logging manager for: " + logEntry, null);
        }

        try {
            // Real call to post log
            final LogResult logResult = loggingManager.postLog(cache, logEntry);

            if (logResult.isOk()) {
                LastTrackableAction.setAction(logEntry.geocode, logEntry.getAction());
            }
            // Display errors to the user
            if (StringUtils.isNotEmpty(logResult.getPostServerMessage())) {
                ActivityMixin.showApplicationToast(logResult.getPostServerMessage());
            }

            // Return request status
            return logResult;
        } catch (final RuntimeException e) {
            Log.e("LogTrackableActivity.Poster.doInBackgroundInternal", e);
            return LogResult.error(StatusCode.LOG_POST_ERROR, "Exception", e);
        }
    }

    @WorkerThread
    private static void changeLogsInDatabase(final Geocache cache, final Consumer<List<LogEntry>> logChanger) {
        final List<LogEntry> logs = new ArrayList<>(cache.getLogs());
        logChanger.accept(logs);
        DataStore.saveLogs(cache.getGeocode(), logs, true);
    }

    @WorkerThread
    private static ImageResult uploadNewImage(final ILoggingManager loggingManager, final String logId, final Image image, final int pos, final int imageCount) {

        //uploader can only deal with files, not with content Uris. Thus scale/compress into a temporary file
        final File imageFileForUpload = ImageUtils.scaleAndCompressImageToTemporaryFile(image.getUri(), image.targetScale, 75);
        if (imageFileForUpload == null) {
            return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR,
                    LOCAL_IMAGE_PROCESSING_FAILURE_PREFIX + image.getUrl(), null);
        }

        final Image imgToSend = image.buildUpon()
            .setUrl(Uri.fromFile(imageFileForUpload))
            .setTitle(getLogImageTitle(image, pos, imageCount))
            .build();
        final ImageResult result = loggingManager.createLogImage(logId, imgToSend);

        //delete temp file for upload
        if (!imageFileForUpload.delete()) {
            Log.i("Temporary image not deleted: " + imageFileForUpload);
        }

        return result;
    }

    @WorkerThread
    private static ImmutableTriple<ImageResult, List<Image>, Runnable> updateImages(final ILoggingManager loggingManager, final String logId, final List<Image> oldImages, final List<Image> newImages, final Consumer<String> progress) {

        final Map<String, Image> oldImageMap = oldImages.stream()
            .filter(img -> img.serviceImageId != null)
            .collect(Collectors.toMap(img -> img.serviceImageId, img -> img, (i1, i2) -> i1, HashMap::new));

        ImageResult result = ImageResult.ok("");
        final List<Image> adaptedImageList = new ArrayList<>();
        final List<Uri> localImageUrisToDelete = new ArrayList<>();

        int imgPos = 0;
        int imgCount = newImages.size();
        for (Image newImg : newImages) {
            final Image oldImg = oldImageMap.remove(newImg.serviceImageId);
            final String newTitle = getLogImageTitle(newImg, imgPos, imgCount);

            if (newImg.serviceImageId == null || oldImg == null) {
                //New Image -> upload
                progress.accept(LocalizationUtils.getString(R.string.log_posting_image_create, String.valueOf(imgPos + 1), String.valueOf(imgCount)));
                result = uploadNewImage(loggingManager, logId, newImg, imgPos, imgCount);
                adaptedImageList.add(newImg.buildUpon()
                    .setServiceImageId(result.getServiceImageId())
                    .setUrl(result.getImageUri())
                    .setTitle(newTitle)
                    .build());

                if (result.isOk()) {
                    localImageUrisToDelete.add(newImg.getUri());
                }

            } else if (loggingManager.supportsEditLogImages() && !imagePropertiesEqual(oldImg, newImg, newTitle)) {
                //Existing image with changed properties -> edit
                progress.accept(LocalizationUtils.getString(R.string.log_posting_image_edit, String.valueOf(imgPos + 1), String.valueOf(imgCount)));
                result = loggingManager.editLogImage(logId, newImg.serviceImageId, getLogImageTitle(newImg, imgPos, imgCount), newImg.getDescription());
                adaptedImageList.add(newImg.buildUpon()
                    .setTitle(newTitle)
                    .build());
            } else {
                //image unchanged
                adaptedImageList.add(newImg.buildUpon()
                    .setTitle(newTitle)
                    .build());
            }
            if (!result.isOk()) {
                return new ImmutableTriple<>(result, Collections.emptyList(), () -> { });
            }
            imgPos++;
        }

        if (loggingManager.supportsDeleteLogImages()) {
            imgPos = 0;
            imgCount = oldImageMap.size();
            for (String oldImgIdToRemove : oldImageMap.keySet()) {
                progress.accept(LocalizationUtils.getString(R.string.log_posting_image_delete, String.valueOf(imgPos + 1), String.valueOf(imgCount)));
                //remove image from log
                result = loggingManager.deleteLogImage(logId, oldImgIdToRemove);
                if (!result.isOk()) {
                    return new ImmutableTriple<>(result, Collections.emptyList(), () -> { });
                }
            }
        }
        return new ImmutableTriple<>(result, adaptedImageList, () -> {
            for (Uri uri : localImageUrisToDelete) {
                ImageUtils.deleteImage(uri);
            }
        });
    }

    private static boolean imagePropertiesEqual(final Image oldImg, final Image newImg, final String newTitle) {
        return Objects.equals(oldImg.title, newTitle) && Objects.equals(oldImg.getDescription(), newImg.getDescription());
    }
}
