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
import cgeo.geocaching.enumerations.LoadFlags;
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

    @Nullable
    public static CreateLogPostState prepareCreateLogPost(final Geocache cache,
            final OfflineLogEntry logEntry, final Map<String, Trackable> inventory) {
        final ILoggingManager loggingManager = cache.getLoggingManager();
        final boolean separateProblemRequired = logEntry.reportProblem != ReportProblemType.NO_PROBLEM
                && !loggingManager.canLogReportType(logEntry.reportProblem);
        final String problemText = separateProblemRequired
                ? LocalizationUtils.getString(logEntry.reportProblem.textId) : "";
        final CreateLogPostState state = CreateLogPostState.create(cache.getGeocode(), logEntry, inventory,
                separateProblemRequired, problemText);
        return CreateLogPostStore.create(state) ? state : null;
    }

    /** Resume a persisted create attempt by id. State is always reloaded from storage. */
    @WorkerThread
    @SuppressWarnings("PMD.NPathComplexity")
    public static LogResult createLogTaskLogic(final Geocache cache, final String postId, final Consumer<String> progress) {
        try (ContextLogger cLog = new ContextLogger("LogUtils.createLogTaskLogic(%s, %s)", cache.getGeocode(), postId)) {
            try {
                final CreateLogPostStore.LoadResult loaded = CreateLogPostStore.loadResult(postId);
                if (loaded.status == CreateLogPostStore.LoadStatus.NOT_FOUND) {
                    return LogResult.error(StatusCode.LOG_POST_ERROR, "This saved log upload no longer exists", null);
                }
                if (loaded.status == CreateLogPostStore.LoadStatus.CORRUPT || loaded.state == null) {
                    return LogResult.error(StatusCode.LOG_POST_ERROR,
                            "The saved log upload cannot be read. It was not sent again.", null);
                }
                final CreateLogPostState initialState = loaded.state;
                if (!StringUtils.equalsIgnoreCase(cache.getGeocode(), initialState.geocode)) {
                    return LogResult.error(StatusCode.LOG_POST_ERROR, "The saved log belongs to another cache", null);
                }
                if (initialState.completionStatus != CreateLogPostState.CompletionStatus.ACTIVE) {
                    return LogResult.error(StatusCode.LOG_POST_ERROR, "The saved log is no longer active", null);
                }

                final ILoggingManager loggingManager = cache.getLoggingManager();
                final IConnector cacheConnector = loggingManager.getConnector();
                final List<CreateLogPostStepRunner.Step<CreateLogPostState>> remoteSteps = new ArrayList<>();
                final LogResult[] actionFailure = new LogResult[1];

                remoteSteps.add(new CreateLogPostStepRunner.Step<>(
                        state -> toRunnerStatus(state.mainStatus),
                        (state, status) -> state.mainStatus = fromRunnerStatus(status),
                        state -> {
                            progress.accept(LocalizationUtils.getString(R.string.log_posting_log));
                            final OfflineLogEntry entry = state.toOfflineLogEntry();
                            final LogResult result = loggingManager.createLog(entry.buildUponOfflineLogEntry()
                                    .setLog(trimForPublishing(entry.log)).build(), state.toInventory());
                            actionFailure[0] = result;
                            return outcome(result);
                        }, state -> {
                            state.mainServiceLogId = actionFailure[0].getServiceLogId();
                            actionFailure[0] = null;
                        }));
                remoteSteps.add(new CreateLogPostStepRunner.Step<>(
                        state -> toRunnerStatus(state.problemStatus),
                        (state, status) -> state.problemStatus = fromRunnerStatus(status),
                        state -> {
                            progress.accept(LocalizationUtils.getString(R.string.log_posting_problemreport));
                            final OfflineLogEntry problemLog = new OfflineLogEntry.Builder()
                                    .setLogType(LogType.getById(state.problemLogTypeId)).setDate(state.logDate)
                                    .setLog(state.problemLogText).setPassword(state.password).build();
                            final LogResult result = loggingManager.createLog(problemLog, null);
                            actionFailure[0] = result;
                            return outcome(result);
                        }, state -> {
                            state.problemServiceLogId = actionFailure[0].getServiceLogId();
                            actionFailure[0] = null;
                        }));
                for (int index = 0; index < initialState.images.size(); index++) {
                    final int imageIndex = index;
                    final ImageResult[] imageResult = new ImageResult[1];
                    remoteSteps.add(new CreateLogPostStepRunner.Step<>(
                            state -> toRunnerStatus(state.images.get(imageIndex).status),
                            (state, status) -> state.images.get(imageIndex).status = fromRunnerStatus(status),
                            state -> {
                                final CreateLogPostState.ImageState image = state.images.get(imageIndex);
                                progress.accept(LocalizationUtils.getString(R.string.log_posting_image_create,
                                        String.valueOf(image.position + 1), String.valueOf(state.images.size())));
                                final Image localImage = new Image.Builder().setUrl(Uri.parse(image.localUri))
                                        .setTitle(image.uploadTitle).setDescription(image.description)
                                        .setTargetScale(image.targetScale).build();
                                imageResult[0] = uploadNewImage(loggingManager, state.mainServiceLogId,
                                        localImage, image.position, state.images.size());
                                actionFailure[0] = imageResult[0].isOk() ? null
                                        : LogResult.error(imageResult[0].getStatusCode(), imageResult[0].getPostServerMessage(), null);
                                return outcome(imageResult[0]);
                            }, state -> {
                                final CreateLogPostState.ImageState image = state.images.get(imageIndex);
                                image.serviceImageId = imageResult[0].getServiceImageId();
                                image.remoteUri = imageResult[0].getImageUri();
                                imageResult[0] = null;
                                actionFailure[0] = null;
                            }));
                }

                final CreateLogPostWorkflow.RunResult<CreateLogPostState> remoteResult =
                        CreateLogPostWorkflow.runLoaded(postId,
                                id -> loadCreatePostState(id, cache.getGeocode()),
                                CreateLogPostStore::save, remoteSteps);
                if (remoteResult.result != CreateLogPostWorkflow.Result.COMPLETE) {
                    return workflowFailure(remoteResult.result, actionFailure[0],
                            "A previous request may already have reached the website. Check the website before continuing.");
                }
                final CreateLogPostState state = remoteResult.state;
                if (!state.isRemoteComplete()) {
                    return LogResult.error(StatusCode.LOG_POST_ERROR,
                            "Required remote log steps are incomplete. Nothing else was saved as complete.", null);
                }
                if (state.remoteCompletedAt == 0) {
                    state.remoteCompletedAt = System.currentTimeMillis();
                    if (!CreateLogPostStore.save(state)) {
                        return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not save upload progress", null);
                    }
                }
                final OfflineLogEntry logEntry = state.toOfflineLogEntry();
                final Map<String, Trackable> inventory = state.toInventory();
                final LogResult tailResult = finishCreateLocally(cache, state, loggingManager,
                        cacheConnector, logEntry, inventory, progress);
                if (!tailResult.isOk()) {
                    return tailResult;
                }
                state.completionStatus = CreateLogPostState.CompletionStatus.COMPLETE;
                if (!CreateLogPostStore.save(state)) {
                    return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not save completion state", null);
                }
                if (!CreateLogPostStore.remove(postId)) {
                    Log.w("The completed create-log recovery state could not be removed");
                }
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

    static boolean saveConfirmedCreateLogs(final Geocache cache, final CreateLogPostState state) {
        if (!state.isMainLogConfirmed()) {
            return false;
        }
        final IConnector connector = cache.getLoggingManager().getConnector();
        final List<LogEntry> logs = new ArrayList<>(cache.getLogs());
        removeCreateLogDuplicates(logs, state.mainServiceLogId, state.problemServiceLogId);
        logs.add(0, applyCommonsToOwnLog(state.toOfflineLogEntry().buildUpon(), connector)
                .setServiceLogId(state.mainServiceLogId).setLogImages(state.getConfirmedImages()).build());
        if (state.problemStatus == CreateLogPostState.StepStatus.CONFIRMED) {
            logs.add(0, applyCommonsToOwnLog(new LogEntry.Builder()
                    .setLogType(LogType.getById(state.problemLogTypeId)).setDate(state.logDate)
                    .setLog(state.problemLogText), connector)
                    .setServiceLogId(state.problemServiceLogId).build());
        }
        DataStore.saveLogs(cache.getGeocode(), logs, true);
        final List<LogEntry> saved = DataStore.loadLogs(cache.getGeocode());
        return saved.stream().anyMatch(log -> StringUtils.equals(state.mainServiceLogId, log.serviceLogId))
                && (state.problemStatus != CreateLogPostState.StepStatus.CONFIRMED
                        || saved.stream().anyMatch(log -> StringUtils.equals(state.problemServiceLogId, log.serviceLogId)));
    }

    static void removeCreateLogDuplicates(final List<LogEntry> logs, final String mainServiceLogId,
            @Nullable final String problemServiceLogId) {
        final boolean hasProblemId = StringUtils.isNotBlank(problemServiceLogId);
        logs.removeIf(existing -> StringUtils.equals(mainServiceLogId, existing.serviceLogId)
                || hasProblemId && StringUtils.equals(problemServiceLogId, existing.serviceLogId));
    }

    private static LogResult finishCreateLocally(final Geocache cache, final CreateLogPostState state,
            final ILoggingManager loggingManager, final IConnector cacheConnector,
            final OfflineLogEntry logEntry, final Map<String, Trackable> inventory,
            final Consumer<String> progress) {
        progress.accept(LocalizationUtils.getString(R.string.log_posting_save_internal));
        final LogEntry adaptedMain = applyCommonsToOwnLog(logEntry.buildUpon(), cacheConnector)
                .setServiceLogId(state.mainServiceLogId).setLogImages(state.getConfirmedImages()).build();
        if (!runLocalStep(state, () -> state.localLogsStatus, status -> state.localLogsStatus = status,
                () -> saveConfirmedCreateLogs(cache, state))) {
            return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not save local log state", null);
        }
        if (!runLocalStep(state, () -> state.cacheStateStatus, status -> state.cacheStateStatus = status,
                () -> persistLocalCacheState(cache, loggingManager, adaptedMain))) {
            return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not update local cache state", null);
        }
        if (!runLocalStep(state, () -> state.favoriteStateStatus, status -> state.favoriteStateStatus = status, () -> {
            if (!loggingManager.supportsLogWithFavorite() || !logEntry.favorite) {
                return true;
            }
            final Geocache persisted = DataStore.loadCache(cache.getGeocode(), LoadFlags.LOAD_ALL_DB_ONLY);
            if (persisted == null) {
                return false;
            }
            if (persisted.isFavorite()) {
                cache.setFavorite(true);
                cache.setFavoritePoints(persisted.getFavoritePoints());
                return true;
            }
            cache.setFavorite(true);
            final int targetFavoritePoints = persisted.getFavoritePoints() + 1;
            cache.setFavoritePoints(targetFavoritePoints);
            DataStore.saveChangedCache(cache);
            final Geocache saved = DataStore.loadCache(cache.getGeocode(), LoadFlags.LOAD_ALL_DB_ONLY);
            return saved != null && saved.isFavorite()
                    && saved.getFavoritePoints() == targetFavoritePoints;
        })) {
            return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not update favorite state", null);
        }
        if (!runLocalStep(state, () -> state.trackableStateStatus, status -> state.trackableStateStatus = status, () -> {
            if (loggingManager.supportsLogWithTrackables()) {
                boolean saved = true;
                for (Map.Entry<String, LogTypeTrackable> entry : logEntry.inventoryActions.entrySet()) {
                    saved &= LastTrackableAction.setAction(entry.getKey(), entry.getValue());
                }
                return saved;
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
        final DataStore.ConditionalDeleteResult draftDeleteResult = clearOfflineDraftIfUnchanged(cache, state);
        if (draftDeleteResult == DataStore.ConditionalDeleteResult.FAILED) {
            return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not clear the posted offline draft", null);
        }
        if (!runLocalStep(state, () -> state.localImageCleanupStatus,
                status -> state.localImageCleanupStatus = status, () -> cleanupLocalImages(state))) {
            Log.w("Some uploaded log image files could not be removed");
        }
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

    private static CreateLogPostStepRunner.Status toRunnerStatus(final CreateLogPostState.StepStatus status) {
        return CreateLogPostStepRunner.Status.valueOf(status.name());
    }

    private static CreateLogPostState.StepStatus fromRunnerStatus(final CreateLogPostStepRunner.Status status) {
        return CreateLogPostState.StepStatus.valueOf(status.name());
    }

    private static CreateLogPostWorkflow.LoadResult<CreateLogPostState> loadCreatePostState(
            final String postId, final String expectedGeocode) {
        final CreateLogPostStore.LoadResult loaded = CreateLogPostStore.loadResult(postId);
        switch (loaded.status) {
            case VALID:
                if (loaded.state == null
                        || loaded.state.completionStatus != CreateLogPostState.CompletionStatus.ACTIVE
                        || !StringUtils.equalsIgnoreCase(expectedGeocode, loaded.state.geocode)
                        || !StringUtils.equals(postId, loaded.state.postId)) {
                    return new CreateLogPostWorkflow.LoadResult<>(CreateLogPostWorkflow.LoadStatus.NOT_FOUND, null);
                }
                return new CreateLogPostWorkflow.LoadResult<>(CreateLogPostWorkflow.LoadStatus.VALID, loaded.state);
            case CORRUPT:
                return new CreateLogPostWorkflow.LoadResult<>(CreateLogPostWorkflow.LoadStatus.CORRUPT, null);
            case NOT_FOUND:
            default:
                return new CreateLogPostWorkflow.LoadResult<>(CreateLogPostWorkflow.LoadStatus.NOT_FOUND, null);
        }
    }

    private static CreateLogPostStepRunner.ActionOutcome outcome(final StatusResult result) {
        if (result.isOk()) {
            return CreateLogPostStepRunner.ActionOutcome.SUCCESS;
        }
        return isDefinitiveFailure(result) ? CreateLogPostStepRunner.ActionOutcome.DEFINITIVE_FAILURE
                : CreateLogPostStepRunner.ActionOutcome.UNKNOWN;
    }

    private static LogResult workflowFailure(final CreateLogPostWorkflow.Result result,
            @Nullable final LogResult actionFailure, final String uncertainMessage) {
        switch (result) {
            case STEP_FAILED:
                return actionFailure == null
                        ? LogResult.error(StatusCode.LOG_POST_ERROR, "A log upload step failed", null) : actionFailure;
            case STORAGE_FAILED:
                return LogResult.error(StatusCode.LOG_POST_ERROR, "Could not save upload progress", null);
            case UNCERTAIN:
                return actionFailure != null && !isDefinitiveFailure(actionFailure) ? actionFailure
                        : LogResult.error(StatusCode.LOG_POST_ERROR, uncertainMessage, null);
            case NOT_FOUND:
                return LogResult.error(StatusCode.LOG_POST_ERROR, "This saved log upload no longer exists", null);
            case CORRUPT:
                return LogResult.error(StatusCode.LOG_POST_ERROR,
                        "The saved log upload cannot be read. It was not sent again.", null);
            case COMPLETE:
            default:
                return null;
        }
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
        final CreateLogPostStepRunner.Result result = CreateLogPostStepRunner.run(state,
                CreateLogPostStore::save,
                new CreateLogPostStepRunner.Step<>(ignored -> toRunnerStatus(getter.get()),
                        (ignored, status) -> setter.set(fromRunnerStatus(status)),
                        ignored -> action.get() ? CreateLogPostStepRunner.ActionOutcome.SUCCESS
                                : CreateLogPostStepRunner.ActionOutcome.DEFINITIVE_FAILURE,
                        ignored -> { }, true));
        return result == CreateLogPostStepRunner.Result.CONFIRMED
                || result == CreateLogPostStepRunner.Result.ALREADY_COMPLETE;
    }

    private static boolean runOptionalRemoteStep(final CreateLogPostState state, final StatusGetter getter,
            final StatusSetter setter, final Supplier<Boolean> action) {
        final CreateLogPostStepRunner.Result result = CreateLogPostStepRunner.run(state,
                CreateLogPostStore::save,
                new CreateLogPostStepRunner.Step<>(ignored -> toRunnerStatus(getter.get()),
                        (ignored, status) -> setter.set(fromRunnerStatus(status)),
                        ignored -> action.get() ? CreateLogPostStepRunner.ActionOutcome.SUCCESS
                                : CreateLogPostStepRunner.ActionOutcome.UNKNOWN));
        return result == CreateLogPostStepRunner.Result.CONFIRMED
                || result == CreateLogPostStepRunner.Result.ALREADY_COMPLETE;
    }

    static boolean localCacheStateMatches(final Geocache expected, @Nullable final Geocache actual) {
        return actual != null && actual.isFound() == expected.isFound()
                && actual.isDNF() == expected.isDNF()
                && actual.isArchived() == expected.isArchived()
                && actual.isDisabled() == expected.isDisabled()
                && actual.getVisitedDate() == expected.getVisitedDate()
                && actual.getAttributes().equals(expected.getAttributes());
    }

    private static boolean persistLocalCacheState(final Geocache cache,
            final ILoggingManager loggingManager, final LogEntry adaptedMain) {
        final boolean wasFound = cache.isFound();
        adaptCacheCommonsIdempotently(cache, adaptedMain);
        if (adaptedMain.logType.isFoundLog() && !wasFound && loggingManager.getConnector() instanceof ILogin) {
            ((ILogin) loggingManager.getConnector()).increaseCachesFound(1);
        }
        DataStore.saveChangedCache(cache);
        return localCacheStateMatches(cache,
                DataStore.loadCache(cache.getGeocode(), LoadFlags.LOAD_ALL_DB_ONLY));
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

    static List<Uri> findLocalImagesSafeToDelete(final CreateLogPostState completed,
            @Nullable final OfflineLogEntry currentDraft, final List<CreateLogPostState> activeStates) {
        return completed.images.stream()
                .filter(image -> image.status == CreateLogPostState.StepStatus.CONFIRMED)
                .map(image -> Uri.parse(image.localUri))
                .filter(uri -> currentDraft == null || currentDraft.logImages.stream()
                        .noneMatch(image -> uri.equals(image.getUri())))
                .filter(uri -> activeStates.stream().noneMatch(state -> state.images.stream()
                        .anyMatch(image -> uri.toString().equals(image.localUri))))
                .toList();
    }

    private static boolean cleanupLocalImages(final CreateLogPostState state) {
        final OfflineLogEntry currentDraft = DataStore.loadLogOffline(state.geocode);
        final List<CreateLogPostState> activeStates = CreateLogPostStore.loadActiveForGeocode(state.geocode).stream()
                .filter(active -> !active.postId.equals(state.postId)).toList();
        boolean complete = true;
        for (Uri uri : findLocalImagesSafeToDelete(state, currentDraft, activeStates)) {
            if (ContentStorage.get().getFileInfo(uri) != null && !ImageUtils.deleteImage(uri)) {
                complete = false;
            }
        }
        return complete;
    }

    static DataStore.ConditionalDeleteResult clearOfflineDraftIfUnchanged(final Geocache cache,
            final CreateLogPostState state) {
        final DataStore.ConditionalDeleteResult result = DataStore.clearLogOfflineIfMatches(
                cache.getGeocode(), state::matchesDraft);
        if (result == DataStore.ConditionalDeleteResult.DELETED) {
            cache.setOfflineLog(null);
        }
        return result;
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

    static LogEntry.Builder applyCommonsToOwnLog(final LogEntry.Builder logBuilder, final IConnector cacheConnector) {
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
