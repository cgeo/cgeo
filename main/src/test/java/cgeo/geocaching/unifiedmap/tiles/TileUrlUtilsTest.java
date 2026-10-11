package cgeo.geocaching.unifiedmap.tiles;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TileUrlUtilsTest {

    @Test
    public void normalizesUppercasePlaceholders() {
        assertThat(TileUrlUtils.normalize("https://example.com/{Z}/{X}/{Y}.png"))
                .isEqualTo("https://example.com/{Z}/{X}/{Y}.png");
    }

    @Test
    public void normalizesLowercasePlaceholders() {
        assertThat(TileUrlUtils.normalize("https://example.com/{z}/{x}/{y}.png"))
                .isEqualTo("https://example.com/{Z}/{X}/{Y}.png");
    }

    @Test
    public void normalizesMixedCasePlaceholders() {
        assertThat(TileUrlUtils.normalize("https://example.com/{z}/{X}/{y}.png"))
                .isEqualTo("https://example.com/{Z}/{X}/{Y}.png");
    }

    @Test
    public void trimsSurroundingWhitespace() {
        assertThat(TileUrlUtils.normalize("  https://example.com/{z}/{x}/{y}.png  "))
                .isEqualTo("https://example.com/{Z}/{X}/{Y}.png");
    }

    @Test
    public void keepsQueryParameters() {
        assertThat(TileUrlUtils.normalize("https://example.com/tiles?z={z}&x={x}&y={y}&key=abc123"))
                .isEqualTo("https://example.com/tiles?z={Z}&x={X}&y={Y}&key=abc123");
    }

    @Test
    public void acceptsPlainHttp() {
        assertThat(TileUrlUtils.normalize("http://example.com/{z}/{x}/{y}.png"))
                .isEqualTo("http://example.com/{Z}/{X}/{Y}.png");
    }

    @Test
    public void acceptsPartialOrMissingPlaceholders() {
        // the default tile path gets appended to these, as the tile providers have always done
        assertThat(TileUrlUtils.normalize("https://example.com/tiles.png")).isEqualTo("https://example.com/tiles.png");
        assertThat(TileUrlUtils.normalize("https://example.com/{z}/{x}.png")).isEqualTo("https://example.com/{Z}/{X}.png");
    }

    @Test
    public void rejectsNonHttpSchemes() {
        assertThat(TileUrlUtils.normalize("ftp://example.com/{z}/{x}/{y}.png")).isNull();
        assertThat(TileUrlUtils.normalize("file:///tiles/{z}/{x}/{y}.png")).isNull();
    }

    @Test
    public void rejectsBlankInput() {
        assertThat(TileUrlUtils.normalize(null)).isNull();
        assertThat(TileUrlUtils.normalize("")).isNull();
        assertThat(TileUrlUtils.normalize("   ")).isNull();
    }

    @Test
    public void isValidMatchesNormalize() {
        assertThat(TileUrlUtils.isValid("https://example.com/{z}/{x}/{y}.png")).isTrue();
        assertThat(TileUrlUtils.isValid("ftp://example.com/{z}/{x}/{y}.png")).isFalse();
    }

    @Test
    public void splitsIntoBaseAndTilePath() {
        assertThat(TileUrlUtils.split("https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png"))
                .containsExactly("https://tiles.openseamap.org", "/seamark/{Z}/{X}/{Y}.png");
    }

    @Test
    public void splitsKeepingPortAndQuery() {
        assertThat(TileUrlUtils.split("https://example.com:8443/t?z={z}&x={x}&y={y}"))
                .containsExactly("https://example.com:8443", "/t?z={Z}&x={X}&y={Y}");
    }

    @Test
    public void splitReturnsNullForUnusableTemplate() {
        assertThat(TileUrlUtils.split("nonsense")).isNull();
    }

    @Test
    public void extractsHost() {
        assertThat(TileUrlUtils.host("https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png"))
                .isEqualTo("tiles.openseamap.org");
        assertThat(TileUrlUtils.host("https://example.com:8443/t?z={z}&x={x}&y={y}"))
                .isEqualTo("example.com");
    }

    @Test
    public void extractsHostWithoutPlaceholders() {
        // tile providers may be configured with a bare url, so the host is still wanted there
        assertThat(TileUrlUtils.host("https://example.com/tiles")).isEqualTo("example.com");
    }

    @Test
    public void hostIsNullForUnusableTemplate() {
        assertThat(TileUrlUtils.host("ftp://example.com/{z}/{x}/{y}.png")).isNull();
        assertThat(TileUrlUtils.host("not a url at all")).isNull();
        assertThat(TileUrlUtils.host(null)).isNull();
    }

    @Test
    public void allowsAUrlWithoutPlaceholders() {
        assertThat(TileUrlUtils.normalize("https://example.com/tiles"))
                .isEqualTo("https://example.com/tiles");
        assertThat(TileUrlUtils.normalize("https://example.com/{z}/{x}/{y}.png"))
                .isEqualTo("https://example.com/{Z}/{X}/{Y}.png");
        assertThat(TileUrlUtils.normalize("ftp://example.com/tiles")).isNull();
        assertThat(TileUrlUtils.normalize("nonsense")).isNull();
    }

    @Test
    public void appendsTheDefaultTilePathOnlyWhenNeeded() {
        assertThat(TileUrlUtils.withDefaultTilePath("https://example.com/tiles"))
                .isEqualTo("https://example.com/tiles/{Z}/{X}/{Y}.png");
        assertThat(TileUrlUtils.withDefaultTilePath("https://example.com/tiles/"))
                .isEqualTo("https://example.com/tiles/{Z}/{X}/{Y}.png");
        assertThat(TileUrlUtils.withDefaultTilePath("https://example.com/{Z}/{X}/{Y}.png"))
                .isEqualTo("https://example.com/{Z}/{X}/{Y}.png");
    }

    @Test
    public void resolvesPlaceholdersForTile() {
        assertThat(TileUrlUtils.forTile("https://example.com/{Z}/{X}/{Y}.png", 14, 8802, 4780))
                .isEqualTo("https://example.com/14/8802/4780.png");
    }
}
