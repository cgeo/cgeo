package cgeo.geocaching;

import cgeo.geocaching.utils.LazyInitializedList;

import android.test.AndroidTestCase;

import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class LazyInitializedListTest extends AndroidTestCase {

    private static final int MAKE_NULL = -1;
    private static final int MAKE_EXCEPTION = -2;

    private static class MyList extends LazyInitializedList<Integer> {

        private int counter;

        MyList(final int counter) {
            this.counter = counter;
        }

        @Override
        public List<Integer> call() {
            if (counter == MAKE_NULL) {
                return null;
            }
            if (counter == MAKE_EXCEPTION) {
                throw new IllegalStateException("exception in call()");
            }
            final List<Integer> result = new LinkedList<>();
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
        final MyList list = new MyList(0);
        assertThat(list.getCounter()).overridingErrorMessage("call() must not be called prematurely").isEqualTo(0);
        list.size();
        assertThat(list.getCounter()).overridingErrorMessage("call() must be called when needed").isEqualTo(1);
        list.size();
        assertThat(list.getCounter()).overridingErrorMessage("call() must be called only once").isEqualTo(1);
    }

    public static void testSize() {
        final MyList list = new MyList(3);
        assertThat(list).overridingErrorMessage("completed size must be identical to call() result").hasSize(3);
    }

    public static void testValue() {
        final MyList list = new MyList(1);
        assertThat(list.get(0)).overridingErrorMessage("value must be identical to call() result").isEqualTo(1);
    }

    public static void testNull() {
        final MyList list = new MyList(MAKE_NULL);
        assertThat(list).overridingErrorMessage("null returned by call() must create an empty list").isEmpty();
    }

    public static void testException() {
        final MyList list = new MyList(MAKE_EXCEPTION);
        assertThat(list).overridingErrorMessage("exception in call() must create an empty list").isEmpty();
    }

}
