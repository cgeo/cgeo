package cgeo.geocaching.log;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class LogTypeTest {

    @Test
    public void testGetById() {
        assertThat(LogType.getById(0)).isEqualTo(LogType.UNKNOWN);
        assertThat(LogType.getById(4711)).isEqualTo(LogType.UNKNOWN);
        assertThat(LogType.getById(23)).isEqualTo(LogType.ENABLE_LISTING);
    }

    @Test
    public void testGetByIconName() {
        assertThat(LogType.getByIconName("")).isEqualTo(LogType.UNKNOWN);
        assertThat(LogType.getByIconName(null)).isEqualTo(LogType.UNKNOWN);
        assertThat(LogType.getByIconName("11")).isEqualTo(LogType.WEBCAM_PHOTO_TAKEN);
    }

    @Test
    public void testGetByType() {
        assertThat(LogType.getByType("obviously unknown type")).isEqualTo(LogType.UNKNOWN);
        assertThat(LogType.getByType("grabbed it")).isEqualTo(LogType.GRABBED_IT);
        assertThat(LogType.getByType("  gRAbbed IT ")).isEqualTo(LogType.GRABBED_IT);
    }

}
