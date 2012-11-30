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

    public void testSingleSelection() {
        assertChecked(0);

        solo.scrollToBottom();
        final ArrayList<CheckBox> boxes = solo.getCurrentCheckBoxes();
        final int lastIndex = boxes.size() - 1;
        solo.clickOnCheckBox(lastIndex);
        assertTrue(solo.getCurrentCheckBoxes().get(lastIndex).isChecked());
        assertFalse(solo.getCurrentCheckBoxes().get(0).isChecked());
        assertChecked(1);

        solo.scrollUp();
        assertChecked(1);

        solo.clickOnCheckBox(0);
        assertChecked(1);
        assertTrue(solo.getCurrentCheckBoxes().get(0).isChecked());
    }

    private void assertChecked(int expectedChecked) {
        int checked = 0;
        final ArrayList<CheckBox> boxes = solo.getCurrentCheckBoxes();
        assertNotNull(boxes);
        assertTrue(boxes.size() > 1);
        for (CheckBox checkBox : boxes) {
            if (checkBox.isChecked()) {
                checked++;
            }
        }
        assertEquals(expectedChecked, checked);
    }

}
