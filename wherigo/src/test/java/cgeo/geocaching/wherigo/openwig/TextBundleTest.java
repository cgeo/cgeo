package cgeo.geocaching.wherigo.openwig;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link TextBundle}'s single-file, section-delimited parsing and its ResourceBundle-
 * style fallback chain, which is pure logic once the resource's raw bytes are available - the
 * only Engine dependency (fetching those bytes from the cartridge) is swapped out here via a test
 * subclass overriding readOwnBytes(). The language tag is always passed in explicitly, never read
 * from the device/JVM locale.
 */
public class TextBundleTest {

    private static final class TestableTextBundle extends TextBundle {
        private byte[] content = new byte[0];

        void setContent(final String text) {
            content = text.getBytes(StandardCharsets.UTF_8);
        }

        protected byte[] readOwnBytes() {
            return content;
        }
    }

    private static TestableTextBundle bundleWithText(final String text) {
        final TestableTextBundle bundle = new TestableTextBundle();
        bundle.setContent(text);
        return bundle;
    }

    @Test
    public void fileWithoutSectionHeadersIsTreatedAsDefaultSection() {
        final TestableTextBundle bundle = bundleWithText("greeting=Hello\n");

        assertThat(bundle.getText("greeting", "fr_FR")).isEqualTo("Hello");
    }

    @Test
    public void getTextPrefersLanguageSpecificOverDefault() {
        final TestableTextBundle bundle = bundleWithText(
            "[]\n" +
            "greeting=Hello\n" +
            "[de]\n" +
            "greeting=Hallo\n"
        );

        assertThat(bundle.getText("greeting", "de_DE")).isEqualTo("Hallo");
    }

    @Test
    public void getTextPrefersLanguageCountryOverLanguageOverDefault() {
        final TestableTextBundle bundle = bundleWithText(
            "[]\n" +
            "greeting=Hello\n" +
            "[de]\n" +
            "greeting=Hallo (DE)\n" +
            "[de_AT]\n" +
            "greeting=Servus\n"
        );

        assertThat(bundle.getText("greeting", "de_AT")).isEqualTo("Servus");
    }

    @Test
    public void getTextFallsBackPerKeyNotPerSection() {
        // [de_AT] exists but doesn't define "farewell" - must fall through to [de], then default,
        // exactly like a real ResourceBundle's parent-delegation, not stop at the first section found
        final TestableTextBundle bundle = bundleWithText(
            "[]\n" +
            "greeting=Hello\n" +
            "farewell=Goodbye\n" +
            "[de]\n" +
            "greeting=Hallo\n" +
            "farewell=Auf Wiedersehen\n" +
            "[de_AT]\n" +
            "greeting=Servus\n"
        );

        assertThat(bundle.getText("greeting", "de_AT")).isEqualTo("Servus");
        assertThat(bundle.getText("farewell", "de_AT")).isEqualTo("Auf Wiedersehen");
    }

    @Test
    public void getTextReturnsNullWhenKeyMissingEverywhere() {
        final TestableTextBundle bundle = bundleWithText("greeting=Hello\n");

        assertThat(bundle.getText("nonexistent", "de_AT")).isNull();
    }

    @Test
    public void getTextTreatsNullAndEmptyLanguageAsDefaultOnly() {
        final TestableTextBundle bundle = bundleWithText(
            "[]\n" +
            "greeting=Hello\n" +
            "[de]\n" +
            "greeting=Hallo\n"
        );

        assertThat(bundle.getText("greeting", null)).isEqualTo("Hello");
        assertThat(bundle.getText("greeting", "")).isEqualTo("Hello");
    }

    @Test
    public void getTextReturnsNullWhenResourceIsEmpty() {
        assertThat(new TestableTextBundle().getText("greeting", "de")).isNull();
    }

    @Test
    public void getTextReturnsNullForNullKey() {
        final TestableTextBundle bundle = bundleWithText("greeting=Hello\n");

        assertThat(bundle.getText(null, "de")).isNull();
    }

    @Test
    public void sectionHeaderCanAppearAfterContentAlreadyBoundToDefault() {
        // anything before the first header belongs to the default section, even if a header
        // never explicitly opens with "[]"
        final TestableTextBundle bundle = bundleWithText(
            "greeting=Hello\n" +
            "[de]\n" +
            "greeting=Hallo\n"
        );

        assertThat(bundle.getText("greeting", "fr")).isEqualTo("Hello");
        assertThat(bundle.getText("greeting", "de")).isEqualTo("Hallo");
    }

    @Test
    public void utf8BomAtStartOfFileDoesNotBreakFirstSectionHeader() {
        final TestableTextBundle bundle = bundleWithText(
            "\uFEFF[]\n" +
            "greeting=Hello\n" +
            "[de]\n" +
            "greeting=Hallo\n"
        );

        assertThat(bundle.getText("greeting", "fr")).isEqualTo("Hello");
        assertThat(bundle.getText("greeting", "de")).isEqualTo("Hallo");
    }

    @Test
    public void sectionAppearingTwiceMergesRatherThanOverwrites() {
        final TestableTextBundle bundle = bundleWithText(
            "[de]\n" +
            "greeting=Hallo\n" +
            "[fr]\n" +
            "greeting=Bonjour\n" +
            "[de]\n" +
            "farewell=Auf Wiedersehen\n"
        );

        assertThat(bundle.getText("greeting", "de")).isEqualTo("Hallo");
        assertThat(bundle.getText("farewell", "de")).isEqualTo("Auf Wiedersehen");
    }

    @Test
    public void candidateTagsProducesFullCascadeForLanguageCountryVariant() {
        final List<String> tags = TextBundle.candidateTags("de_AT_tirol");

        assertThat(tags).containsExactly("de_AT_tirol", "de_AT", "de", "");
    }

    @Test
    public void candidateTagsOmitsIntermediateLevelsWhenPartsAreEmpty() {
        assertThat(TextBundle.candidateTags("de")).containsExactly("de", "");
        assertThat(TextBundle.candidateTags("")).containsExactly("");
        assertThat(TextBundle.candidateTags(null)).containsExactly("");
    }
}
