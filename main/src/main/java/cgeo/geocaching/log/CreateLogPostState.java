package cgeo.geocaching.log;

import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.utils.JsonUtils;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;

/**
 * Persistent snapshot and progress for creating a cache log.
 *
 * The snapshot is deliberately independent from the mutable offline draft. Once the main log has
 * been submitted, all subsequent work must use this snapshot so a changed draft can never be
 * attached to an already existing online log.
 */
public final class CreateLogPostState {

    public static final int SCHEMA_VERSION = 1;

    public enum StepStatus {
        PENDING,
        IN_FLIGHT,
        CONFIRMED,
        SKIPPED
    }

    public enum CompletionStatus {
        ACTIVE,
        COMPLETE,
        ABANDONED
    }

    public static final class ImageState {
        public String clientId;
        public int position;
        public String localUri;
        public String title;
        public String uploadTitle;
        public String description;
        public int targetScale;
        public StepStatus status = StepStatus.PENDING;
        public String serviceImageId;
        public String remoteUri;

        private String calculateClientId() {
            return position + ":" + StringUtils.defaultString(localUri) + ":"
                    + StringUtils.defaultString(title) + ":" + StringUtils.defaultString(uploadTitle) + ":"
                    + StringUtils.defaultString(description) + ":" + targetScale;
        }
    }

    public static final class TrackableState {
        public String geocode;
        public String trackingCode;
        public String name;
        public int brandId;
        public int actionId;
    }

    public int schemaVersion = SCHEMA_VERSION;
    public String postId;
    public String geocode;
    public long createdAt;
    public long updatedAt;
    public String fingerprint;
    public long remoteCompletedAt;

    public int logTypeId;
    public long logDate;
    public String logText;
    public String password;
    public String reportProblemCode;
    public String imageTitlePrefix;
    public int imageScale;
    public boolean favorite;
    public float rating;

    public boolean separateProblemRequired;
    public int problemLogTypeId;
    public String problemLogText;

    public StepStatus mainStatus = StepStatus.PENDING;
    public String mainServiceLogId;
    public StepStatus problemStatus = StepStatus.SKIPPED;
    public String problemServiceLogId;
    public final List<ImageState> images = new ArrayList<>();
    public final List<TrackableState> trackables = new ArrayList<>();

    public StepStatus localLogsStatus = StepStatus.PENDING;
    public StepStatus cacheStateStatus = StepStatus.PENDING;
    public StepStatus favoriteStateStatus = StepStatus.PENDING;
    public StepStatus trackableStateStatus = StepStatus.PENDING;
    public StepStatus ratingStatus = StepStatus.PENDING;
    public StepStatus genericTrackablesStatus = StepStatus.PENDING;
    public StepStatus localImageCleanupStatus = StepStatus.PENDING;
    public CompletionStatus completionStatus = CompletionStatus.ACTIVE;

    private CreateLogPostState() {
        // use factory methods
    }

    @NonNull
    public static CreateLogPostState create(@NonNull final String geocode, @NonNull final OfflineLogEntry logEntry,
            @Nullable final Map<String, Trackable> inventory, final boolean separateProblemRequired,
            @Nullable final String problemLogText) {
        final CreateLogPostState state = new CreateLogPostState();
        state.postId = UUID.randomUUID().toString();
        state.geocode = geocode;
        state.createdAt = System.currentTimeMillis();
        state.updatedAt = state.createdAt;
        state.logTypeId = logEntry.logType.id;
        state.logDate = logEntry.date;
        state.logText = logEntry.log;
        state.password = logEntry.password;
        state.reportProblemCode = logEntry.reportProblem.code;
        state.imageTitlePrefix = logEntry.imageTitlePraefix;
        state.imageScale = logEntry.imageScale;
        state.favorite = logEntry.favorite;
        state.rating = logEntry.rating;
        state.separateProblemRequired = separateProblemRequired;
        state.problemLogTypeId = separateProblemRequired ? logEntry.reportProblem.logType.id : LogType.UNKNOWN.id;
        state.problemLogText = separateProblemRequired ? StringUtils.defaultString(problemLogText) : "";
        state.problemStatus = separateProblemRequired ? StepStatus.PENDING : StepStatus.SKIPPED;

        int position = 0;
        final int imageCount = logEntry.logImages.size();
        for (Image image : logEntry.logImages) {
            final ImageState imageState = new ImageState();
            imageState.position = position++;
            imageState.localUri = image.getUri().toString();
            imageState.title = image.getTitle();
            imageState.uploadTitle = LogUtils.getLogImageTitle(image, imageState.position, imageCount);
            imageState.description = image.getDescription();
            imageState.targetScale = image.targetScale;
            imageState.clientId = imageState.calculateClientId();
            state.images.add(imageState);
        }

        if (inventory != null) {
            final List<Trackable> sortedInventory = new ArrayList<>(inventory.values());
            sortedInventory.sort(Comparator.comparing(Trackable::getGeocode, Comparator.nullsFirst(String::compareTo)));
            for (Trackable trackable : sortedInventory) {
                final TrackableState trackableState = new TrackableState();
                trackableState.geocode = trackable.getGeocode();
                trackableState.trackingCode = trackable.getTrackingcode();
                trackableState.name = trackable.getName();
                trackableState.brandId = trackable.getBrand().getId();
                final LogTypeTrackable action = logEntry.inventoryActions.get(trackable.getGeocode());
                trackableState.actionId = (action == null ? LogTypeTrackable.DO_NOTHING : action).id;
                state.trackables.add(trackableState);
            }
        }
        state.fingerprint = state.calculateFingerprint();
        return state;
    }

