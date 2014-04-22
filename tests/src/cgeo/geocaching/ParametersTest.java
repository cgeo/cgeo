package cgeo.geocaching;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.network.Parameters;

import android.test.AndroidTestCase;

import java.security.InvalidParameterException;

public class ParametersTest extends AndroidTestCase {

    public static void testException() {
        try {
            final Parameters params = new Parameters("aaa", "AAA", "bbb");
            params.clear(); // this will never be invoked, but suppresses warnings about unused objects
            fail("Exception not raised");
        } catch (InvalidParameterException e) {
            // Ok
        }
        try {
            final Parameters params = new Parameters("aaa", "AAA");
            params.put("bbb", "BBB", "ccc");
            fail("Exception not raised");
        } catch (InvalidParameterException e) {
            // Ok
        }
    }

    public static void testMultipleValues() {
        final Parameters params = new Parameters("aaa", "AAA", "bbb", "BBB");
        params.put("ccc", "CCC", "ddd", "DDD");
        assertThat(params.toString()).isEqualTo("aaa=AAA&bbb=BBB&ccc=CCC&ddd=DDD");
    }

    public static void testSort() {
        final Parameters params = new Parameters();
        params.put("aaa", "AAA");
        params.put("ccc", "CCC");
        params.put("bbb", "BBB");
        assertThat(params.toString()).isEqualTo("aaa=AAA&ccc=CCC&bbb=BBB");
        params.sort();
        assertThat(params.toString()).isEqualTo("aaa=AAA&bbb=BBB&ccc=CCC");
    }

    public static void testToString() {
        final Parameters params = new Parameters();
        params.put("name", "foo&bar");
        params.put("type", "moving");
        assertThat(params.toString()).isEqualTo("name=foo%26bar&type=moving");
    }

}
