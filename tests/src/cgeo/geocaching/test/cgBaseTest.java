package cgeo.geocaching.test;

import cgeo.geocaching.cgBase;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class cgBaseTest extends AndroidTestCase {

    public void testReplaceWhitespaces() {
        StringBuffer buffer = new StringBuffer("  foo\n\tbar   \r   baz  ");
        Assert.assertEquals("foo bar baz", cgBase.replaceWhitespace(buffer));
    }

}
