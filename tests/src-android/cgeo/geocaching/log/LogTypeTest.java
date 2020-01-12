package cgeo.geocaching.log;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class LogTypeTest extends TestCase {

    public static void testGetById() {
        assertThat(LogType.getById(0)).isEqualTo(LogType.UNKNOWN);
        assertThat(LogType.getById(4711)).isEqualTo(LogType.UNKNOWN);
        assertThat(LogType.getById(23)).isEqualTo(LogType.ENABLE_LISTING);
    }

    public static void testGetByIconName() {
        assertThat(LogType.getByIconName("")).isEqualTo(LogType.UNKNOWN);
        assertThat(LogType.getByIconName(null)).isEqualTo(LogType.UNKNOWN);
        assertThat(LogType.getByIconName("11")).isEqualTo(LogType.WEBCAM_PHOTO_TAKEN);
    }

    public static void testGetByType() {
        assertThat(LogType.getByType("obviously unknown type")).isEqualTo(LogType.UNKNOWN);
        assertThat(LogType.getByType("grabbed it")).isEqualTo(LogType.GRABBED_IT);
        assertThat(LogType.getByType("  gRAbbed IT ")).isEqualTo(LogType.GRABBED_IT);
    }

}