    @NonNull
    public OfflineLogEntry toOfflineLogEntry() {
        final OfflineLogEntry.Builder builder = new OfflineLogEntry.Builder()
                .setLogType(LogType.getById(logTypeId))
                .setDate(logDate)
                .setLog(StringUtils.defaultString(logText))
                .setPassword(password)
                .setReportProblem(ReportProblemType.findByCode(reportProblemCode))
                .setImageTitlePraefix(imageTitlePrefix)
                .setImageScale(imageScale)
                .setFavorite(favorite)
                .setRating(rating);
        for (ImageState image : images) {
            builder.addLogImage(imageToModel(image, false));
        }
        for (TrackableState trackable : trackables) {
            builder.addInventoryAction(trackable.geocode, LogTypeTrackable.getById(trackable.actionId));
        }
        return builder.build();
    }

    @NonNull
    public Map<String, Trackable> toInventory() {
        final Map<String, Trackable> inventory = new HashMap<>();
        for (TrackableState trackableState : trackables) {
            final Trackable trackable = new Trackable();
            trackable.setGeocode(trackableState.geocode);
            trackable.setTrackingcode(trackableState.trackingCode);
            trackable.setName(trackableState.name);
            trackable.forceSetBrand(TrackableBrand.getById(trackableState.brandId));
            inventory.put(trackable.getGeocode(), trackable);
        }
        return inventory;
    }

    @NonNull
    public List<Image> getConfirmedImages() {
        final List<Image> confirmed = new ArrayList<>();
        images.stream()
                .filter(image -> image.status == StepStatus.CONFIRMED)
                .sorted(Comparator.comparingInt(image -> image.position))
                .forEach(image -> confirmed.add(imageToModel(image, true)));
        return confirmed;
    }

    private static Image imageToModel(final ImageState image, final boolean useRemoteUri) {
        final String uri = useRemoteUri && StringUtils.isNotBlank(image.remoteUri) ? image.remoteUri : image.localUri;
        return new Image.Builder()
                .setServiceImageId(image.serviceImageId)
                .setUrl(Uri.parse(StringUtils.defaultString(uri)))
                .setTitle(useRemoteUri ? image.uploadTitle : image.title)
                .setDescription(image.description)
                .setTargetScale(image.targetScale)
                .build();
    }

    public boolean isMainLogConfirmed() {
        return mainStatus == StepStatus.CONFIRMED && StringUtils.isNotBlank(mainServiceLogId);
    }

    public boolean hasUncertainStep() {
        if (mainStatus == StepStatus.IN_FLIGHT || problemStatus == StepStatus.IN_FLIGHT
                || ratingStatus == StepStatus.IN_FLIGHT || genericTrackablesStatus == StepStatus.IN_FLIGHT) {
            return true;
        }
        for (ImageState image : images) {
            if (image.status == StepStatus.IN_FLIGHT) {
                return true;
            }
        }
        return false;
    }

    public boolean isRemoteComplete() {
        if (mainStatus != StepStatus.CONFIRMED
                || !(problemStatus == StepStatus.CONFIRMED || problemStatus == StepStatus.SKIPPED)) {
            return false;
        }
        for (ImageState image : images) {
            if (image.status != StepStatus.CONFIRMED) {
                return false;
            }
        }
        return true;
    }

    public boolean matches(@NonNull final OfflineLogEntry logEntry, @Nullable final Map<String, Trackable> inventory,
            final boolean needsSeparateProblem, @Nullable final String problemText) {
        final CreateLogPostState candidate = create(geocode, logEntry, inventory, needsSeparateProblem, problemText);
        return StringUtils.equals(fingerprint, candidate.fingerprint);
    }

