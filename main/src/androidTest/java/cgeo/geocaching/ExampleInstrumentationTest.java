package cgeo.geocaching;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

//@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentationTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("cgeo.geocaching".equals(appContext.getPackageName()) || "cgeo.geocaching.developer".equals(appContext.getPackageName()));
    }
}
