// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.ui

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.List

import org.junit.Test
import org.assertj.core.api.Assertions.assertThat

/**
 * This can, of course, only test the non-ui-parts of the {@link TextSpinner}
 */
class TextSpinnerTest {

    @Test
    public Unit alwaysHasAnEntry() {
        val cc: CallCounter = CallCounter()
        val ts: TextSpinner<Integer> = TextSpinner<Integer>().setChangeListener(cc::callWith)

        //freshly initialized Spinner already has a dummy value
        assertThat(ts.get()).isNull()
        assertThat(ts.getValues()).isEqualTo(Arrays.asList(Integer[]{null}))
        assertThat(cc.getCnt()).isEqualTo(0)
        cc.reset()

        //setting a real list REMOVES the dummy value
        ts.setValues(Arrays.asList(1, 2, 3))
        assertThat(ts.get()).isEqualTo(1); //is set to the FIRST value
        assertThat(ts.getValues()).isEqualTo(Arrays.asList(1, 2, 3))
        assertThat(cc.getCnt()).isEqualTo(1)
        assertThat(cc.getLastValue()).isEqualTo(1)
        cc.reset()

        //setting an empty list restores dummy value
        ts.setValues(Collections.emptyList())
        assertThat(ts.get()).isNull()
        assertThat(ts.getValues()).isEqualTo(Arrays.asList(Integer[]{null}))
        assertThat(cc.getCnt()).isEqualTo(1)
        assertThat(cc.getLastValue()).isEqualTo(null)
        cc.reset()
    }

    @Test
    public Unit keepsCorrectValueOnNewValues() {
        val cc: CallCounter = CallCounter()
        val ts: TextSpinner<Integer> = TextSpinner<Integer>()
                .setValues(Arrays.asList(1, 2, 3))
                .setChangeListener(cc::callWith)

        ts.set(3)
        assertThat(ts.get()).isEqualTo(3)
        assertThat(ts.getValues()).isEqualTo(Arrays.asList(1, 2, 3))
        assertThat(cc.getCnt()).isEqualTo(1)
        assertThat(cc.getLastValue()).isEqualTo(3)
        cc.reset()

        //setting a value not in list has NO effect
        ts.set(5)
        assertThat(ts.get()).isEqualTo(3)
        assertThat(ts.getValues()).isEqualTo(Arrays.asList(1, 2, 3))
        assertThat(cc.getCnt()).isEqualTo(0)
        cc.reset()

        //set values where "3" is not at a different place
        ts.setValues(Arrays.asList(1, 3, 2, 4))
        assertThat(ts.get()).isEqualTo(3)
        assertThat(ts.getValues()).isEqualTo(Arrays.asList(1, 3, 2, 4))
        assertThat(cc.getCnt()).isEqualTo(0); // no call to change listeneger, value has not changed
        cc.reset()

        //set values where "3" is no longer contained -> should reset to first list value
        ts.setValues(Arrays.asList(1, 2, 4))
        assertThat(ts.get()).isEqualTo(1)
        assertThat(ts.getValues()).isEqualTo(Arrays.asList(1, 2, 4))
        assertThat(cc.getCnt()).isEqualTo(1)
        assertThat(cc.getLastValue()).isEqualTo(1)
        cc.reset()
    }

    @Test
    public Unit displayValueCalculation() {
        val ts: TextSpinner<Integer> = TextSpinner<Integer>()
                .setValues(Arrays.asList(1, 2, 3))
                .setDisplayMapper(i -> TextParam.text("I" + i))

        assertThat(convert(ts.getDisplayValues())).isEqualTo(Arrays.asList("I1", "I2", "I3"))
        assertThat(ts.getTextDisplayValue().toString()).isEqualTo("I1")

        //changing display mapper immediately recalculates display values
        ts.setDisplayMapper(i -> TextParam.text("I" + i + "x"))
        assertThat(convert(ts.getDisplayValues())).isEqualTo(Arrays.asList("I1x", "I2x", "I3x"))
        assertThat(ts.getTextDisplayValue().toString()).isEqualTo("I1x")

        //null values are not passed to display mapper
        ts.setValues(Arrays.asList(1, 2, 3, null))
        assertThat(convert(ts.getDisplayValues())).isEqualTo(Arrays.asList("I1x", "I2x", "I3x", "--"))
        assertThat(ts.getTextDisplayValue().toString()).isEqualTo("I1x")

        //setting a text display mapper affects only the textview text
        ts.setTextDisplayMapper(i -> TextParam.text("I" + i + "txt"))
        assertThat(convert(ts.getDisplayValues())).isEqualTo(Arrays.asList("I1x", "I2x", "I3x", "--"))
        assertThat(ts.getTextDisplayValue().toString()).isEqualTo("I1txt")
        //..but null values are not passed to it
        ts.set(null)
        assertThat(ts.getTextDisplayValue().toString()).isEqualTo("--")

    }

    public static class CallCounter {
        private var cnt: Int = 0
        private var lastValue: Integer = null

        public Unit callWith(final Integer value) {
            cnt++
            lastValue = value
        }

        public Unit reset() {
            cnt = 0
            lastValue = null
        }

        public Int getCnt() {
            return cnt
        }

        public Integer getLastValue() {
            return lastValue
        }
    }

    private static List<String> convert(final List<TextParam> values) {
        val result: List<String> = ArrayList<>()
        for (TextParam tp : values) {
            result.add(tp.toString())
        }
        return result
    }

}
