package cgeo.geocaching.test;

import static cgeo.geocaching.cgBase.buildURI;

import android.test.AndroidTestCase;

import java.net.URI;

import junit.framework.Assert;

public class URITest extends AndroidTestCase {

    public void testBasicURI() {
        Assert.assertEquals("http://www.example.com/foo/bar",
                buildURI(false, "www.example.com", "/foo/bar").toString());
    }

    public void testSecureURI() {
        Assert.assertEquals("https://www.example.com/foo/bar",
                buildURI(true, "www.example.com", "/foo/bar").toString());
    }

    public void testWithPath() {
        Assert.assertEquals("http://www.example.com/foo/bar?a=b&c=d",
                buildURI(false, "www.example.com", "/foo/bar", "a=b&c=d").toString());
    }

    public void testWithSplitPath() {
        final URI uri = buildURI(false, "www.example.com", "/foo/bar?a=b&c=d");
        Assert.assertEquals("http://www.example.com/foo/bar?a=b&c=d", uri.toString());
        Assert.assertEquals("a=b&c=d", uri.getQuery());
    }

}
