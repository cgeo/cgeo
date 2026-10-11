package cgeo.geocaching.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Standalone fault-injection checks for the production resumable-step implementation. */
public final class CreateLogPostWorkflowSelfTest {

    private static final class State {
        CreateLogPostStepRunner.Status main = CreateLogPostStepRunner.Status.PENDING;
        CreateLogPostStepRunner.Status image = CreateLogPostStepRunner.Status.PENDING;
        String remoteId;
    }

    private CreateLogPostWorkflowSelfTest() {
        // utility class
    }

    public static void main(final String[] args) {
        initialSaveFailureDoesNotRunAction();
        confirmationSaveFailurePreventsRetry();
        staleWorkflowDoesNotRecreateRemovedState();
        reloadedFinishedWorkflowDoesNotWrite();
        definitiveFailureReturnsToPending();
        localIdempotentInFlightCanResume();
        localActionFailureIsNotConfirmed();
        multipleStepsResumeOnlyPendingStep();
        System.out.println("CreateLogPostWorkflowSelfTest: PASS (8 scenarios)");
    }

    private static void initialSaveFailureDoesNotRunAction() {
        final State state = new State();
        final AtomicInteger writes = new AtomicInteger();
        final CreateLogPostStepRunner.Result result = CreateLogPostStepRunner.run(state, ignored -> false,
                step(s -> s.main, (s, status) -> s.main = status, s -> {
                    writes.incrementAndGet();
                    return CreateLogPostStepRunner.ActionOutcome.SUCCESS;
                }, s -> s.remoteId = "main"));
        check(result == CreateLogPostStepRunner.Result.STORAGE_FAILED, "initial save must fail");
        check(writes.get() == 0, "remote action ran after initial save failure");
        check(state.main == CreateLogPostStepRunner.Status.PENDING, "in-memory state did not roll back");
    }

    private static void confirmationSaveFailurePreventsRetry() {
        final State state = new State();
        final AtomicInteger saves = new AtomicInteger();
        final AtomicInteger writes = new AtomicInteger();
        final CreateLogPostStepRunner.Step<State> step = step(s -> s.main,
                (s, status) -> s.main = status, s -> {
                    writes.incrementAndGet();
                    return CreateLogPostStepRunner.ActionOutcome.SUCCESS;
                }, s -> s.remoteId = "main");
        final CreateLogPostStepRunner.Result first = CreateLogPostStepRunner.run(state,
                ignored -> saves.incrementAndGet() == 1, step);
        check(first == CreateLogPostStepRunner.Result.CONFIRMATION_STORAGE_FAILED,
                "confirmation save failure was not reported");
        check(state.main == CreateLogPostStepRunner.Status.IN_FLIGHT, "unknown outcome was not retained");
        final CreateLogPostStepRunner.Result second = CreateLogPostStepRunner.run(state, ignored -> true, step);
        check(second == CreateLogPostStepRunner.Result.UNCERTAIN, "IN_FLIGHT step was retried");
        check(writes.get() == 1, "remote action ran twice");
    }

    private static void staleWorkflowDoesNotRecreateRemovedState() {
        final State stale = new State();
        stale.main = CreateLogPostStepRunner.Status.CONFIRMED;
        stale.image = CreateLogPostStepRunner.Status.PENDING;
        final AtomicInteger uploads = new AtomicInteger();
        final List<CreateLogPostStepRunner.Step<State>> steps = new ArrayList<>();
        steps.add(step(s -> s.image, (s, status) -> s.image = status, s -> {
            uploads.incrementAndGet();
            return CreateLogPostStepRunner.ActionOutcome.SUCCESS;
        }, ignored -> { }));
        final CreateLogPostWorkflow.Result result = CreateLogPostWorkflow.run("task",
                ignored -> new CreateLogPostWorkflow.LoadResult<>(CreateLogPostWorkflow.LoadStatus.NOT_FOUND, null),
                ignored -> true, steps, ignored -> { });
        check(result == CreateLogPostWorkflow.Result.NOT_FOUND, "removed state was not rejected");
        check(uploads.get() == 0, "stale page uploaded an image");
    }

