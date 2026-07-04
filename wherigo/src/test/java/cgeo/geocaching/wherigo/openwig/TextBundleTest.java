package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link TextBundle}'s ResourceBundle-style locale fallback, which is pure logic once
 * a Media's raw bytes are available - the only Engine dependency (fetching those bytes from the
 * cartridge) is swapped out here via a test subclass overriding readMediaBytes().
 */
public class TextBundleTest {

    private static final Locale ORIGINAL_LOCALE = Locale.getDefault();

    @After
    public void restoreLocaleAndMediaCounter() {
        Locale.setDefault(ORIGINAL_LOCALE);
        Media.reset();
    }

    private static final class TestableTextBundle extends TextBundle {
        private final Map<Integer, byte[]> content = new HashMap<>();

        void putContent(final Media media, final String propertiesText) {
            content.put(media.id, propertiesText.getBytes(StandardCharsets.UTF_8));
        }

        protected byte[] readMediaBytes(final Media media) {
            return content.get(media.id);
        }
    }

    private static TestableTextBundle bundleOf(final Map<String, String> tagToProperties) {
        final TestableTextBundle bundle = new TestableTextBundle();
        final LuaTable resources = new LuaTableImpl();
        for (final Map.Entry<String, String> entry : tagToProperties.entrySet()) {
            final Media media = new Media();
            bundle.putContent(media, entry.getValue());
            resources.rawset(entry.getKey(), media);
        }
        bundle.rawset("Resources", resources);
        return bundle;
    }

    @Test
    public void getTextReturnsDefaultWhenNoLocaleSpecificFileExists() {
        Locale.setDefault(new Locale("fr", "FR"));
        final TestableTextBundle bundle = bundleOf(mapOf("", "greeting=Hello"));

        assertThat(bundle.getText("greeting")).isEqualTo("Hello");
    }

    @Test
    public void getTextPrefersLanguageSpecificOverDefault() {
        Locale.setDefault(new Locale("de", "DE"));
        final TestableTextBundle bundle = bundleOf(mapOf(
            "", "greeting=Hello",
            "de", "greeting=Hallo"
        ));

        assertThat(bundle.getText("greeting")).isEqualTo("Hallo");
    }

    @Test
    public void getTextPrefersLanguageCountryOverLanguageOverDefault() {
        Locale.setDefault(new Locale("de", "AT"));
        final TestableTextBundle bundle = bundleOf(mapOf(
            "", "greeting=Hello",
            "de", "greeting=Hallo (DE)",
            "de_AT", "greeting=Servus"
        ));

        assertThat(bundle.getText("greeting")).isEqualTo("Servus");
    }

    @Test
    public void getTextFallsBackPerKeyNotPerFile() {
        // "de_AT" exists but doesn't define "farewell" - must fall through to "de", then default,
        // exactly like a real ResourceBundle's parent-delegation, not stop at the first file found
        Locale.setDefault(new Locale("de", "AT"));
        final TestableTextBundle bundle = bundleOf(mapOf(
            "", "greeting=Hello\nfarewell=Goodbye",
            "de", "greeting=Hallo\nfarewell=Auf Wiedersehen",
            "de_AT", "greeting=Servus"
        ));

        assertThat(bundle.getText("greeting")).isEqualTo("Servus");
        assertThat(bundle.getText("farewell")).isEqualTo("Auf Wiedersehen");
    }

    @Test
    public void getTextReturnsNullWhenKeyMissingEverywhere() {
        Locale.setDefault(new Locale("de", "AT"));
        final TestableTextBundle bundle = bundleOf(mapOf("", "greeting=Hello"));

        assertThat(bundle.getText("nonexistent")).isNull();
    }

    @Test
    public void getTextReturnsNullWhenResourcesNeverSet() {
        assertThat(new TestableTextBundle().getText("greeting")).isNull();
    }

    @Test
    public void getTextReturnsNullForNullKey() {
        final TestableTextBundle bundle = bundleOf(mapOf("", "greeting=Hello"));

        assertThat(bundle.getText(null)).isNull();
    }

    @Test
    public void candidateTagsProducesFullCascadeForLanguageCountryVariant() {
        final List<String> tags = TextBundle.candidateTags(new Locale("de", "AT", "tirol"));

        assertThat(tags).containsExactly("de_AT_tirol", "de_AT", "de", "");
    }

    @Test
    public void candidateTagsOmitsIntermediateLevelsWhenPartsAreEmpty() {
        assertThat(TextBundle.candidateTags(new Locale("de"))).containsExactly("de", "");
        assertThat(TextBundle.candidateTags(new Locale(""))).containsExactly("");
    }

    private static Map<String, String> mapOf(final String... keyValuePairs) {
        final Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }
}
