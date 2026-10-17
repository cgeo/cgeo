package cgeo.geocaching.log;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Executes one persisted step of cache log creation.
 *
 * The IN_FLIGHT state is written before the action. If a process stops or the outcome is unknown,
 * the next run refuses to execute the action again. A failed confirmation write also leaves the
 * durable state at IN_FLIGHT, preventing duplicate remote writes.
 */
public final class CreateLogPostStepRunner {

    public enum Status {
        PENDING,
        IN_FLIGHT,
        CONFIRMED,
        SKIPPED
    }

    public enum ActionOutcome {
        SUCCESS,
        DEFINITIVE_FAILURE,
        UNKNOWN
    }

    public enum Result {
        CONFIRMED,
        ALREADY_COMPLETE,
        ACTION_FAILED,
        UNCERTAIN,
        STORAGE_FAILED,
        CONFIRMATION_STORAGE_FAILED
    }

    @FunctionalInterface
    public interface Persister<S> {
        boolean save(S state);
    }

    @FunctionalInterface
    public interface Action<S> {
        ActionOutcome run(S state);
    }

    public static final class Step<S> {
        private final Function<S, Status> statusGetter;
        private final BiConsumer<S, Status> statusSetter;
        private final Action<S> action;
        private final Consumer<S> onConfirmed;
        private final boolean retryInFlight;

        public Step(final Function<S, Status> statusGetter, final BiConsumer<S, Status> statusSetter,
                final Action<S> action) {
            this(statusGetter, statusSetter, action, ignored -> { }, false);
        }

        public Step(final Function<S, Status> statusGetter, final BiConsumer<S, Status> statusSetter,
                final Action<S> action, final Consumer<S> onConfirmed) {
            this(statusGetter, statusSetter, action, onConfirmed, false);
        }

        public Step(final Function<S, Status> statusGetter, final BiConsumer<S, Status> statusSetter,
                final Action<S> action, final Consumer<S> onConfirmed, final boolean retryInFlight) {
            this.statusGetter = statusGetter;
            this.statusSetter = statusSetter;
            this.action = action;
            this.onConfirmed = onConfirmed;
            this.retryInFlight = retryInFlight;
        }
    }

    private CreateLogPostStepRunner() {
        // utility class
    }

    public static <S> Result run(final S state, final Persister<S> persister, final Step<S> step) {
        return run(state, persister, step, step.retryInFlight);
    }

    /**
     * @param retryInFlight only for local idempotent actions; remote writes must keep this false
     */
    public static <S> Result run(final S state, final Persister<S> persister, final Step<S> step,
            final boolean retryInFlight) {
        final Status current = step.statusGetter.apply(state);
        if (current == Status.CONFIRMED || current == Status.SKIPPED) {
            return Result.ALREADY_COMPLETE;
        }
        if (current == Status.IN_FLIGHT && !retryInFlight) {
            return Result.UNCERTAIN;
        }
        if (current == Status.IN_FLIGHT) {
            step.statusSetter.accept(state, Status.PENDING);
            if (!persister.save(state)) {
                step.statusSetter.accept(state, Status.IN_FLIGHT);
                return Result.STORAGE_FAILED;
            }
        }

        step.statusSetter.accept(state, Status.IN_FLIGHT);
        if (!persister.save(state)) {
            step.statusSetter.accept(state, Status.PENDING);
            return Result.STORAGE_FAILED;
        }

        final ActionOutcome outcome;
        try {
            outcome = step.action.run(state);
        } catch (RuntimeException ignored) {
            return Result.UNCERTAIN;
        }
        if (outcome == ActionOutcome.UNKNOWN) {
            return Result.UNCERTAIN;
        }
        if (outcome == ActionOutcome.DEFINITIVE_FAILURE) {
            step.statusSetter.accept(state, Status.PENDING);
            if (!persister.save(state)) {
                step.statusSetter.accept(state, Status.IN_FLIGHT);
                return Result.STORAGE_FAILED;
            }
            return Result.ACTION_FAILED;
        }

        step.onConfirmed.accept(state);
        step.statusSetter.accept(state, Status.CONFIRMED);
        if (!persister.save(state)) {
            step.statusSetter.accept(state, Status.IN_FLIGHT);
            return Result.CONFIRMATION_STORAGE_FAILED;
        }
        return Result.CONFIRMED;
    }
}
