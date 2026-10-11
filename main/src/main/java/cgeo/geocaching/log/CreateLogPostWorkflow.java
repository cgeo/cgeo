package cgeo.geocaching.log;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage-independent workflow used by cache-log creation.
 *
 * Each invocation reloads the task by id. A stale page therefore cannot recreate a completed or
 * abandoned task after another page removed it.
 */
public final class CreateLogPostWorkflow<S> {

    public enum LoadStatus {
        VALID,
        NOT_FOUND,
        CORRUPT
    }

    public enum Result {
        COMPLETE,
        STEP_FAILED,
        UNCERTAIN,
        STORAGE_FAILED,
        NOT_FOUND,
        CORRUPT
    }

    public static final class LoadResult<S> {
        public final LoadStatus status;
        public final S state;

        public LoadResult(final LoadStatus status, final S state) {
            this.status = status;
            this.state = state;
        }
    }

    @FunctionalInterface
    public interface Loader<S> {
        LoadResult<S> load(String taskId);
    }

    @FunctionalInterface
    public interface Remover {
        void remove(String taskId);
    }

    private CreateLogPostWorkflow() {
        // utility class
    }

    public static <S> Result run(final String taskId, final Loader<S> loader,
            final CreateLogPostStepRunner.Persister<S> persister,
            final List<CreateLogPostStepRunner.Step<S>> steps, final Remover remover) {
        final LoadResult<S> loaded = loader.load(taskId);
        if (loaded.status == LoadStatus.NOT_FOUND) {
            return Result.NOT_FOUND;
        }
        if (loaded.status == LoadStatus.CORRUPT || loaded.state == null) {
            return Result.CORRUPT;
        }
        final S state = loaded.state;
        for (CreateLogPostStepRunner.Step<S> step : new ArrayList<>(steps)) {
            final CreateLogPostStepRunner.Result stepResult = CreateLogPostStepRunner.run(state, persister, step);
            switch (stepResult) {
                case CONFIRMED:
                case ALREADY_COMPLETE:
                    break;
                case ACTION_FAILED:
                    return Result.STEP_FAILED;
                case STORAGE_FAILED:
                case CONFIRMATION_STORAGE_FAILED:
                    return Result.STORAGE_FAILED;
                case UNCERTAIN:
                default:
                    return Result.UNCERTAIN;
            }
        }
        remover.remove(taskId);
        return Result.COMPLETE;
    }
}
