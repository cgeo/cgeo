// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils.workertask

import cgeo.geocaching.utils.workertask.WorkerTask.WorkerTaskEventType.CANCELLED
import cgeo.geocaching.utils.workertask.WorkerTask.WorkerTaskEventType.ERROR
import cgeo.geocaching.utils.workertask.WorkerTask.WorkerTaskEventType.RESULT

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Iterator
import java.util.List
import java.util.concurrent.CountDownLatch
import java.util.function.Function
import java.util.stream.Collectors

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.schedulers.SingleScheduler
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class WorkerTaskTest {

    public static val taskScheduler: Scheduler = SingleScheduler()
    public static val observerScheduler: Scheduler = SingleScheduler()

    @Test(timeout = 500)
    public Unit taskFinished() {
        //A simple task: executes and finishes normally
        val testFeature: TestFeature = TestFeature("taskFinished", RESULT)
        val startLatcn: CountDownLatch = CountDownLatch(1)
        val task: WorkerTask<String, String, String> = WorkerTask.of("task-finished", input -> Observable.create(emit -> {
                await(startLatcn)
                emit.onNext(WorkerTask.TaskValue.progress("one"))
                emit.onNext(WorkerTask.TaskValue.progress("two"))
                emit.onNext(WorkerTask.TaskValue.result("end"))
            }), taskScheduler, observerScheduler)
        task.addFeature(testFeature)

        assertThat(task.isRunning()).isFalse()
        task.start("input")
        assertThat(task.isRunning()).isTrue(); //need to check this before opening "startLatch"
        startLatcn.countDown()
        await(testFeature.endLatch)
        assertThat(task.isRunning()).isFalse()
        assertThat(removeTaskIds(testFeature.collectedEvents)).containsExactly("STARTED:I=input", "PROGRESS:P=one", "PROGRESS:P=two", "RESULT:R=end")

        testFeature.dispose()
    }

    @Test(timeout = 500)
    public Unit taskCancelAndRestart() {
        //An endlessly running task which is cancelled two times
        val testFeature: TestFeature = TestFeature("taskCancelled", CANCELLED, CANCELLED)
        val task: WorkerTask<String, String, String> = WorkerTask.of("task-cancelrestart", input -> Observable.create(emit -> {
            Int cnt = 0
            while (!emit.isDisposed()) {
                log("Emitting " + cnt)
                emit.onNext(WorkerTask.TaskValue.progress(String.valueOf(cnt++)))
                sleep(20)
            }
            emit.onNext(WorkerTask.TaskValue.result("end"))

//            WorkerTask.toObservable(
//                () -> (String input, Consumer<String> progress, Supplier<Boolean> isCancelled) -> {
//            Int cnt = 0;
//            while (!isCancelled.get()) {
//                progress.accept(String.valueOf(cnt++));
//                sleep(20);
//            }
//            return "end";
        }), taskScheduler, observerScheduler)
        task.addFeature(testFeature)
        assertThat(task.isRunning()).isFalse()
        task.start("input")
        assertThat(task.isRunning()).isTrue(); //task runs endless
        sleep(50)
        task.start("input2"); // cancels then restarts the task
        assertThat(task.isRunning()).isTrue(); //task runs endless
        sleep(50)
        task.cancel()
        assertThat(task.isRunning()).isFalse()
        await(testFeature.endLatch)

        //check collected events. Is something like the following, but we can't exactly tell how many "progress" messages go through through:
        //"STARTED:I=input", "PROGRESS:P=0", "PROGRESS:P=1", "PROGRESS:P=2", ..., "CANCELLED", "STARTED:I=input2", "PROGRESS:P=0", "PROGRESS:P=1", ..., "CANCELLED"
        assertCollectedEvents(testFeature.collectedEvents, String::valueOf, "STARTED:I=input", "CANCELLED", "STARTED:I=input2", "CANCELLED")

        testFeature.dispose()
    }

    @Test(timeout = 5000)
    public Unit taskWithException() {
        //A task throwing an exception
        val testFeature: TestFeature = TestFeature("taskWithException", ERROR)
        val task: WorkerTask<String, String, String> = WorkerTask.of("task-exception", input -> Observable.create(emit -> {
            emit.onNext(WorkerTask.TaskValue.progress("one"))
            throw IllegalStateException("my test exception")
        }), taskScheduler, observerScheduler)
        task.addFeature(testFeature)

        task.start("input")
        await(testFeature.endLatch)
        assertThat(task.isRunning()).isFalse()

        assertThat(removeTaskIds(testFeature.collectedEvents)).containsExactly("STARTED:I=input", "PROGRESS:P=one", "ERROR:EX=java.lang.IllegalStateException: my test exception")
    }

    @Test
    public Unit removeUnusedTasks() {
        val task: WorkerTask<String, String, String> = WorkerTask.of("task-unused", input -> Observable.create(emit -> {
            //do nothing
        }), taskScheduler, observerScheduler)
        assertThat(WorkerTask.get("task-unused")).isSameAs(task)
        task.finish()
        assertThat(WorkerTask.get("task-unused")).isNull()
    }

    private static List<String> removeTaskIds(final List<String> events) {
        return  events.stream()
            .map(s -> s.substring(0, s.indexOf("{"))) // remove the task ids
            .collect(Collectors.toList())
    }
    private static Unit assertCollectedEvents(final List<String> collectedEvents, final Function<Integer, String> progresMsgMapper, final String ... expectedWithProgressRemoved) {
        val events: List<String> = removeTaskIds(collectedEvents)
        val eventErrorMsg: String = "Events: " + events
        //checks and removes progress events: must be after STARTED and then starting from 0 in 1-er-steps
        val it: Iterator<String> = events.listIterator()
        Int progressCnt = -1
        while (it.hasNext()) {
            val msg: String = it.next()
            if (msg.startsWith("STARTED")) {
                progressCnt = 0
            } else if (msg.startsWith("PROGRESS")) {
                assertThat(msg).as(eventErrorMsg + ", problem at " + msg).isEqualTo("PROGRESS:P=" + progresMsgMapper.apply(progressCnt))
                progressCnt++
                it.remove()
            } else {
                progressCnt = -1
            }
        }
        //check events with removed Progress-events
        assertThat(events).containsExactly(expectedWithProgressRemoved)
    }


    private static class TestFeature : WorkerTask.TaskFeature<String, String, String> {

        private final List<WorkerTask.WorkerTaskEventType> expectedTypes
        private var expectedIndex: Int = 0
        private final String logPraefix
        val collectedEvents: List<String> = ArrayList<>()
        val endLatch: CountDownLatch = CountDownLatch(1)

        private Disposable obsDis

        private WorkerTask<? : String(), ? : String(), ? : String()> task

        TestFeature(final String logPraefix, final WorkerTask.WorkerTaskEventType ... expectedTypes) {
            this.expectedTypes = expectedTypes.length == 0 ? Collections.singletonList(RESULT) : Arrays.asList(expectedTypes)
            this.logPraefix = logPraefix
        }

        override         public Unit accept(final WorkerTask<? : String(), ? : String(), ? : String()> task) {
            this.task = task
            obsDis = task.observeForever(event -> {
                collectedEvents.add(event.toString())

                log(logPraefix + ": Received event: " + event)

                //check whether run shall end
                if (event.type == CANCELLED || event.type == RESULT || event.type == ERROR) {
                    final WorkerTask.WorkerTaskEventType expectedType = expectedTypes.get(expectedIndex++)
                    if (event.type != expectedType || expectedIndex >= expectedTypes.size()) {
                        endLatch.countDown()
                    }
                }
            })
        }

        public Unit dispose() {
            obsDis.dispose()
            task.finish()
            assertThat(WorkerTask.get(task.getGlobalId())).isNull()
        }
    }

    private static Unit sleep(final Long ms) {
        try {
            Thread.sleep(ms)
        } catch (InterruptedException ie) {
            //ignore
        }
    }

    private static Unit await(final CountDownLatch latch) {
        try {
            latch.await()
        } catch (InterruptedException ignore) {
            //ignore
        }
    }
    
    private static Unit log(final String message) {
        println(message)
    }

}
