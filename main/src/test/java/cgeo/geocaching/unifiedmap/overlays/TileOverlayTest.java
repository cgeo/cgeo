package cgeo.geocaching.unifiedmap.overlays;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TileOverlayTest {

    private static final String URL = "https://tiles.openseamap.org/seamark/{Z}/{X}/{Y}.png";

    @Test
    public void displayNameUsesTheGivenName() {
        assertThat(new TileOverlay("k", "Sea marks", URL).getDisplayName()).isEqualTo("Sea marks");
    }

    @Test
    public void displayNameFallsBackToHostWhenNameIsBlank() {
        assertThat(new TileOverlay("k", "", URL).getDisplayName()).isEqualTo("tiles.openseamap.org");
        assertThat(new TileOverlay("k", "   ", URL).getDisplayName()).isEqualTo("tiles.openseamap.org");
    }

    @Test
    public void createGeneratesDistinctKeys() {
        assertThat(TileOverlay.create("a", URL).getKey())
                .isNotEqualTo(TileOverlay.create("a", URL).getKey());
    }

    @Test
    public void survivesAJsonRoundTrip() {
        final TileOverlay original = new TileOverlay("key-1", "Sea marks", URL);
        final TileOverlay restored = TileOverlay.fromJson(original.toJson());
        assertThat(restored).isNotNull();
        assertThat(restored.getKey()).isEqualTo("key-1");
        assertThat(restored.getName()).isEqualTo("Sea marks");
        assertThat(restored.getUrl()).isEqualTo(URL);
    }

    @Test
    public void jsonRoundTripKeepsABlankName() {
        final TileOverlay restored = TileOverlay.fromJson(new TileOverlay("key-1", "", URL).toJson());
        assertThat(restored).isNotNull();
        assertThat(restored.getName()).isEmpty();
        assertThat(restored.getDisplayName()).isEqualTo("tiles.openseamap.org");
    }

    @Test
    public void fromJsonRejectsEntriesWithoutAUsableUrl() {
        assertThat(TileOverlay.fromJson(new TileOverlay("key-1", "n", "not a url").toJson())).isNull();
        assertThat(TileOverlay.fromJson(new TileOverlay("", "n", URL).toJson())).isNull();
        assertThat(TileOverlay.fromJson(null)).isNull();
    }

    @Test
    public void withNameAndWithUrlKeepTheKey() {
        final TileOverlay original = new TileOverlay("key-1", "Sea marks", URL);
        assertThat(original.withName("Renamed").getKey()).isEqualTo("key-1");
        assertThat(original.withName("Renamed").getDisplayName()).isEqualTo("Renamed");
        assertThat(original.withUrl("https://other.example/{Z}/{X}/{Y}.png").getUrl())
                .isEqualTo("https://other.example/{Z}/{X}/{Y}.png");
    }
}
