package cgeo.geocaching.connector.gc;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.enumerations.StatusCode;

import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;

import android.test.suitebuilder.annotation.Suppress;

public class GCLoginTest extends TestCase {

    final GCLogin instance = GCLogin.getInstance();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertThat(instance.login()).isEqualTo(StatusCode.NO_ERROR);
    }

    public static void testHomeLocation() {
        assertThat(StringUtils.isNotBlank(GCLogin.retrieveHomeLocation().toBlocking().value())).isTrue();
    }

    @Suppress // It currently fails on CI
    public void testAvatar() {
        assertThat(instance.downloadAvatar()).isNotNull();
    }

}