    private static void reloadedFinishedWorkflowDoesNotWrite() {
        final State finished = new State();
        final AtomicInteger writes = new AtomicInteger();
        final List<CreateLogPostStepRunner.Step<State>> steps = new ArrayList<>();
        steps.add(step(s -> s.main, (s, status) -> s.main = status, s -> {
            writes.incrementAndGet();
            return CreateLogPostStepRunner.ActionOutcome.SUCCESS;
        }, ignored -> { }));

        final CreateLogPostWorkflow.RunResult<State> result = CreateLogPostWorkflow.runLoaded("task",
                ignored -> new CreateLogPostWorkflow.LoadResult<>(CreateLogPostWorkflow.LoadStatus.NOT_FOUND, null),
                ignored -> true, steps);

        check(result.result == CreateLogPostWorkflow.Result.NOT_FOUND, "finished reload was accepted");
        check(writes.get() == 0, "finished reload executed a remote action");
    }

    private static void definitiveFailureReturnsToPending() {
        final State state = new State();
        final CreateLogPostStepRunner.Result result = CreateLogPostStepRunner.run(state, ignored -> true,
                step(s -> s.main, (s, status) -> s.main = status,
                        s -> CreateLogPostStepRunner.ActionOutcome.DEFINITIVE_FAILURE, ignored -> { }));
        check(result == CreateLogPostStepRunner.Result.ACTION_FAILED, "definitive failure was not reported");
        check(state.main == CreateLogPostStepRunner.Status.PENDING, "definitive failure did not return to PENDING");
    }

    private static void localIdempotentInFlightCanResume() {
        final State state = new State();
        state.main = CreateLogPostStepRunner.Status.IN_FLIGHT;
        final AtomicInteger actions = new AtomicInteger();
        final CreateLogPostStepRunner.Result result = CreateLogPostStepRunner.run(state, ignored -> true,
                step(s -> s.main, (s, status) -> s.main = status, s -> {
                    actions.incrementAndGet();
                    return CreateLogPostStepRunner.ActionOutcome.SUCCESS;
                }, ignored -> { }), true);
        check(result == CreateLogPostStepRunner.Result.CONFIRMED, "local IN_FLIGHT step did not resume");
        check(actions.get() == 1, "local action did not run exactly once on resume");
    }

    private static void localActionFailureIsNotConfirmed() {
        final State state = new State();
        final CreateLogPostStepRunner.Step<State> localStep = new CreateLogPostStepRunner.Step<>(
                s -> s.main, (s, status) -> s.main = status,
                ignored -> CreateLogPostStepRunner.ActionOutcome.DEFINITIVE_FAILURE,
                ignored -> { }, true);

        final CreateLogPostStepRunner.Result result = CreateLogPostStepRunner.run(state, ignored -> true, localStep);

        check(result == CreateLogPostStepRunner.Result.ACTION_FAILED, "local failure was not reported");
        check(state.main == CreateLogPostStepRunner.Status.PENDING, "failed local action was confirmed");
    }

    private static void multipleStepsResumeOnlyPendingStep() {
        final State state = new State();
        state.main = CreateLogPostStepRunner.Status.CONFIRMED;
        final AtomicInteger mainWrites = new AtomicInteger();
        final AtomicInteger imageWrites = new AtomicInteger();
        final List<CreateLogPostStepRunner.Step<State>> steps = new ArrayList<>();
        steps.add(step(s -> s.main, (s, status) -> s.main = status, s -> {
            mainWrites.incrementAndGet();
            return CreateLogPostStepRunner.ActionOutcome.SUCCESS;
        }, ignored -> { }));
        steps.add(step(s -> s.image, (s, status) -> s.image = status, s -> {
            imageWrites.incrementAndGet();
            return CreateLogPostStepRunner.ActionOutcome.SUCCESS;
        }, ignored -> { }));
        final CreateLogPostWorkflow.Result result = CreateLogPostWorkflow.run("task",
                ignored -> new CreateLogPostWorkflow.LoadResult<>(CreateLogPostWorkflow.LoadStatus.VALID, state),
                ignored -> true, steps, ignored -> { });
        check(result == CreateLogPostWorkflow.Result.COMPLETE, "multi-step resume did not complete");
        check(mainWrites.get() == 0, "confirmed main log was repeated");
        check(imageWrites.get() == 1, "pending image was not uploaded exactly once");
    }

    private static CreateLogPostStepRunner.Step<State> step(final java.util.function.Function<State, CreateLogPostStepRunner.Status> getter,
            final java.util.function.BiConsumer<State, CreateLogPostStepRunner.Status> setter,
            final CreateLogPostStepRunner.Action<State> action, final java.util.function.Consumer<State> confirmed) {
        return new CreateLogPostStepRunner.Step<>(getter, setter, action, confirmed);
    }

    private static void check(final boolean value, final String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
