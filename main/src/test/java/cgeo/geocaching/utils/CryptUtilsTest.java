package cgeo.geocaching.utils;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CryptUtilsTest {
    @Test
    public void testROT13() {
        assertThat(CryptUtils.rot13("")).isEqualTo("");
        assertThat(CryptUtils.rot13((String) null)).isEqualTo("");
        assertThat(CryptUtils.rot13("Cache hint")).isEqualTo("Pnpur uvag");
        assertThat(CryptUtils.rot13("Cache [plain] hint")).isEqualTo("Pnpur [plain] uvag");
        assertThat(CryptUtils.rot13("[all plain]")).isEqualTo("[all plain]");
        assertThat(CryptUtils.rot13("123")).isEqualTo("123");
    }

    @Test
    public void testIssue1902() {
        assertThat(CryptUtils.rot13("ƖƖyƖƖƖƖ")).isEqualTo("ƖƖlƖƖƖƖ");
    }

    @Test
    public void testMd5() {
        assertThat(CryptUtils.md5("")).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
        // expected value taken from debugger. should assure every developer uses UTF-8
        assertThat(CryptUtils.md5("äöü")).isEqualTo("a7f4e3ec08f09be2ef7ecb4eea5f8981");
    }
}
