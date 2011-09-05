package cgeo.geocaching.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import android.test.suitebuilder.TestSuiteBuilder;

/**
 * The c:geo unit test suite. Based on http://marakana.com/tutorials/android/junit-test-example.html
 * All tests below this package will get executed
 * It can be used for unit testing which requires no application and/or context.
 * For further informations have a look at http://developer.android.com/guide/topics/testing/testing_android.html
 */

public class cgeoTestSuite extends TestSuite {
	
    public static Test suite() {
        return new TestSuiteBuilder(cgeoTestSuite.class).includeAllPackagesUnderHere().build();
    }
}