    public boolean matchesDraft(@Nullable final OfflineLogEntry logEntry) {
        if (logEntry == null) {
            return false;
        }
        if (logTypeId != logEntry.logType.id || logDate != logEntry.date
                || !StringUtils.equals(logText, logEntry.log)
                || !StringUtils.equals(password, logEntry.password)
                || !StringUtils.equals(reportProblemCode, logEntry.reportProblem.code)
                || !StringUtils.equals(imageTitlePrefix, logEntry.imageTitlePraefix)
                || imageScale != logEntry.imageScale
                || favorite != logEntry.favorite || Float.compare(rating, logEntry.rating) != 0
                || !matchesInventoryActions(logEntry.inventoryActions)
                || images.size() != logEntry.logImages.size()) {
            return false;
        }
        for (int index = 0; index < images.size(); index++) {
            final ImageState image = images.get(index);
            final Image draftImage = logEntry.logImages.get(index);
            if (!StringUtils.equals(image.localUri, draftImage.getUri().toString())
                    || !StringUtils.equals(image.title, draftImage.getTitle())
                    || !StringUtils.equals(image.description, draftImage.getDescription())
                    || image.targetScale != draftImage.targetScale) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesInventoryActions(final Map<String, LogTypeTrackable> actions) {
        for (TrackableState trackable : trackables) {
            final LogTypeTrackable expected = LogTypeTrackable.getById(trackable.actionId);
            final LogTypeTrackable actual = actions.getOrDefault(trackable.geocode, LogTypeTrackable.DO_NOTHING);
            if (expected != actual) {
                return false;
            }
        }
        return actions.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() != LogTypeTrackable.DO_NOTHING)
                .allMatch(entry -> trackables.stream().anyMatch(trackable -> trackable.geocode.equals(entry.getKey())));
    }

    @NonNull
    public String getSummary() {
        final String text = StringUtils.normalizeSpace(StringUtils.defaultString(logText));
        return text.length() <= 60 ? text : text.substring(0, 57) + "…";
    }

    @NonNull
    public String toJson() {
        return JsonUtils.nodeToString(toNode(), false);
    }

    @Nullable
    public static CreateLogPostState fromJson(@Nullable final String json) {
        final JsonNode root = JsonUtils.stringToNode(json, true);
        if (root == null || JsonUtils.getInt(root, "schemaVersion", -1) != SCHEMA_VERSION) {
            return null;
        }
        try {
            final CreateLogPostState state = new CreateLogPostState();
            state.schemaVersion = JsonUtils.getInt(root, "schemaVersion", SCHEMA_VERSION);
            state.postId = JsonUtils.getText(root, "postId", null);
            state.geocode = JsonUtils.getText(root, "geocode", null);
            state.createdAt = getLong(root, "createdAt", 0);
            state.updatedAt = getLong(root, "updatedAt", 0);
            state.fingerprint = JsonUtils.getText(root, "fingerprint", null);
            state.remoteCompletedAt = getLong(root, "remoteCompletedAt", 0);
            state.logTypeId = JsonUtils.getInt(root, "logTypeId", LogType.UNKNOWN.id);
            state.logDate = getLong(root, "logDate", 0);
            state.logText = JsonUtils.getText(root, "logText", "");
            state.password = JsonUtils.getText(root, "password", null);
            state.reportProblemCode = JsonUtils.getText(root, "reportProblemCode", ReportProblemType.NO_PROBLEM.code);
            state.imageTitlePrefix = JsonUtils.getText(root, "imageTitlePrefix", "");
            state.imageScale = JsonUtils.getInt(root, "imageScale", -1);
            state.favorite = JsonUtils.getBoolean(root, "favorite", false);
            state.rating = JsonUtils.getFloat(root, "rating", 0f);
            state.separateProblemRequired = JsonUtils.getBoolean(root, "separateProblemRequired", false);
            state.problemLogTypeId = JsonUtils.getInt(root, "problemLogTypeId", LogType.UNKNOWN.id);
            state.problemLogText = JsonUtils.getText(root, "problemLogText", "");
            state.mainStatus = getRequiredStatus(root, "mainStatus");
            state.mainServiceLogId = JsonUtils.getText(root, "mainServiceLogId", null);
            state.problemStatus = getRequiredStatus(root, "problemStatus");
            state.problemServiceLogId = JsonUtils.getText(root, "problemServiceLogId", null);
            state.localLogsStatus = getRequiredStatus(root, "localLogsStatus");
            state.cacheStateStatus = getRequiredStatus(root, "cacheStateStatus");
            state.favoriteStateStatus = getRequiredStatus(root, "favoriteStateStatus");
            state.trackableStateStatus = getRequiredStatus(root, "trackableStateStatus");
            state.ratingStatus = getRequiredStatus(root, "ratingStatus");
            state.genericTrackablesStatus = getRequiredStatus(root, "genericTrackablesStatus");
            state.localImageCleanupStatus = getRequiredStatus(root, "localImageCleanupStatus");
            state.completionStatus = getRequiredCompletion(root, "completionStatus");

            final JsonNode imagesNode = JsonUtils.get(root, "images");
            if (imagesNode != null) {
                for (JsonNode imageNode : imagesNode) {
                    final ImageState image = new ImageState();
                    image.clientId = JsonUtils.getText(imageNode, "clientId", null);
                    image.position = JsonUtils.getInt(imageNode, "position", state.images.size());
                    image.localUri = JsonUtils.getText(imageNode, "localUri", "");
                    image.title = JsonUtils.getText(imageNode, "title", null);
                    image.uploadTitle = JsonUtils.getText(imageNode, "uploadTitle", null);
                    image.description = JsonUtils.getText(imageNode, "description", null);
                    image.targetScale = JsonUtils.getInt(imageNode, "targetScale", -1);
                    image.status = getRequiredStatus(imageNode, "status");
                    image.serviceImageId = JsonUtils.getText(imageNode, "serviceImageId", null);
                    image.remoteUri = JsonUtils.getText(imageNode, "remoteUri", null);
                    state.images.add(image);
                }
            }
            final JsonNode trackablesNode = JsonUtils.get(root, "trackables");
            if (trackablesNode != null) {
                for (JsonNode trackableNode : trackablesNode) {
                    final TrackableState trackable = new TrackableState();
                    trackable.geocode = JsonUtils.getText(trackableNode, "geocode", "");
                    trackable.trackingCode = JsonUtils.getText(trackableNode, "trackingCode", null);
                    trackable.name = JsonUtils.getText(trackableNode, "name", null);
                    trackable.brandId = JsonUtils.getInt(trackableNode, "brandId", TrackableBrand.UNKNOWN.getId());
                    trackable.actionId = JsonUtils.getInt(trackableNode, "actionId", LogTypeTrackable.DO_NOTHING.id);
                    state.trackables.add(trackable);
                }
            }
            if (StringUtils.isAnyBlank(state.postId, state.geocode, state.fingerprint)
                    || state.logDate <= 0
                    || LogType.getById(state.logTypeId) == LogType.UNKNOWN
                    || state.images.stream().map(image -> image.position).distinct().count() != state.images.size()
                    || state.trackables.stream().anyMatch(trackable -> StringUtils.isBlank(trackable.geocode))
                    || state.mainStatus == StepStatus.SKIPPED
                    || state.problemStatus == StepStatus.SKIPPED && state.separateProblemRequired
                    || state.images.stream().anyMatch(image -> image.status == StepStatus.SKIPPED)
                    || state.mainStatus == StepStatus.CONFIRMED && StringUtils.isBlank(state.mainServiceLogId)
                    || state.mainStatus != StepStatus.CONFIRMED && StringUtils.isNotBlank(state.mainServiceLogId)
                    || state.problemStatus == StepStatus.CONFIRMED && StringUtils.isBlank(state.problemServiceLogId)
                    || state.problemStatus != StepStatus.CONFIRMED && StringUtils.isNotBlank(state.problemServiceLogId)
                    || state.images.stream().anyMatch(image -> StringUtils.isBlank(image.uploadTitle)
                            || !StringUtils.equals(image.clientId, image.calculateClientId())
                            || image.status == StepStatus.CONFIRMED && StringUtils.isBlank(image.remoteUri)
                            || image.status != StepStatus.CONFIRMED
                                    && (StringUtils.isNotBlank(image.remoteUri) || StringUtils.isNotBlank(image.serviceImageId)))
                    || !StringUtils.equals(state.fingerprint, state.calculateFingerprint())) {
                return null;
            }
            if (!state.separateProblemRequired && state.problemStatus != StepStatus.SKIPPED) {
                return null;
            }
            return state;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private ObjectNode toNode() {
        final ObjectNode root = fingerprintNode();
        final ArrayNode imageArray = (ArrayNode) root.get("images");
        final List<ImageState> sortedImages = images.stream()
                .sorted(Comparator.comparingInt(candidate -> candidate.position)).toList();
        for (int index = 0; index < sortedImages.size(); index++) {
            final ImageState image = sortedImages.get(index);
            final ObjectNode node = (ObjectNode) imageArray.get(index);
            node.put("clientId", image.clientId);
            node.put("status", image.status.name());
            putNullable(node, "serviceImageId", image.serviceImageId);
            putNullable(node, "remoteUri", image.remoteUri);
        }
        root.put("schemaVersion", schemaVersion);
        root.put("postId", postId);
        root.put("createdAt", createdAt);
        root.put("updatedAt", updatedAt);
        root.put("fingerprint", fingerprint);
        root.put("remoteCompletedAt", remoteCompletedAt);
        root.put("mainStatus", mainStatus.name());
        putNullable(root, "mainServiceLogId", mainServiceLogId);
        root.put("problemStatus", problemStatus.name());
        putNullable(root, "problemServiceLogId", problemServiceLogId);
        root.put("localLogsStatus", localLogsStatus.name());
        root.put("cacheStateStatus", cacheStateStatus.name());
        root.put("favoriteStateStatus", favoriteStateStatus.name());
        root.put("trackableStateStatus", trackableStateStatus.name());
        root.put("ratingStatus", ratingStatus.name());
        root.put("genericTrackablesStatus", genericTrackablesStatus.name());
        root.put("localImageCleanupStatus", localImageCleanupStatus.name());
        root.put("completionStatus", completionStatus.name());
        return root;
    }

    private ObjectNode fingerprintNode() {
        final ObjectNode snapshot = JsonUtils.createObjectNode();
        snapshot.put("geocode", geocode);
        snapshot.put("logTypeId", logTypeId);
        snapshot.put("logDate", logDate);
        snapshot.put("logText", StringUtils.defaultString(logText));
        putNullable(snapshot, "password", password);
        snapshot.put("reportProblemCode", StringUtils.defaultString(reportProblemCode));
        snapshot.put("imageTitlePrefix", StringUtils.defaultString(imageTitlePrefix));
        snapshot.put("imageScale", imageScale);
        snapshot.put("favorite", favorite);
        snapshot.put("rating", rating);
        snapshot.put("separateProblemRequired", separateProblemRequired);
        snapshot.put("problemLogTypeId", problemLogTypeId);
        snapshot.put("problemLogText", StringUtils.defaultString(problemLogText));
        final ArrayNode imageArray = snapshot.putArray("images");
        images.stream().sorted(Comparator.comparingInt(image -> image.position)).forEach(image -> {
            final ObjectNode node = imageArray.addObject();
            node.put("position", image.position);
            node.put("localUri", StringUtils.defaultString(image.localUri));
            putNullable(node, "title", image.title);
            putNullable(node, "uploadTitle", image.uploadTitle);
            putNullable(node, "description", image.description);
            node.put("targetScale", image.targetScale);
        });
        final ArrayNode trackableArray = snapshot.putArray("trackables");
        trackables.stream().sorted(Comparator.comparing(trackable -> trackable.geocode, Comparator.nullsFirst(String::compareTo))).forEach(trackable -> {
            final ObjectNode node = trackableArray.addObject();
            node.put("geocode", StringUtils.defaultString(trackable.geocode));
            putNullable(node, "trackingCode", trackable.trackingCode);
            putNullable(node, "name", trackable.name);
            node.put("brandId", trackable.brandId);
            node.put("actionId", trackable.actionId);
        });
        return snapshot;
    }

    private String calculateFingerprint() {
        final ObjectNode snapshot = fingerprintNode();
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(JsonUtils.nodeToString(snapshot, false).getBytes(StandardCharsets.UTF_8));
            final StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                result.append(String.format(Locale.US, "%02x", b & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static void putNullable(final ObjectNode node, final String name, @Nullable final String value) {
        if (value == null) {
            node.putNull(name);
        } else {
            node.put(name, value);
        }
    }

    private static long getLong(final JsonNode node, final String name, final long defaultValue) {
        final JsonNode value = JsonUtils.get(node, name);
        return value != null && value.isNumber() ? value.longValue() : defaultValue;
    }

    private static StepStatus getRequiredStatus(final JsonNode node, final String name) {
        final String value = JsonUtils.getText(node, name, null);
        if (value == null) {
            throw new IllegalArgumentException("Missing step status: " + name);
        }
        return StepStatus.valueOf(value);
    }

    private static CompletionStatus getRequiredCompletion(final JsonNode node, final String name) {
        final String value = JsonUtils.getText(node, name, null);
        if (value == null) {
            throw new IllegalArgumentException("Missing completion status: " + name);
        }
        return CompletionStatus.valueOf(value);
    }
}
