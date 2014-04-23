package cgeo.geocaching.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

public class MiscUtilsTest extends TestCase {

    public static void testBufferEmpty() {
        for (final List<String> s : MiscUtils.buffer(new LinkedList<String>(), 10)) {
            assertThat(s).isNotNull(); // only to silence findbugs and the compiler
            fail("empty collection should not iterate");
        }
    }

    public static void testMultiple() {
        final List<Integer> list = new LinkedList<Integer>();
        for (int i = 0; i < 50; i++) {
            list.add(i);
        }
        int count = 0;
        for (final List<Integer> subList: MiscUtils.buffer(list, 10)) {
            assertThat(subList).hasSize(10);
            assertThat(subList.get(0)).as("sublist content").isEqualTo(count * 10);
            count++;
        }
        assertThat(count).isEqualTo(5);
    }

    public static void testNonMultiple() {
        final List<Integer> list = new LinkedList<Integer>();
        for (int i = 0; i < 48; i++) {
            list.add(i);
        }
        int count = 0;
        for (final List<Integer> subList: MiscUtils.buffer(list, 10)) {
            assertThat(subList.size()).overridingErrorMessage("each sublist has no more than the allowed number of arguments").isLessThanOrEqualTo(10);
            count += subList.size();
        }
        assertEquals("all the elements were seen", 48, count);
    }

    public static void testArguments() {
        try {
            MiscUtils.buffer(new LinkedList<Integer>(), 0);
            fail("an exception should be raised");
        } catch (final IllegalArgumentException e) {
            // Ok
        } catch (final Exception e) {
            fail("bad exception raised: " + e);
        }
    }

}
