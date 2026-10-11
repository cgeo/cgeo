package cgeo.geocaching.unifiedmap.tiles;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TileUrlShareTest {

    private static final String URL = "https://tiles.openseamap.org/seamark/{Z}/{X}/{Y}.png";

    @Test
    public void formatsNameAndUrl() {
        assertThat(TileUrlShare.format("Sea marks", URL)).isEqualTo("Sea marks;" + URL);
    }

    @Test
    public void formatsUrlOnlyWhenThereIsNoName() {
        assertThat(TileUrlShare.format("", URL)).isEqualTo(URL);
        assertThat(TileUrlShare.format("   ", URL)).isEqualTo(URL);
    }

    @Test
    public void parsesNameAndUrl() {
        assertThat(TileUrlShare.parse("Sea marks;" + URL)).containsExactly("Sea marks", URL);
    }

    @Test
    public void parsesABareUrl() {
        assertThat(TileUrlShare.parse(URL)).containsExactly("", URL);
    }

    @Test
    public void normalisesThePlaceholdersWhileParsing() {
        assertThat(TileUrlShare.parse("Sea marks;https://example.com/{z}/{x}/{y}.png"))
                .containsExactly("Sea marks", "https://example.com/{Z}/{X}/{Y}.png");
    }

    @Test
    public void trimsSurroundingWhitespace() {
        assertThat(TileUrlShare.parse("  Sea marks ; " + URL + "  ")).containsExactly("Sea marks", URL);
    }

    @Test
    public void keepsASemicolonThatBelongsToTheUrl() {
        final String withSemicolon = "https://example.com/{Z}/{X}/{Y}.png?a=1;b=2";
        assertThat(TileUrlShare.parse(withSemicolon)).containsExactly("", withSemicolon);
    }

    @Test
    public void splitsOnlyOnTheFirstSeparator() {
        final String withSemicolon = "https://example.com/{Z}/{X}/{Y}.png?a=1;b=2";
        assertThat(TileUrlShare.parse("My map;" + withSemicolon)).containsExactly("My map", withSemicolon);
    }

    @Test
    public void keepsASemicolonThatBelongsToTheName() {
        assertThat(TileUrlShare.parse("My;name;" + URL)).containsExactly("My;name", URL);
    }

    @Test
    public void roundTripsANameContainingASemicolon() {
        assertThat(TileUrlShare.parse(TileUrlShare.format("My;name", URL)))
                .containsExactly("My;name", URL);
    }

    @Test
    public void roundTripsThroughFormatAndParse() {
        final String shared = TileUrlShare.format("My map", "https://example.com/{z}/{x}/{y}.png?a=1;b=2");
        assertThat(TileUrlShare.parse(shared))
                .containsExactly("My map", "https://example.com/{Z}/{X}/{Y}.png?a=1;b=2");
    }

    @Test
    public void rejectsTextWithoutAUsableUrl() {
        assertThat(TileUrlShare.parse("just some chat text")).isNull();
        assertThat(TileUrlShare.parse("Name;not a url")).isNull();
        assertThat(TileUrlShare.parse("")).isNull();
        assertThat(TileUrlShare.parse(null)).isNull();
    }

    @Test
    public void acceptsAUrlWithoutPlaceholders() {
        // the default tile path is appended when such a url is used
        assertThat(TileUrlShare.parse("https://example.com/tiles"))
                .containsExactly("", "https://example.com/tiles");
    }

}
