package cgeo.geocaching.utils;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.connector.gc.GCConstants;

import junit.framework.TestCase;

public class CryptUtilsTest extends TestCase {
    public static void testROT13() {
        assertThat(CryptUtils.rot13("")).isEqualTo("");
        assertThat(CryptUtils.rot13((String) null)).isEqualTo("");
        assertThat(CryptUtils.rot13("Cache hint")).isEqualTo("Pnpur uvag");
        assertThat(CryptUtils.rot13("Cache [plain] hint")).isEqualTo("Pnpur [plain] uvag");
        assertThat(CryptUtils.rot13("[all plain]")).isEqualTo("[all plain]");
        assertThat(CryptUtils.rot13("123")).isEqualTo("123");
    }

    public static void testConvertToGcBase31() {
        assertThat(GCConstants.gccodeToGCId("GC1PKK9")).isEqualTo(1186660);
        assertThat(GCConstants.gccodeToGCId("GC1234")).isEqualTo(4660);
        assertThat(GCConstants.gccodeToGCId("GCF123")).isEqualTo(61731);
    }

    public static void testIssue1902() {
        assertThat(CryptUtils.rot13("ƖƖyƖƖƖƖ")).isEqualTo("ƖƖlƖƖƖƖ");
    }

    public static void testMd5() {
        assertThat(CryptUtils.md5("")).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
        // expected value taken from debugger. should assure every developer uses UTF-8
        assertThat(CryptUtils.md5("äöü")).isEqualTo("a7f4e3ec08f09be2ef7ecb4eea5f8981");
    }
}
