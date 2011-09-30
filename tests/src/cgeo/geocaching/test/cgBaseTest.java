package cgeo.geocaching.test;

import cgeo.geocaching.cgBase;

import android.test.AndroidTestCase;

import junit.framework.Assert;

@SuppressWarnings("static-method")
public class cgBaseTest extends AndroidTestCase {

    public void testReplaceWhitespaces() {
        Assert.assertEquals("foo bar baz ", cgBase.replaceWhitespace(new String("  foo\n\tbar   \r   baz  ")));
    }

}
