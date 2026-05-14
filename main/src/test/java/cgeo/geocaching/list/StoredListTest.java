package cgeo.geocaching.list;

import static cgeo.geocaching.list.StoredList.UserInterface.handleListNameInputHelper;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class StoredListTest {

    @Test
    public void handleListNaming() {
        // some basic list creation tests
        assertThat(handleListNameInputHelper("",  "A")).isEqualTo("A");
        assertThat(handleListNameInputHelper("A", "B")).isEqualTo("A:B");
        assertThat(handleListNameInputHelper("A:B", "C")).isEqualTo("A:B:C");
        assertThat(handleListNameInputHelper("A:B:C", "D")).isEqualTo("A:B:C:D");
        // fix some formatting errors
        assertThat(handleListNameInputHelper("A::B", "C")).isEqualTo("A:B:C");
        assertThat(handleListNameInputHelper("A:", "C")).isEqualTo("A:C");
        assertThat(handleListNameInputHelper("A::", "C")).isEqualTo("A:C");
        assertThat(handleListNameInputHelper("A::::", "C")).isEqualTo("A:C");
        assertThat(handleListNameInputHelper("A: ", "C")).isEqualTo("A:C");
        assertThat(handleListNameInputHelper("A: B", "C")).isEqualTo("A:B:C");
        assertThat(handleListNameInputHelper("A: B: ", "C")).isEqualTo("A:B:C");
        // fix some user input errors
        assertThat(handleListNameInputHelper("A", "::C")).isEqualTo("A:C");
        assertThat(handleListNameInputHelper("A", "::B:C")).isEqualTo("A:B:C");
        assertThat(handleListNameInputHelper("A", "B::C")).isEqualTo("A:B:C");
        assertThat(handleListNameInputHelper("A", "B: :C")).isEqualTo("A:B:C");
        assertThat(handleListNameInputHelper("", "::C")).isEqualTo("C");
        assertThat(handleListNameInputHelper("A", "")).isEqualTo("A");
        assertThat(handleListNameInputHelper("A", " ")).isEqualTo("A");
        assertThat(handleListNameInputHelper("A", ":")).isEqualTo("A");
        assertThat(handleListNameInputHelper("A", "::")).isEqualTo("A");
    }

}
