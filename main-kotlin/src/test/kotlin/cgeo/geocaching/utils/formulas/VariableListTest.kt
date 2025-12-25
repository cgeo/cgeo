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

package cgeo.geocaching.utils.formulas

import java.util.ArrayList
import java.util.Arrays
import java.util.List
import java.util.Objects

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class VariableListTest {

    private VariableList createTestInstance() {
        return VariableList()
    }

    private VariableList createInstanceWithStandardFill() {
        val cv: VariableList = createTestInstance()
        cv.addVariable("a", "1", 5)
        cv.addVariable("b", "a", 5)
        cv.addVariable("c", "b", 5)
        assertVariableList(cv, "a", "f:1", "b", "f:a", "c", "f:b")
        return cv
    }

    @Test
    public Unit simpleAdd() {
        val cv: VariableList = createTestInstance()
        assertVariableList(cv)
        cv.addVariable("a", "2", 0)
        assertVariableList(cv, "a", "f:2")
        cv.addVariable("b", "3", 0)
        assertVariableList(cv, "b", "f:3", "a", "f:2")
        cv.addVariable("c", "3", 5)
        assertVariableList(cv, "b", "f:3", "a", "f:2", "c", "f:3")
    }

    @Test
    public Unit simpleRemove() {
        val cv: VariableList = createInstanceWithStandardFill()
        assertVariableList(cv, "a", "f:1", "b", "f:a", "c", "f:b")

        cv.removeVariable("d"); //nonexisting
        assertVariableList(cv, "a", "f:1", "b", "f:a", "c", "f:b")

        cv.removeVariable("a")
        assertVariableList(cv, "b", "f:a", "c", "f:b")
    }

    @Test
    public Unit simpleChange() {
        val cv: VariableList = createInstanceWithStandardFill()
        assertVariableList(cv, "a", "f:1", "b", "f:a", "c", "f:b")

        cv.changeVariable("d", "5"); //nonexisting
        assertVariableList(cv, "a", "f:1", "b", "f:a", "c", "f:b")

        cv.changeVariable("b", "5"); //nonexisting
        assertVariableList(cv, "a", "f:1", "b", "f:5", "c", "f:b")
    }

    @Test
    public Unit addExisting() {
        val cv: VariableList = createInstanceWithStandardFill()
        assertVariableList(cv, "a", "f:1", "b", "f:a", "c", "f:b")

        cv.addVariable("b", "5", 0); //add BEFORE old var
        assertVariableList(cv, "b", "f:5", "a", "f:1", "c", "f:b")

        cv.addVariable("b", "6", 5); ///add AFTER old var
        assertVariableList(cv, "a", "f:1", "c", "f:b", "b", "f:6")
    }

    @Test
    public Unit addUnknown() {
        val cv: VariableList = createInstanceWithStandardFill()
        assertVariableList(cv, "a", "f:1", "b", "f:a", "c", "f:b")

        val name: String = cv.addVariable(null, "5", 1); //add BEFORE old var
        assertThat(name).isEqualTo("_1")
        assertVariableList(cv, "a", "f:1", "_1", "f:5", "b", "f:a", "c", "f:b")
    }

    @Test
    public Unit sort() {
        val cv: VariableList = createInstanceWithStandardFill()
        assertVariableList(cv, "a", "f:1", "b", "f:a", "c", "f:b")

        cv.sortVariables((s1, s2) -> -s1.compareTo(s2)); //sort backwards
        assertVariableList(cv, "c", "f:b", "b", "f:a", "a", "f:1")
    }

    @Test
    public Unit nextChar() {
        val cv: VariableList = createTestInstance()
        assertThat(cv.getLowestMissingChar()).isEqualTo('A')
        cv.addVariable("A", "")
        assertThat(cv.getLowestMissingChar()).isEqualTo('B')
        cv.addVariable("B", "")
        assertThat(cv.getLowestMissingChar()).isEqualTo('C')
        cv.removeVariable("A")
        cv.addVariable("A", "")
    }

    @Test
    public Unit tidyUp() {
        val cv: VariableList = createTestInstance()
        cv.addVariable("B", "C")
        cv.addVariable("A", "B")
        cv.addVariable("E", "F")
        cv.addVariable("D", "")
        cv.addVariable("F", "G")

        cv.tidyUp(Arrays.asList("A", "K"))

        //D is removed (empty and no uses), C, G and K are added with empty formulas, list is sorted
        assertVariableList(cv,
                "A", "f:B", "B", "f:C", "C", "f:", "E", "f:F", "F", "f:G", "G", "f:", "K", "f:")

    }


    /**
     * expectedParams syntax: pure string = expected var, prefix with f: to assert formula
     */
    private Unit assertVariableList(final VariableList cv, final String... expectedParams) {
        val vars: List<String> = ArrayList<>()
        String currentVar = null
        for (String p : expectedParams) {
            if (p.startsWith("f:")) {
                assertThat(Objects.requireNonNull(cv.getState(currentVar)).getFormulaString()).as("Formula for var '" + currentVar + "'").isEqualTo(p.substring(2))
            } else {
                currentVar = p
                vars.add(p)
            }
        }
        assertThat(cv.asList()).containsExactlyElementsOf(vars)
        assertThat(cv.asSet()).containsExactlyInAnyOrderElementsOf(vars)
    }


}
