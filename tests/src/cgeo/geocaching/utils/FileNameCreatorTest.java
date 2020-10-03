package cgeo.geocaching.utils;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class FileNameCreatorTest {

    @Test
    public void simpleNameCreator() {
        final FileNameCreator simpleNameCreator = FileNameCreator.forName("myname", "mymimetype");
        for (int i = 0; i < 5; i++) {
            assertThat(simpleNameCreator.createName()).isEqualTo("myname");
            assertThat(simpleNameCreator.getMimeType()).isEqualTo("mymimetype");
        }

        assertThat(FileNameCreator.forName("myname2").getMimeType()).isNull();
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

    @Test
    public void specialFileNameCreators()  {

        assertThat(FileNameCreator.DEFAULT.getMimeType()).isNull();
        assertThat(FileNameCreator.DEFAULT_BINARY.getMimeType()).isEqualTo(FileNameCreator.MIME_TYPE_BINARY);
        assertThat(FileNameCreator.DEFAULT_TEXT.getMimeType()).isEqualTo(FileNameCreator.MIME_TYPE_TEXT);

    }

}
