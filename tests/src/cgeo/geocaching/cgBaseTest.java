package cgeo.geocaching;

import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.BaseUtils;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class cgBaseTest extends AndroidTestCase {

    public static void testReplaceWhitespaces() {
        Assert.assertEquals("foo bar baz ", BaseUtils.replaceWhitespace(new String("  foo\n\tbar   \r   baz  ")));
    }

    public static void testElevation() {
        Assert.assertEquals(125.663703918457, cgBase.getElevation(new Geopoint(48.0, 2.0)), 0.1);
    }

}
