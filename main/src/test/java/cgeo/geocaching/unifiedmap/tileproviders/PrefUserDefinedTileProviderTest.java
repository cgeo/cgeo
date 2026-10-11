package cgeo.geocaching.unifiedmap.tileproviders;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the stored form of a user-defined tile provider. The list is kept as json in a
 * preference, so what it serialises to has to stay readable by later versions.
 */
public class PrefUserDefinedTileProviderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String URL = "https://example.com/{Z}/{X}/{Y}.png";

    @Test
    public void survivesAJsonRoundTrip() throws Exception {
        final PrefUserDefinedTileProvider original = new PrefUserDefinedTileProvider("key-1", "My map", URL);
        final PrefUserDefinedTileProvider restored =
                MAPPER.readValue(MAPPER.writeValueAsString(original), PrefUserDefinedTileProvider.class);

        assertThat(restored.getKey()).isEqualTo("key-1");
        assertThat(restored.getName()).isEqualTo("My map");
        assertThat(restored.getUri()).isEqualTo(URL);
    }

    @Test
    public void survivesAJsonRoundTripAsAList() throws Exception {
        final List<PrefUserDefinedTileProvider> original = List.of(
                new PrefUserDefinedTileProvider("key-1", "One", URL),
                new PrefUserDefinedTileProvider("key-2", null, URL));
        final List<PrefUserDefinedTileProvider> restored =
                MAPPER.readValue(MAPPER.writeValueAsString(original), new TypeReference<List<PrefUserDefinedTileProvider>>() { });

        assertThat(restored).hasSize(2);
        assertThat(restored.get(0).getName()).isEqualTo("One");
        assertThat(restored.get(1).getName()).isNull();
    }

    @Test
    public void identityIsTheKeyAlone() {
        // putUserDefinedTileProvider finds the entry to replace via indexOf, so equality must not
        // depend on the name or the uri
        final PrefUserDefinedTileProvider a = new PrefUserDefinedTileProvider("key-1", "One", URL);
        final PrefUserDefinedTileProvider b = new PrefUserDefinedTileProvider("key-1", "Renamed", "https://other.example/{Z}/{X}/{Y}.png");
        final PrefUserDefinedTileProvider c = new PrefUserDefinedTileProvider("key-2", "One", URL);

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    public void onlyTheThreeFieldsAreStored() throws Exception {
        // getDisplayName() and isConfigured() are derived from the fields, not stored alongside
        // them. Without @JsonIgnore they would be written out as well, and that is the kind of
        // thing that is only noticed once somebody's settings no longer read back.
        final List<String> fields = new ArrayList<>();
        MAPPER.readTree(MAPPER.writeValueAsString(new PrefUserDefinedTileProvider("key-1", "My map", URL)))
                .fieldNames().forEachRemaining(fields::add);

        assertThat(fields).containsExactlyInAnyOrder("key", "name", "uri");
    }

    @Test
    public void survivesBeingSharedAsText() {
        final PrefUserDefinedTileProvider shared =
                PrefUserDefinedTileProvider.fromShareableText(new PrefUserDefinedTileProvider("key-1", "My map", URL).toShareableText());

        assertThat(shared).isNotNull();
        assertThat(shared.getName()).isEqualTo("My map");
        assertThat(shared.getUri()).isEqualTo(URL);
        // the receiving installation knows nothing of the sender's key, so it gets one of its own
        assertThat(shared.getKey()).isNotEqualTo("key-1");
    }

    @Test
    public void isNotReadBackFromTextWithoutAUri() {
        assertThat(PrefUserDefinedTileProvider.fromShareableText("just some chat text")).isNull();
        assertThat(PrefUserDefinedTileProvider.fromShareableText(null)).isNull();
    }

    @Test
    public void unknownFieldsWouldNotBreakReading() throws Exception {
        final String json = "{\"key\":\"key-1\",\"name\":\"My map\",\"uri\":\"" + URL + "\"}";
        assertThat(MAPPER.readValue(json, PrefUserDefinedTileProvider.class).getKey()).isEqualTo("key-1");
    }
}
