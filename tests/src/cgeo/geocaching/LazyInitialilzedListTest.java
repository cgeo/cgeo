package cgeo.geocaching;

import cgeo.geocaching.utils.LazyInitializedList;

import android.test.AndroidTestCase;

import java.util.LinkedList;
import java.util.List;

public class LazyInitialilzedListTest extends AndroidTestCase {

    private static final int MAKE_NULL = -1;
    private static final int MAKE_EXCEPTION = -2;

    private static class MyList extends LazyInitializedList<Integer> {

        private int counter;

        MyList(int counter) {
            this.counter = counter;
        }

        @Override
        public List<Integer> call() {
            if (counter == MAKE_NULL) {
                return null;
            }
            if (counter == MAKE_EXCEPTION) {
                throw new RuntimeException("exception in call()");
            }
            final List<Integer> result = new LinkedList<Integer>();
            for (int i = 0; i < counter; i++) {
                result.add(counter);
            }
            counter += 1;
            return result;
        }

        int getCounter() {
            return counter;
        }

    }

    public static void testCallOnce() {
        final MyList l = new MyList(0);
        assertEquals("call() must not called prematurely", 0, l.getCounter());
        l.size();
        assertEquals("call() must be called when needed", 1, l.getCounter());
        l.size();
        assertEquals("call() must be called only once", 1, l.getCounter());
    }

    public static void testSize() {
        final MyList l = new MyList(3);
        assertEquals("completed size must be identical to call() result", 3, l.size());
    }

    public static void testValue() {
        final MyList l = new MyList(1);
        assertEquals("value must be identical to call() result", Integer.valueOf(1), l.get(0));
    }

    public static void testNull() {
        final MyList l = new MyList(MAKE_NULL);
        assertEquals("null returned by call() must create an empty list", 0, l.size());
    }

    public static void testException() {
        final MyList l = new MyList(MAKE_EXCEPTION);
        assertEquals("exception in call() must create an empty list", 0, l.size());
    }

}
