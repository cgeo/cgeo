package cgeo.geocaching.files;

import cgeo.geocaching.files.LocalStorage;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class LocalStorageTest extends AndroidTestCase {

    public static void testGetExtension() {
        Assert.assertEquals("", LocalStorage.getExtension("foo/bar/xyzzy"));
        Assert.assertEquals(".jpg", LocalStorage.getExtension("foo/bar/xyzzy.jpg"));
        Assert.assertEquals(".jpeg", LocalStorage.getExtension("foo/bar/xyzzy.jpeg"));
        Assert.assertEquals("", LocalStorage.getExtension("foo/bar/xyzzy.mjpeg"));
    }

}
