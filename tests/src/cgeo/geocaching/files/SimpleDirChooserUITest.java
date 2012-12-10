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
        solo = new Solo(getInstrumentation(), getActivity());
    }

    public void testSingleSelection() throws InterruptedException {
        final ArrayList<CheckBox> boxes = solo.getCurrentCheckBoxes();
        final int lastIndex = boxes.size() - 1;
        assertChecked("Newly opened activity", 0);

        solo.scrollToBottom();
        solo.clickOnCheckBox(lastIndex);
        assertTrue(solo.getCurrentCheckBoxes().get(lastIndex).isChecked());
        assertFalse(solo.getCurrentCheckBoxes().get(0).isChecked());
        assertChecked("Clicked last checkbox", 1);

        solo.scrollUp();
        Thread.sleep(20);
        solo.scrollToBottom();
        assertChecked("Refreshing last checkbox", 1);

        solo.scrollToTop();
        solo.clickOnCheckBox(0);
        assertChecked("Clicked first checkbox", 1);
        assertTrue(solo.getCurrentCheckBoxes().get(0).isChecked());
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

}
