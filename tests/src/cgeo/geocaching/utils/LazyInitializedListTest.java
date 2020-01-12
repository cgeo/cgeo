package cgeo.geocaching.utils;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class LazyInitializedListTest {

    private static final class MockedLazyInitializedList extends LazyInitializedList<String> {
        @Override
        public List<String> call() {
            return new ArrayList<>();
        }
    }

    @Test
    public void testAccess() {
        final LazyInitializedList<String> list = new MockedLazyInitializedList();
        assertThat(list).isEmpty();
        list.add("Test");
        assertThat(list).isNotEmpty();
        assertThat(list).hasSize(1);
        int iterations = 0;
        for (final String element : list) {
            assertThat(element).isEqualTo("Test");
            iterations++;
        }
        assertThat(iterations).isEqualTo(1);
    }

}
