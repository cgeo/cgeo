package cgeo.geocaching.utils;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class FileNameCreatorTest {

    @Test
    public void simpleNameCreator() {
        final FileNameCreator simpleNameCreator = FileNameCreator.forName("myname");
        for (int i = 0; i < 5; i++) {
            assertThat(simpleNameCreator.createName()).isEqualTo("myname");
        }

        assertThat(FileNameCreator.forName("myname2").createName()).isEqualTo("myname2");
    }

    @Test
    public void nameUniqueness() {

        final Set<String> names = new HashSet<>();

        //make sure that name creation is indeed unique
        for (int i = 0; i < 2000; i++) {
            names.add(FileNameCreator.DEFAULT.createName());
        }

        assertThat(names.size()).isEqualTo(2000);

    }

}
