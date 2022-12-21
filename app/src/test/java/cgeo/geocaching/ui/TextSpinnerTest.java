package cgeo.geocaching.ui;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This can, of course, only test the non-ui-parts of the {@link TextSpinner}
 */
public class TextSpinnerTest {

    @Test
    public void alwaysHasAnEntry() {
        final CallCounter cc = new CallCounter();
        final TextSpinner<Integer> ts = new TextSpinner<Integer>().setChangeListener(cc::callWith);

        //freshly initialized Spinner already has a dummy value
        assertThat(ts.get()).isNull();
        assertThat(ts.getValues()).isEqualTo(Arrays.asList(new Integer[]{null}));
        assertThat(cc.getCnt()).isEqualTo(0);
        cc.reset();

        //setting a real list REMOVES the dummy value
        ts.setValues(Arrays.asList(new Integer[]{1, 2, 3}));
        assertThat(ts.get()).isEqualTo(1); //is set to the FIRST value
        assertThat(ts.getValues()).isEqualTo(Arrays.asList(new Integer[]{1, 2, 3}));
        assertThat(cc.getCnt()).isEqualTo(1);
        assertThat(cc.getLastValue()).isEqualTo(1);
        cc.reset();

        //setting an empty list restores dummy value
        ts.setValues(Collections.emptyList());
        assertThat(ts.get()).isNull();
        assertThat(ts.getValues()).isEqualTo(Arrays.asList(new Integer[]{null}));
        assertThat(cc.getCnt()).isEqualTo(1);
        assertThat(cc.getLastValue()).isEqualTo(null);
        cc.reset();
    }

    @Test
    public void keepsCorrectValueOnNewValues() {
        final CallCounter cc = new CallCounter();
        final TextSpinner<Integer> ts = new TextSpinner<Integer>()
                .setValues(Arrays.asList(new Integer[]{1, 2, 3}))
                .setChangeListener(cc::callWith);

        ts.set(3);
        assertThat(ts.get()).isEqualTo(3);
        assertThat(ts.getValues()).isEqualTo(Arrays.asList(new Integer[]{1, 2, 3}));
        assertThat(cc.getCnt()).isEqualTo(1);
        assertThat(cc.getLastValue()).isEqualTo(3);
        cc.reset();

        //setting a value not in list has NO effect
        ts.set(5);
        assertThat(ts.get()).isEqualTo(3);
        assertThat(ts.getValues()).isEqualTo(Arrays.asList(new Integer[]{1, 2, 3}));
        assertThat(cc.getCnt()).isEqualTo(0);
        cc.reset();

        //set new values where "3" is not at a different place
        ts.setValues(Arrays.asList(new Integer[]{1, 3, 2, 4}));
        assertThat(ts.get()).isEqualTo(3);
        assertThat(ts.getValues()).isEqualTo(Arrays.asList(new Integer[]{1, 3, 2, 4}));
        assertThat(cc.getCnt()).isEqualTo(0); // no call to change listeneger, value has not changed
        cc.reset();

        //set new values where "3" is no longer contained -> should reset to first list value
        ts.setValues(Arrays.asList(new Integer[]{1, 2, 4}));
        assertThat(ts.get()).isEqualTo(1);
        assertThat(ts.getValues()).isEqualTo(Arrays.asList(new Integer[]{1, 2, 4}));
        assertThat(cc.getCnt()).isEqualTo(1);
        assertThat(cc.getLastValue()).isEqualTo(1);
        cc.reset();
    }

    @Test
    public void displayValueCalculation() {
        final TextSpinner<Integer> ts = new TextSpinner<Integer>()
                .setValues(Arrays.asList(new Integer[]{1, 2, 3}))
                .setDisplayMapper(i -> "I" + i);

        assertThat(ts.getDisplayValues()).isEqualTo(Arrays.asList(new String[]{"I1", "I2", "I3"}));
        assertThat(ts.getTextDisplayValue()).isEqualTo("I1");

        //changing display mapper immediately recalculates display values
        ts.setDisplayMapper(i -> "I" + i + "x");
        assertThat(ts.getDisplayValues()).isEqualTo(Arrays.asList(new String[]{"I1x", "I2x", "I3x"}));
        assertThat(ts.getTextDisplayValue()).isEqualTo("I1x");

        //null values are not passed to display mapper
        ts.setValues(Arrays.asList(new Integer[]{1, 2, 3, null}));
        assertThat(ts.getDisplayValues()).isEqualTo(Arrays.asList(new String[]{"I1x", "I2x", "I3x", "--"}));
        assertThat(ts.getTextDisplayValue()).isEqualTo("I1x");

        //setting a text display mapper affects only the textview text
        ts.setTextDisplayMapper(i -> "I" + i + "txt");
        assertThat(ts.getDisplayValues()).isEqualTo(Arrays.asList(new String[]{"I1x", "I2x", "I3x", "--"}));
        assertThat(ts.getTextDisplayValue()).isEqualTo("I1txt");
        //..but null values are not passed to it
        ts.set(null);
        assertThat(ts.getTextDisplayValue()).isEqualTo("--");

    }

    public static class CallCounter {
        private int cnt = 0;
        private Integer lastValue = null;

        public void callWith(final Integer value) {
            cnt++;
            lastValue = value;
        }

        public void reset() {
            cnt = 0;
            lastValue = null;
        }

        public int getCnt() {
            return cnt;
        }

        public Integer getLastValue() {
            return lastValue;
        }
    }

}
