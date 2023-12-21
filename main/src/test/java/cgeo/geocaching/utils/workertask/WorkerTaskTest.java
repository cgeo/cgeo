package cgeo.geocaching.utils.workertask;

import static cgeo.geocaching.utils.workertask.WorkerTask.WorkerTaskEventType.CANCELLED;
import static cgeo.geocaching.utils.workertask.WorkerTask.WorkerTaskEventType.ERROR;
import static cgeo.geocaching.utils.workertask.WorkerTask.WorkerTaskEventType.FINISHED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.internal.schedulers.SingleScheduler;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

public class WorkerTaskTest {

    @Test(timeout = 500)
    public void taskFinished() {
        //A simple task: executes and finishes normally
        final TestFeature testFeature = new TestFeature("taskFinished", FINISHED);
        final CountDownLatch startLatcn = new CountDownLatch(1);
        final WorkerTask<String, String, String> task = WorkerTask.of(null, () -> (String input, Consumer<String> progress, Supplier<Boolean> isCancelled) -> {
            await(startLatcn);
            progress.accept("one");
            progress.accept("two");
            return "end";
        });
        task.addFeature(testFeature);

        assertThat(task.isRunning()).isFalse();
        task.start("input");
        assertThat(task.isRunning()).isTrue(); //need to check this before opening "startLatch"
        startLatcn.countDown();
        await(testFeature.endLatch);
        assertThat(task.isRunning()).isFalse();
        assertThat(removeTaskIds(testFeature.collectedEvents)).containsExactly("STARTED:I=input", "PROGRESS:P=one", "PROGRESS:P=two", "FINISHED:R=end");
    }

    @Test(timeout = 500)
    public void taskCancelAndRestart() {
        //An endlessly running task which is cancelled two times
        final TestFeature testFeature = new TestFeature("taskCancelled", CANCELLED, CANCELLED);
        final WorkerTask<String, String, String> task = WorkerTask.of(null, () -> (String input, Consumer<String> progress, Supplier<Boolean> isCancelled) -> {
            int cnt = 0;
            while (!isCancelled.get()) {
                progress.accept(String.valueOf(cnt++));
                sleep(20);
            }
            return "end";
        });
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
    }

    @Test(timeout = 500)
    public void taskDispose() {
        //An endlessly running task which is ended when an additional flag is set
        final TestFeature testFeature = new TestFeature("taskDispose", FINISHED);
        final AtomicBoolean endTask = new AtomicBoolean(false);
        final String[] store = new String[1];

        final WorkerTask<String, String, String> task = WorkerTask.of(null, () -> (String input, Consumer<String> progress, Supplier<Boolean> isCancelled) -> {
            int cnt = 0;
            while (!isCancelled.get() && !endTask.get()) {
                progress.accept(String.valueOf(cnt++));
                sleep(20);
            }
            return "end";
        }).setNoOwnerAction(s -> {
            store[0] = s;
            testFeature.endLatch.countDown();
        });

        task.addFeature(testFeature);
        assertThat(task.isRunning()).isFalse();

        task.start("input");
        sleep(100);

        task.dispose(false); //do not cancel! -> task continues running but is detached
        assertThat(task.isRunning()).isTrue();
        sleep(50);

        //let the detached task end
        endTask.set(true);
        await(testFeature.endLatch);
        assertThat(task.isRunning()).isFalse();

        //check collected events. Is something like the following, but we can't exactly tell how many "progress" messages go through through:
        //"STARTED:I=input", "PROGRESS:P=0", "PROGRESS:P=1", "PROGRESS:P=2", ..., "CANCELLED", "STARTED:I=input2", "PROGRESS:P=0", "PROGRESS:P=1", ..., "CANCELLED"
        assertCollectedEvents(testFeature.collectedEvents, String::valueOf, "STARTED:I=input");

        assertThat(store[0]).isEqualTo("end");

    }

    @Test(timeout = 500)
    public void taskWithException() {
        //A task throwing an exception
        final TestFeature testFeature = new TestFeature("taskWithException", ERROR);
        final WorkerTask<String, String, String> task = WorkerTask.of(null, () -> (String input, Consumer<String> progress, Supplier<Boolean> isCancelled) -> {
            progress.accept("one");
            throw new IllegalStateException("my test exception");
        });
        task.addFeature(testFeature);

        task.start("input");
        await(testFeature.endLatch);
        assertThat(task.isRunning()).isFalse();

        assertThat(removeTaskIds(testFeature.collectedEvents)).containsExactly("STARTED:I=input", "PROGRESS:P=one", "ERROR:EX=java.lang.IllegalStateException: my test exception");
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

        public static final Scheduler taskScheduler = new SingleScheduler();
        public static final Scheduler listenerScheduler = new SingleScheduler();

        private final List<WorkerTask.WorkerTaskEventType> expectedTypes;
        private int expectedIndex = 0;
        private final String logPraefix;
        public final List<String> collectedEvents = new ArrayList<>();
        public final CountDownLatch endLatch = new CountDownLatch(1);

        TestFeature(final String logPraefix, final WorkerTask.WorkerTaskEventType ... expectedTypes) {
            this.expectedTypes = expectedTypes.length == 0 ? Collections.singletonList(FINISHED) : Arrays.asList(expectedTypes);
            this.logPraefix = logPraefix;
        }

        @Override
        public void accept(final WorkerTask<? extends String, ? extends String, ? extends String> task) {
            task.addTaskListener(event -> {
                collectedEvents.add(event.toString());

                log(logPraefix + ": Received event: " + event);

                //check whether run shall end
                if (event.type == CANCELLED || event.type == FINISHED || event.type == ERROR) {
                    final WorkerTask.WorkerTaskEventType expectedType = expectedTypes.get(expectedIndex++);
                    if (event.type != expectedType || expectedIndex >= expectedTypes.size()) {
                        task.dispose(true);
                        endLatch.countDown();
                    }
                }
            })
            .setTaskScheduler(taskScheduler)
            .setListenerScheduler(listenerScheduler);
        }
    }

    private static void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            fail("Unexpected interrupt wile sleeping", ie);
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
