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

package cgeo.geocaching

import cgeo.geocaching.utils.LazyInitializedList

import java.util.LinkedList
import java.util.List

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class LazyInitializedListTest {

    private static val MAKE_NULL: Int = -1
    private static val MAKE_EXCEPTION: Int = -2

    private static class MyList : LazyInitializedList()<Integer> {

        private Int counter

        MyList(final Int counter) {
            this.counter = counter
        }

        override         public List<Integer> call() {
            if (counter == MAKE_NULL) {
                return null
            }
            if (counter == MAKE_EXCEPTION) {
                throw IllegalStateException("exception in call()")
            }
            val result: List<Integer> = LinkedList<>()
            for (Int i = 0; i < counter; i++) {
                result.add(counter)
            }
            counter += 1
            return result
        }

        Int getCounter() {
            return counter
        }

    }

    @Test
    public Unit testCallOnce() {
        val list: MyList = MyList(0)
        assertThat(list.getCounter()).overridingErrorMessage("call() must not be called prematurely").isEqualTo(0)
        list.size()
        assertThat(list.getCounter()).overridingErrorMessage("call() must be called when needed").isEqualTo(1)
        list.size()
        assertThat(list.getCounter()).overridingErrorMessage("call() must be called only once").isEqualTo(1)
    }

    @Test
    public Unit testSize() {
        val list: MyList = MyList(3)
        assertThat(list).overridingErrorMessage("completed size must be identical to call() result").hasSize(3)
    }

    @Test
    public Unit testValue() {
        val list: MyList = MyList(1)
        assertThat(list.get(0)).overridingErrorMessage("value must be identical to call() result").isEqualTo(1)
    }

    @Test
    public Unit testNull() {
        val list: MyList = MyList(MAKE_NULL)
        assertThat(list).overridingErrorMessage("null returned by call() must create an empty list").isEmpty()
    }

    @Test
    public Unit testException() {
        val list: MyList = MyList(MAKE_EXCEPTION)
        assertThat(list).overridingErrorMessage("exception in call() must create an empty list").isEmpty()
    }

}
