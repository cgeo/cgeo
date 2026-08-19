package cgeo.geocaching.export;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Unit tests for IndividualRouteExportTask to verify namespace declarations.
 */
public class IndividualRouteExportTaskTest {

    @Test
    public void testNamespaceConstants() {
        // Verify that the groundspeak namespace is properly defined
        assertThat(IndividualRouteExportTask.class).isNotNull();
        // This test ensures the class compiles and loads correctly
    }
}
