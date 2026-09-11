package cgeo.geocaching.utils;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-JVM tests for {@link TextUtils#foldDiacritics(String)} and
 * {@link TextUtils#containsIgnoreCaseAndDiacritics(String, String)} (#16206).
 */
public class TextUtilsDiacriticsTest {

    @Test
    public void foldsCombiningMarkDiacriticsToBaseLetter() {
        // Western European
        assertThat(TextUtils.foldDiacritics("àáâãäåā")).isEqualTo("aaaaaaa");
        assertThat(TextUtils.foldDiacritics("èéêëēĕėẽ")).isEqualTo("eeeeeeee");
        assertThat(TextUtils.foldDiacritics("ìíîïī")).isEqualTo("iiiii");
        assertThat(TextUtils.foldDiacritics("òóôõöōő")).isEqualTo("ooooooo");
        assertThat(TextUtils.foldDiacritics("ùúûüūůű")).isEqualTo("uuuuuuu");
        assertThat(TextUtils.foldDiacritics("çñ")).isEqualTo("cn");
        // Slavic (caron / acute / breve)
        assertThat(TextUtils.foldDiacritics("šžčřě")).isEqualTo("szcre");
        assertThat(TextUtils.foldDiacritics("ćńśźż")).isEqualTo("cnszz");
        // Baltic / cedilla-below
        assertThat(TextUtils.foldDiacritics("āēīūķļņģ")).isEqualTo("aeiuklng");
    }

    @Test
    public void foldsBothRomanianSVariants() {
        // U+015F (s with cedilla) and U+0219 (s with comma below) must both fold to "s"
        assertThat(TextUtils.foldDiacritics("la du\u015f")).isEqualTo("la dus");
        assertThat(TextUtils.foldDiacritics("la du\u0219")).isEqualTo("la dus");
        // and the corresponding t variants
        assertThat(TextUtils.foldDiacritics("\u0163\u021b")).isEqualTo("tt");
    }

    @Test
    public void foldsAtomicLettersWithoutCanonicalDecomposition() {
        assertThat(TextUtils.foldDiacritics("ß")).isEqualTo("ss");
        assertThat(TextUtils.foldDiacritics("ø")).isEqualTo("o");
        assertThat(TextUtils.foldDiacritics("æ")).isEqualTo("ae");
        assertThat(TextUtils.foldDiacritics("œ")).isEqualTo("oe");
        assertThat(TextUtils.foldDiacritics("ł")).isEqualTo("l");
        assertThat(TextUtils.foldDiacritics("đ")).isEqualTo("d");
        assertThat(TextUtils.foldDiacritics("ð")).isEqualTo("d");
        assertThat(TextUtils.foldDiacritics("þ")).isEqualTo("th");
        assertThat(TextUtils.foldDiacritics("ħ")).isEqualTo("h");
        assertThat(TextUtils.foldDiacritics("ŧ")).isEqualTo("t");
        assertThat(TextUtils.foldDiacritics("ŋ")).isEqualTo("n");
        assertThat(TextUtils.foldDiacritics("ĸ")).isEqualTo("k");
        assertThat(TextUtils.foldDiacritics("ſ")).isEqualTo("s");
        assertThat(TextUtils.foldDiacritics("ı")).isEqualTo("i");
    }

    @Test
    public void foldsUppercaseIncludingAtomicAndCapitalSharpS() {
        assertThat(TextUtils.foldDiacritics("MÜLLER")).isEqualTo("muller");
        assertThat(TextUtils.foldDiacritics("ZÜRICH")).isEqualTo("zurich");
        assertThat(TextUtils.foldDiacritics("GROSSE STRAßE")).isEqualTo("grosse strasse");
        assertThat(TextUtils.foldDiacritics("STRA\u1E9EE")).isEqualTo("strasse"); // capital sharp S U+1E9E
        assertThat(TextUtils.foldDiacritics("ØÆŒŁÐÞ")).isEqualTo("oaeoeldth");
    }

    @Test
    public void foldsViaNfdVietnameseHornAndDottedCapitalI() {
        // Vietnamese horn letters have a canonical decomposition -> handled by NFD, no explicit mapping
        assertThat(TextUtils.foldDiacritics("ơư")).isEqualTo("ou");
        assertThat(TextUtils.foldDiacritics("Ơ\u01B0")).isEqualTo("ou");
        // Turkish dotted capital I lowercases to "i" + combining dot, which NFD strips
        assertThat(TextUtils.foldDiacritics("\u0130stanbul")).isEqualTo("istanbul");
        // dotless lowercase i needs the explicit mapping
        assertThat(TextUtils.foldDiacritics("\u0131stanbul")).isEqualTo("istanbul");
    }

    @Test
    public void preservesAsciiDigitsPunctuationAndSpaces() {
        assertThat(TextUtils.foldDiacritics("Fake Cache No 5")).isEqualTo("fake cache no 5");
        assertThat(TextUtils.foldDiacritics("GC12345")).isEqualTo("gc12345");
        assertThat(TextUtils.foldDiacritics("a-b_c.d (e)")).isEqualTo("a-b_c.d (e)");
        assertThat(TextUtils.foldDiacritics("  keep  spaces  ")).isEqualTo("  keep  spaces  ");
    }

    @Test
    public void handlesNullAndEmpty() {
        assertThat(TextUtils.foldDiacritics(null)).isNull();
        assertThat(TextUtils.foldDiacritics("")).isEqualTo("");
    }

    @Test
    public void foldsRealWorldCacheAndOwnerNames() {
        assertThat(TextUtils.foldDiacritics("straße")).isEqualTo("strasse");
        assertThat(TextUtils.foldDiacritics("søen")).isEqualTo("soen");
        assertThat(TextUtils.foldDiacritics("łódź")).isEqualTo("lodz");
        assertThat(TextUtils.foldDiacritics("Þór")).isEqualTo("thor");
        assertThat(TextUtils.foldDiacritics("æøå")).isEqualTo("aeoa");
        assertThat(TextUtils.foldDiacritics("cœur")).isEqualTo("coeur");
        assertThat(TextUtils.foldDiacritics("Ærøskøbing")).isEqualTo("aeroskobing");
        assertThat(TextUtils.foldDiacritics("Þórsmörk")).isEqualTo("thorsmork");
        assertThat(TextUtils.foldDiacritics("Ĉefa")).isEqualTo("cefa");
        assertThat(TextUtils.foldDiacritics("Ħaġar Qim")).isEqualTo("hagar qim");
        assertThat(TextUtils.foldDiacritics("kękę")).isEqualTo("keke");
        assertThat(TextUtils.foldDiacritics("Åre")).isEqualTo("are");
        assertThat(TextUtils.foldDiacritics("Řeka")).isEqualTo("reka");
    }

    @Test
    public void containsIgnoreCaseAndDiacriticsMatchesFoldedSubstring() {
        assertThat(TextUtils.containsIgnoreCaseAndDiacritics("la duş", "dus")).isTrue();
        assertThat(TextUtils.containsIgnoreCaseAndDiacritics("la du\u0219", "DUS")).isTrue();
        assertThat(TextUtils.containsIgnoreCaseAndDiacritics("Große Straße", "grosse strasse")).isTrue();
        assertThat(TextUtils.containsIgnoreCaseAndDiacritics("Zürich", "ZUR")).isTrue();
        assertThat(TextUtils.containsIgnoreCaseAndDiacritics("łódź", "lodz")).isTrue();
        assertThat(TextUtils.containsIgnoreCaseAndDiacritics("Müller", "xyz")).isFalse();
        assertThat(TextUtils.containsIgnoreCaseAndDiacritics(null, "a")).isFalse();
        assertThat(TextUtils.containsIgnoreCaseAndDiacritics("a", null)).isFalse();
    }
}
