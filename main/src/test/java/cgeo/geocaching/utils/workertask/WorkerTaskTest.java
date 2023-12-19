package cgeo.geocaching.utils.workertask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.internal.schedulers.SingleScheduler;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

public class WorkerTaskTest {

    @Test
    public void taskFinished() throws InterruptedException {
        //A simple task: executes and finishes normally
        final TestFeature testFeature = new TestFeature();
        final WorkerTaskConfiguration<String, String, String> config = WorkerTaskConfiguration.of("testTask-finished", () -> (input, progress, isCancelled) -> {
            progress.accept("one");
            progress.accept("two");
            return "end";
        });
        final WorkerTask<String, String, String> task = WorkerTask.create(config.addFeature(testFeature));
        task.start("input");
        testFeature.endLatch.await();
        assertThat(testFeature.collectedEvents).containsExactly("STARTED:I=input", "PROGRESS:P=one", "PROGRESS:P=two", "FINISHED:R=end");
    }

    @Test
    public void taskCancelled() throws InterruptedException {
        //An endlessly running task which is cancelled
        final TestFeature testFeature = new TestFeature();
        final WorkerTaskConfiguration<String, String, String> config = WorkerTaskConfiguration.of("testTask-cancelled", () -> (input, progress, isCancelled) -> {
            int cnt = 0;
            while (!isCancelled.get()) {
                progress.accept(String.valueOf(cnt++));
                sleep(20);
            }
            return "end";
        });
        final WorkerTask<String, String, String> task = WorkerTask.create(config.addFeature(testFeature));
        task.start("input");
        sleep(50);
        task.cancel();
        testFeature.endLatch.await();

        //check collected events. Is something like the following, but we can't exactly tell which "progress" messages went through:
        //"STARTED:I=input", "PROGRESS:P=0", "PROGRESS:P=1", "PROGRESS:P=2", ..., "CANCELLED"
        final List<String> events = testFeature.collectedEvents;
        assertThat(events.get(0)).isEqualTo("STARTED:I=input");
        assertThat(events.get(events.size() - 1)).isEqualTo("CANCELLED");
        for (int i = 1 ; i < events.size() - 1; i++) {
            assertThat(events.get(i)).isEqualTo("PROGRESS:P=" + (i - 1));
        }
    }

    @Test
    public void taskReconnect() throws InterruptedException {
        //An endless running task which is started, then disconnected, then reconnected, then cancelled
        final String taskid = "testTask-reconnect";
        assertThat(WorkerTask.taskExists(taskid)).isFalse();
        final TestFeature testFeature = new TestFeature();
        final WorkerTaskConfiguration<String, String, String> config = WorkerTaskConfiguration.of(taskid, () -> (input, progress, isCancelled) -> {
            int cnt = 0;
            while (!isCancelled.get()) {
                progress.accept(String.valueOf(cnt++));
                sleep(20);
            }
            return "end";
        });
        config.addFeature(testFeature);

        final WorkerTask<String, String, String> task = WorkerTask.create(config);
        task.start("input");
        assertThat(WorkerTask.taskIsRunning(taskid)).isTrue();
        sleep(50);
        task.disconnect();
        sleep(50);
        assertThat(WorkerTask.taskIsRunning(taskid)).isTrue();
        final WorkerTask<String, String, String> task2 = WorkerTask.create(config);
        sleep(50);
        task2.cancel();
        testFeature.endLatch.await();
        assertThat(WorkerTask.taskIsRunning(taskid)).isFalse();

        //task2 is still connected
        assertThat(WorkerTask.taskExists(taskid)).isTrue();
        task2.disconnect();
        //no task is still connected
        assertThat(WorkerTask.taskExists(taskid)).isFalse();

        //check collected events. Is something like the following, but we can't exactly tell which "progress" messages went through:
        //"STARTED:I=input", "PROGRESS:P=0", "PROGRESS:P=1", ..., "DISCONNECTED","RECONNECTED","PROGRESS:P=4", ..., "PROGRESS:P=5","CANCELLED"
        final List<String> events = testFeature.collectedEvents;
        assertThat(events.get(0)).isEqualTo("STARTED:I=input");
        assertThat(events.get(events.size() - 1)).isEqualTo("CANCELLED");
        final int idxDisconnected = events.indexOf("DISCONNECTED");
        final int idxReconnected = events.indexOf("RECONNECTED");
        assertThat(idxDisconnected + 1).as("disconnected and reconnected are behind each other").isEqualTo(idxReconnected);
    }


    private static class TestFeature implements Consumer<WorkerTaskConfiguration<?, ?, ?>> {

        public static final Scheduler taskScheduler = new SingleScheduler();
        public static final Scheduler listenerScheduler = new SingleScheduler();


        public final List<String> collectedEvents = new ArrayList<>();
        public final CountDownLatch endLatch = new CountDownLatch(1);

        public final DelegateWorkerTaskControl<String> taskControl = new DelegateWorkerTaskControl<>();


        @Override
        @SuppressWarnings("unchecked")
        public void accept(final WorkerTaskConfiguration<?, ?, ?> config) {
            acceptInternal((WorkerTaskConfiguration<String, String, String>) config);
        }

        private void acceptInternal(final WorkerTaskConfiguration<String, String, String> config) {
            taskControl.setDelegate(config.taskControl);
            config.addTaskListener(event -> {
                collectedEvents.add(event.toString());

                //check whether run ended
                if (event.type == WorkerTaskEventType.CANCELLED || event.type == WorkerTaskEventType.FINISHED) {
                    endLatch.countDown();
                }
            }).setTaskScheduler(taskScheduler).setListenerScheduler(listenerScheduler);
        }
    }

    private static void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            fail("Unexpected interrupt wile sleeping", ie);
        }
    }

}
