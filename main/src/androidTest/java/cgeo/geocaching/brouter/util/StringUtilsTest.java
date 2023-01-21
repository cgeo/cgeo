package cgeo.geocaching.brouter.util;

import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest {
    private static final String[] raw = new String[]{"hallo", "is 1<2 ?", "or 4>5 ?", "or 1<>2 ?", "\"hi\" 'there'"};
    private static final String[] xml = new String[]{"hallo", "is 1&lt;2 ?", "or 4&gt;5 ?", "or 1&lt;&gt;2 ?", "&quot;hi&quot; &apos;there&apos;"};
    private static final String[] jsn = new String[]{"hallo", "is 1<2 ?", "or 4>5 ?", "or 1<>2 ?", "\\\"hi\\\" \\'there\\'"};

    @Test
    public void xmlEncodingTest() {
        for (int i = 0; i < raw.length; i++) {
            Assert.assertEquals("xml encoding mismatch for raw: " + raw[i], xml[i], StringUtils.escapeXml10(raw[i]));
        }
    }

    @Test
    public void jsonEncodingTest() {
        for (int i = 0; i < raw.length; i++) {
            Assert.assertEquals("json encoding mismatch for raw: " + raw[i], jsn[i], StringUtils.escapeJson(raw[i]));
        }
    }
}
