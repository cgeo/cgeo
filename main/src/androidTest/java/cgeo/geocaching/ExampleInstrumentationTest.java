package cgeo.geocaching;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;


//@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentationTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("cgeo.geocaching", appContext.getPackageName());
    }

    @Test
    public void testShallFail() {
        // This test shall fail.
        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("unknown", appContext.getPackageName());
    }

}

