package cgeo.geocaching.files;

import com.jayway.android.robotium.solo.Solo;

import android.annotation.TargetApi;
import android.os.Build;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.CheckBox;

import java.util.ArrayList;

@TargetApi(Build.VERSION_CODES.FROYO)
public class SimpleDirChooserUITest extends ActivityInstrumentationTestCase2<SimpleDirChooser> {

    private Solo solo;

    public SimpleDirChooserUITest() {
        super(SimpleDirChooser.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
    }

    public void testSingleSelection() throws InterruptedException {
        // normally our activity should be ready, but we already had Jenkins report no checkboxes right here at the beginning
        solo.waitForActivity(solo.getCurrentActivity().getClass().getSimpleName(), 2000);

        assertChecked("Newly opened activity", 0);
        solo.scrollToBottom();
        pause();
        // according to the documentation, automatic pauses only happen in the clickXYZ() methods.
        // Therefore lets introduce a manual pause after the scrolling methods.

        final int lastIndex = solo.getCurrentCheckBoxes().size() - 1;

        solo.clickOnCheckBox(lastIndex);
        assertTrue(solo.getCurrentCheckBoxes().get(lastIndex).isChecked());
        assertFalse(solo.getCurrentCheckBoxes().get(0).isChecked());
        assertChecked("Clicked last checkbox", 1);

        solo.scrollUp();
        pause();
        solo.scrollToBottom();
        pause();
        assertChecked("Refreshing last checkbox", 1);

        solo.scrollToTop();
        pause();
        solo.clickOnCheckBox(0);
        assertChecked("Clicked first checkbox", 1);
        assertTrue(solo.getCurrentCheckBoxes().get(0).isChecked());
        solo.clickOnCheckBox(1);
        assertChecked("Clicked second checkbox", 1);
        assertTrue(solo.getCurrentCheckBoxes().get(1).isChecked());
    }

    private static void pause() throws InterruptedException {
        Thread.sleep(500);
    }

    private void assertChecked(String message, int expectedChecked) {
        int checked = 0;
        final ArrayList<CheckBox> boxes = solo.getCurrentCheckBoxes();
        assertNotNull("Could not get checkboxes", boxes);
        assertTrue("There are no checkboxes", boxes.size() > 1);
        for (CheckBox checkBox : boxes) {
            if (checkBox.isChecked()) {
                checked++;
            }
        }
        assertEquals(message, expectedChecked, checked);
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
        super.tearDown();
    }
}
