package cgeo.geocaching.utils;

import android.test.AndroidTestCase;

public class MemorySubjectTest extends AndroidTestCase {

    private static class Observer implements IObserver<Integer> {
        public int times = 0;
        public Integer value;

        @Override
        public void update(final Integer data) {
            value = data;
            times++;
        }
    }

    private Observer observer;
    private MemorySubject<Integer> memorySubject;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        observer = new Observer();
        memorySubject = new MemorySubject<Integer>();
    }

    public void testInitial() {
        assertNull(observer.value);
        assertEquals(0, observer.times);
        assertNull(memorySubject.getMemory());
    }

    public void testMemory() {
        memorySubject.addObserver(observer);
        memorySubject.notifyObservers(10);
        assertEquals(Integer.valueOf(10), observer.value);
        assertEquals(1, observer.times);
        assertEquals(Integer.valueOf(10), memorySubject.getMemory());
        memorySubject.notifyObservers(20);
        assertEquals(Integer.valueOf(20), observer.value);
        assertEquals(2, observer.times);
        assertEquals(Integer.valueOf(20), memorySubject.getMemory());
    }

    public void testAttach() {
        memorySubject.notifyObservers(10);
        assertNull(observer.value);
        assertEquals(0, observer.times);
        memorySubject.addObserver(observer);
        assertEquals(Integer.valueOf(10), observer.value);
        assertEquals(1, observer.times);
        memorySubject.notifyObservers(20);
        assertEquals(Integer.valueOf(20), observer.value);
        assertEquals(2, observer.times);
    }

    public void testDetach() {
        memorySubject.addObserver(observer);
        memorySubject.notifyObservers(10);
        assertEquals(Integer.valueOf(10), observer.value);
        assertEquals(1, observer.times);
        assertEquals(Integer.valueOf(10), memorySubject.getMemory());
        memorySubject.deleteObserver(observer);
        memorySubject.notifyObservers(20);
        assertEquals(Integer.valueOf(10), observer.value);
        assertEquals(1, observer.times);
        assertEquals(Integer.valueOf(20), memorySubject.getMemory());
    }

    public void testMultiple() {
        final Observer otherObserver = new Observer();
        memorySubject.addObserver(otherObserver);
        testDetach();
        assertEquals(Integer.valueOf(20), otherObserver.value);
        assertEquals(2, otherObserver.times);
    }

}
