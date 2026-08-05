package cgeo.geocaching.utils.workertask;

import static cgeo.geocaching.utils.workertask.WorkerTask.WorkerTaskEventType.CANCELLED;
import static cgeo.geocaching.utils.workertask.WorkerTask.WorkerTaskEventType.ERROR;
import static cgeo.geocaching.utils.workertask.WorkerTask.WorkerTaskEventType.RESULT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.schedulers.SingleScheduler;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class WorkerTaskTest {

    public static final Scheduler taskScheduler = new SingleScheduler();
    public static final Scheduler observerScheduler = new SingleScheduler();

    @Test(timeout = 500)
    public void taskFinished() {
        //A simple task: executes and finishes normally
        final TestFeature testFeature = new TestFeature("taskFinished", RESULT);
        final CountDownLatch startLatcn = new CountDownLatch(1);
        final WorkerTask<String, String, String> task = WorkerTask.of("task-finished", input -> Observable.create(emit -> {
                await(startLatcn);
                emit.onNext(WorkerTask.TaskValue.progress("one"));
                emit.onNext(WorkerTask.TaskValue.progress("two"));
                emit.onNext(WorkerTask.TaskValue.result("end"));
            }), taskScheduler, observerScheduler);
        task.addFeature(testFeature);

        assertThat(task.isRunning()).isFalse();
        task.start("input");
        assertThat(task.isRunning()).isTrue(); //need to check this before opening "startLatch"
        startLatcn.countDown();
        await(testFeature.endLatch);
        assertThat(task.isRunning()).isFalse();
        assertThat(removeTaskIds(testFeature.collectedEvents)).containsExactly("STARTED:I=input", "PROGRESS:P=one", "PROGRESS:P=two", "RESULT:R=end");

        testFeature.dispose();
    }

    @Test(timeout = 500)
    public void taskCancelAndRestart() {
        //An endlessly running task which is cancelled two times
        final TestFeature testFeature = new TestFeature("taskCancelled", CANCELLED, CANCELLED);
        final WorkerTask<String, String, String> task = WorkerTask.of("task-cancelrestart", input -> Observable.create(emit -> {
            int cnt = 0;
            while (!emit.isDisposed()) {
                log("Emitting " + cnt);
                emit.onNext(WorkerTask.TaskValue.progress(String.valueOf(cnt++)));
                sleep(20);
            }
            emit.onNext(WorkerTask.TaskValue.result("end"));

//            WorkerTask.toObservable(
//                () -> (String input, Consumer<String> progress, Supplier<Boolean> isCancelled) -> {
//            int cnt = 0;
//            while (!isCancelled.get()) {
//                progress.accept(String.valueOf(cnt++));
//                sleep(20);
//            }
//            return "end";
        }), taskScheduler, observerScheduler);
        task.addFeature(testFeature);
        assertThat(task.isRunning()).isFalse();
        task.start("input");
        assertThat(task.isRunning()).isTrue(); //task runs endless
        sleep(50);
        task.start("input2"); // cancels then restarts the task
        assertThat(task.isRunning()).isTrue(); //task runs endless
        sleep(50);
        task.cancel();
        assertThat(task.isRunning()).isFalse();
        await(testFeature.endLatch);

        //check collected events. Is something like the following, but we can't exactly tell how many "progress" messages go through through:
        //"STARTED:I=input", "PROGRESS:P=0", "PROGRESS:P=1", "PROGRESS:P=2", ..., "CANCELLED", "STARTED:I=input2", "PROGRESS:P=0", "PROGRESS:P=1", ..., "CANCELLED"
        assertCollectedEvents(testFeature.collectedEvents, String::valueOf, "STARTED:I=input", "CANCELLED", "STARTED:I=input2", "CANCELLED");

        testFeature.dispose();
    }

    @Test(timeout = 5000)
    public void taskWithException() {
        //A task throwing an exception
        final TestFeature testFeature = new TestFeature("taskWithException", ERROR);
        final WorkerTask<String, String, String> task = WorkerTask.of("task-exception", input -> Observable.create(emit -> {
            emit.onNext(WorkerTask.TaskValue.progress("one"));
            throw new IllegalStateException("my test exception");
        }), taskScheduler, observerScheduler);
        task.addFeature(testFeature);

        task.start("input");
        await(testFeature.endLatch);
        assertThat(task.isRunning()).isFalse();

        assertThat(removeTaskIds(testFeature.collectedEvents)).containsExactly("STARTED:I=input", "PROGRESS:P=one", "ERROR:EX=java.lang.IllegalStateException: my test exception");
    }

    @Test
    public void removeUnusedTasks() {
        final WorkerTask<String, String, String> task = WorkerTask.of("task-unused", input -> Observable.create(emit -> {
            //do nothing
        }), taskScheduler, observerScheduler);
        assertThat(WorkerTask.get("task-unused")).isSameAs(task);
        task.finish();
        assertThat(WorkerTask.get("task-unused")).isNull();
    }

    private static List<String> removeTaskIds(final List<String> events) {
        return  events.stream()
            .map(s -> s.substring(0, s.indexOf("{"))) // remove the task ids
            .collect(Collectors.toList());
    }
    private static void assertCollectedEvents(final List<String> collectedEvents, final Function<Integer, String> progresMsgMapper, final String ... expectedWithProgressRemoved) {
        final List<String> events = removeTaskIds(collectedEvents);
        final String eventErrorMsg = "Events: " + events;
        //checks and removes progress events: must be after STARTED and then starting from 0 in 1-er-steps
        final Iterator<String> it = events.listIterator();
        int progressCnt = -1;
        while (it.hasNext()) {
            final String msg = it.next();
            if (msg.startsWith("STARTED")) {
                progressCnt = 0;
            } else if (msg.startsWith("PROGRESS")) {
                assertThat(msg).as(eventErrorMsg + ", problem at " + msg).isEqualTo("PROGRESS:P=" + progresMsgMapper.apply(progressCnt));
                progressCnt++;
                it.remove();
            } else {
                progressCnt = -1;
            }
        }
        //check events with removed Progress-events
        assertThat(events).containsExactly(expectedWithProgressRemoved);
    }


    private static class TestFeature implements WorkerTask.TaskFeature<String, String, String> {

        private final List<WorkerTask.WorkerTaskEventType> expectedTypes;
        private int expectedIndex = 0;
        private final String logPraefix;
        public final List<String> collectedEvents = new ArrayList<>();
        public final CountDownLatch endLatch = new CountDownLatch(1);

        private Disposable obsDis;

        private WorkerTask<? extends String, ? extends String, ? extends String> task;

        TestFeature(final String logPraefix, final WorkerTask.WorkerTaskEventType ... expectedTypes) {
            this.expectedTypes = expectedTypes.length == 0 ? Collections.singletonList(RESULT) : Arrays.asList(expectedTypes);
            this.logPraefix = logPraefix;
        }

        @Override
        public void accept(final WorkerTask<? extends String, ? extends String, ? extends String> task) {
            this.task = task;
            obsDis = task.observeForever(event -> {
                collectedEvents.add(event.toString());

                log(logPraefix + ": Received event: " + event);

                //check whether run shall end
                if (event.type == CANCELLED || event.type == RESULT || event.type == ERROR) {
                    final WorkerTask.WorkerTaskEventType expectedType = expectedTypes.get(expectedIndex++);
                    if (event.type != expectedType || expectedIndex >= expectedTypes.size()) {
                        endLatch.countDown();
                    }
                }
            });
        }

        public void dispose() {
            obsDis.dispose();
            task.finish();
            assertThat(WorkerTask.get(task.getGlobalId())).isNull();
        }
    }

    private static void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            //ignore
        }
    }

    private static void await(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ignore) {
            //ignore
        }
    }
    
    private static void log(final String message) {
        System.out.println(message);
    }

}
