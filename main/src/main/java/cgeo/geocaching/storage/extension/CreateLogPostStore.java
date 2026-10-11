package cgeo.geocaching.storage.extension;

import cgeo.geocaching.log.CreateLogPostState;
import cgeo.geocaching.storage.DataStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/** Persistent store for resumable cache log creation attempts. */
public final class CreateLogPostStore extends DataStore.DBExtension {

    public enum LoadStatus {
        NOT_FOUND,
        VALID,
        CORRUPT
    }

    public static final class LoadResult {
        public final LoadStatus status;
        @Nullable public final CreateLogPostState state;
        @Nullable public final String geocode;

        private LoadResult(final LoadStatus status, @Nullable final CreateLogPostState state,
                @Nullable final String geocode) {
            this.status = status;
            this.state = state;
            this.geocode = geocode;
        }
    }

    public static final class Candidate {
        public final String postId;
        public final String geocode;
        public final boolean readable;
        @Nullable public final CreateLogPostState state;
        public final long updatedAt;

        private Candidate(final String postId, final String geocode, final boolean readable,
                @Nullable final CreateLogPostState state, final long updatedAt) {
            this.postId = postId;
            this.geocode = geocode;
            this.readable = readable;
            this.state = state;
            this.updatedAt = updatedAt;
        }
    }

    private static final DataStore.DBExtensionType TYPE = DataStore.DBExtensionType.DBEXTENSION_CREATE_LOG_POST;

    private CreateLogPostStore() {
        // utility class
    }

    public static boolean save(@NonNull final CreateLogPostState state) {
        state.updatedAt = System.currentTimeMillis();
        return replaceAll(TYPE, state.postId, state.updatedAt, 0, 0, 0,
                state.toJson(), state.geocode, "", "") != null;
    }

    @NonNull
    public static LoadResult loadResult(@Nullable final String postId) {
        if (StringUtils.isBlank(postId)) {
            return new LoadResult(LoadStatus.NOT_FOUND, null, null);
        }
        final DataStore.DBExtension extension = load(TYPE, postId);
        if (extension == null) {
            return new LoadResult(LoadStatus.NOT_FOUND, null, null);
        }
        final CreateLogPostState state = CreateLogPostState.fromJson(extension.getString1());
        return state == null
                ? new LoadResult(LoadStatus.CORRUPT, null, extension.getString2())
                : new LoadResult(LoadStatus.VALID, state, state.geocode);
    }

    @Nullable
    public static CreateLogPostState load(@Nullable final String postId) {
        return loadResult(postId).state;
    }

    @NonNull
    public static List<Candidate> loadCandidatesForGeocode(@Nullable final String geocode) {
        final List<Candidate> result = new ArrayList<>();
        if (StringUtils.isBlank(geocode)) {
            return result;
        }
        for (DataStore.DBExtension extension : getAll(TYPE, null)) {
            if (!geocode.equalsIgnoreCase(extension.getString2())) {
                continue;
            }
            final CreateLogPostState state = CreateLogPostState.fromJson(extension.getString1());
            if (state != null && state.completionStatus != CreateLogPostState.CompletionStatus.ACTIVE) {
                continue;
            }
            result.add(new Candidate(extension.getKey(), extension.getString2(), state != null,
                    state, extension.getLong1()));
        }
        result.sort(Comparator.comparingLong((Candidate candidate) -> candidate.updatedAt).reversed());
        return result;
    }

    @NonNull
    public static List<CreateLogPostState> loadActiveForGeocode(@Nullable final String geocode) {
        final List<CreateLogPostState> result = new ArrayList<>();
        for (Candidate candidate : loadCandidatesForGeocode(geocode)) {
            if (candidate.state != null
                    && candidate.state.completionStatus == CreateLogPostState.CompletionStatus.ACTIVE) {
                result.add(candidate.state);
            }
        }
        return result;
    }

    public static void remove(@NonNull final String postId) {
        removeAll(TYPE, postId);
    }
}
