package cgeo.geocaching.unifiedmap.tileproviders;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class CustomMapUrlTest {

    @Test
    public void normalizeAndFormatTemplate() {
        final String template = "https://tiles.example.org/map/{z}/{X}/{y}?apiKey=secret";

        assertThat(CustomMapUrl.normalizeTemplate(template))
                .isEqualTo("https://tiles.example.org/map/{Z}/{X}/{Y}?apiKey=secret");
        assertThat(CustomMapUrl.formatTileUrl(template, 12, 2201, 1380))
                .isEqualTo("https://tiles.example.org/map/12/2201/1380?apiKey=secret");
    }

    @Test
    public void splitTemplate() {
        final String template = "https://tiles.example.org:8443/map/{z}/{x}/{y}.png?token=secret";

        assertThat(CustomMapUrl.getBaseUrl(template)).isEqualTo("https://tiles.example.org:8443");
        assertThat(CustomMapUrl.getTilePath(template)).isEqualTo("/map/{Z}/{X}/{Y}.png?token=secret");
    }

    @Test
    public void migrateLegacyTemplate() {
        assertThat(CustomMapUrl.migrateLegacyTemplate("https://tiles.example.org/maps?token=secret"))
                .isEqualTo("https://tiles.example.org/maps/{Z}/{X}/{Y}.png?token=secret");
        assertThat(CustomMapUrl.migrateLegacyTemplate("https://tiles.example.org/{z}/{x}/{y}.png"))
                .isEqualTo("https://tiles.example.org/{Z}/{X}/{Y}.png");
    }

    @Test
    public void validateTemplate() {
        assertThat(CustomMapUrl.isValidTemplate("https://tiles.example.org/{z}/{x}/{y}"))
                .isTrue();
        assertThat(CustomMapUrl.isValidTemplate("http://192.168.1.10:8080/{Z}/{X}/{Y}.png"))
                .isTrue();
        assertThat(CustomMapUrl.isValidTemplate("https://tiles.example.org/{z}/{x}"))
                .isFalse();
        assertThat(CustomMapUrl.isValidTemplate("ftp://tiles.example.org/{z}/{x}/{y}"))
                .isFalse();
        assertThat(CustomMapUrl.isValidTemplate("https:///tiles/{z}/{x}/{y}"))
                .isFalse();
    }
}
