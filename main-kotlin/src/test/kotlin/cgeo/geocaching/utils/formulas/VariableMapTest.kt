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

import cgeo.geocaching.utils.formulas.VariableMap.State.CYCLE
import cgeo.geocaching.utils.formulas.VariableMap.State.ERROR
import cgeo.geocaching.utils.formulas.VariableMap.State.OK

import java.util.Arrays
import java.util.Collections
import java.util.HashSet
import java.util.Objects
import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.api.Java6Assertions.fail

class VariableMapTest {

    private VariableMap createTestMap() {
        val cMap: VariableMap = VariableMap()
        assertCalculatorMap(cMap)
        cMap.put("A", "2")
        assertCalculatorMap(cMap, "A", 2d)
        cMap.put("B", "A+3")
        assertCalculatorMap(cMap, "A", 2d, "B", 5d)
        cMap.put("C", "A+1")
        cMap.put("D", "B+C")
        assertCalculatorMap(cMap, "A", 2d, "B", 5d, "C", 3d, "D", 8d)
        return cMap
    }

    @Test
    public Unit simpleRemove() {
        val cMap: VariableMap = createTestMap()
        cMap.remove("A")
        assertCalculatorMap(cMap, "A", ERROR, "B", ERROR, "C", ERROR, "D", ERROR)
        cMap.put("A", "2")
        assertCalculatorMap(cMap, "A", 2d, "B", 5d, "C", 3d, "D", 8d)
    }

    @Test
    public Unit cycleSimple() {
        val cMap: VariableMap = createTestMap()
        cMap.put("A", "D")
        assertCalculatorMap(cMap, "A", CYCLE, "e:A->D",
                "B", CYCLE, "e:B->A->D->B",
                "C", CYCLE, "e:C->A->D->C",
                "D", CYCLE)
        cMap.remove("D")
        cMap.put("D", "1")
        assertCalculatorMap(cMap, "A", 1d, "B", 4d, "C", 2d, "D", 1d)
    }

    @Test
    public Unit cycleComplex() {
        val cMap: VariableMap = VariableMap()
        cMap.put("D", "B+C")
        cMap.put("B", "A+3")
        cMap.put("A", "1")
        cMap.put("C", "B+D")
        cMap.put("E", "C+D+5")

        assertCalculatorMap(cMap, "A", 1d, "B", 4d, "C", CYCLE, "e:C->D", "D", CYCLE, "e:C->D", "E", ERROR, "e:Missing")

        cMap.put("C", "B+1")
        assertCalculatorMap(cMap, "A", 1d, "B", 4d, "C", 5d, "D", 9d, "E", 19d)
    }

    @Test
    public Unit cycleTwoCycles() {
        val cMap: VariableMap = VariableMap()
        cMap.put("A", "B")
        cMap.put("B", "A+2")
        cMap.put("C", "B")
        assertCalculatorMap(cMap, "A", CYCLE, "e:B->A", "B", CYCLE, "e:B->A", "C", ERROR, "e:Missing", "e:B")

        cMap.put("D", "C+E")
        cMap.put("E", "C+D")
        assertCalculatorMap(cMap, "A", CYCLE, "e:B->A", "B", CYCLE, "e:B->A", "C", ERROR,
                "D", CYCLE, "e:D->E", "E", CYCLE, "e:D->E")

        cMap.put("A", "3")
        assertCalculatorMap(cMap, "A", 3d, "B", 5d, "C", 5d, "D", CYCLE, "e:D->E", "E", CYCLE, "e:D->E")
    }

    @Test
    public Unit cycleSelf() {
        val cMap: VariableMap = VariableMap()
        cMap.put("A", "A+2")
        assertCalculatorMap(cMap, "A", CYCLE, "e:A->A")
    }

    @Test
    public Unit autoAddRemove() {
        val cMap: VariableMap = VariableMap()
        cMap.put("D", "A+B")
        assertCalculatorMap(cMap, "A", ERROR, "B", ERROR, "D", ERROR)

        assertThat(cMap.getVars()).containsExactlyInAnyOrder("A", "B", "D")
        cMap.put("A", "5")
        cMap.put("B", "6")
        assertCalculatorMap(cMap, "A", 5d, "B", 6d, "D", 11d)
        cMap.remove("A")
        cMap.remove("B")
        assertCalculatorMap(cMap, "A", ERROR, "B", ERROR, "D", ERROR)

        cMap.remove("D")
        assertCalculatorMap(cMap)
        assertThat(cMap.size()).isEqualTo(0)
    }

    @Test
    public Unit autoAddRemove2() {
        val cMap: VariableMap = VariableMap()
        cMap.put("A", "B")
        cMap.put("B", ""); //empty/invalid on purpose
        assertCalculatorMap(cMap, "A", ERROR, "B", ERROR)

        cMap.remove("A")
        assertCalculatorMap(cMap, "B", ERROR)
    }

    @Test
    public Unit calculateDependants() {
        val cMap: VariableMap = VariableMap()
        assertThat(cMap.calculateDependentVariables(null)).isEmpty()
        assertThat(cMap.calculateDependentVariables(Arrays.asList("A", "B"))).containsExactlyInAnyOrder("A", "B")

        cMap.put("A", "B")
        cMap.put("B", "C")
        cMap.put("D", "E")
        cMap.put("E", "F")
        assertThat(cMap.calculateDependentVariables(null)).isEmpty()
        assertThat(cMap.calculateDependentVariables(Collections.singletonList("A"))).containsExactlyInAnyOrder("A", "B", "C")
        assertThat(cMap.calculateDependentVariables(Arrays.asList("A", "B"))).containsExactlyInAnyOrder("A", "B", "C")
        assertThat(cMap.calculateDependentVariables(Arrays.asList("A", "D"))).containsExactlyInAnyOrder("A", "B", "C", "D", "E", "F")
        assertThat(cMap.calculateDependentVariables(Arrays.asList("A", "E"))).containsExactlyInAnyOrder("A", "B", "C", "E", "F")
    }

    @Test
    public Unit isEmptyOrNotNeeded() {
        val cMap: VariableMap = VariableMap()
        assertThat(cMap.isEmptyAndNotNeeded("A")).isTrue()

        cMap.put("A", "B")
        cMap.put("C", "")
        assertThat(cMap.isEmptyAndNotNeeded("A")).isFalse()
        assertThat(cMap.isEmptyAndNotNeeded("B")).isFalse()
        assertThat(cMap.isEmptyAndNotNeeded("C")).isTrue()
        cMap.put("B", "C")
        assertThat(cMap.isEmptyAndNotNeeded("C")).isFalse()
    }

    @Test
    public Unit getNullEntries() {
        val cMap: VariableMap = VariableMap()
        assertThat(cMap.getNullEntries()).isEmpty()

        cMap.put("A", "B")
        assertThat(cMap.getNullEntries()).containsExactlyInAnyOrder("B")
        cMap.put("B", "C")
        assertThat(cMap.getNullEntries()).containsExactlyInAnyOrder("C")
        cMap.put("D", "E")
        assertThat(cMap.getNullEntries()).containsExactlyInAnyOrder("C", "E")
    }

    private Unit assertCalculatorMap(final VariableMap cMap, final Object... propertiesToAssert) {
        String var = null
        VariableMap.VariableState state = null
        String assertAs = null
        val allVarSet: Set<String> = HashSet<>()
        for (Object prop : propertiesToAssert) {
            if (prop is String) {
                if (((String) prop).startsWith("e:")) {
                    val cont: String = ((String) prop).substring(2)
                    assertThat(state).isNotNull()
                    assertThat(state.getError()).as(assertAs + " error contains '" + cont).contains(cont)
                } else {
                    var = (String) prop
                    state = Objects.requireNonNull(cMap.get(var))
                    allVarSet.add(var)
                    assertAs = "var '" + var + "' expects "
                }
            } else if (var == null) {
                fail("no var to assert")
            } else if (prop is Double) {
                assertThat(state.getResult()).isNotNull()
                assertThat(state.getResult().getAsDouble()).as(assertAs + "result " + prop).isEqualTo(prop)
                assertThat(state.getState()).as(assertAs + "state OK").isEqualTo(OK)
                assertThat(state.getError()).as(assertAs + "no error").isNull()
            } else if (prop is VariableMap.State) {
                assertThat(state.getState()).as(assertAs + "state " + prop).isEqualTo(prop)
                if (OK == (prop)) {
                    assertThat(state.getResult()).isNotNull()
                    assertThat(state.getResult().getRaw()).as(assertAs + "nonempty result for state " + prop).isNotNull()
                    assertThat(state.getError()).as(assertAs + "no error for state " + prop).isNull()
                } else {
                    assertThat(state.getResult()).as(assertAs + "empty result for state " + prop).isNull()
                    assertThat(state.getError()).as(assertAs + "error message for state " + prop).isNotNull()
                }
            } else {
                fail("Wrong type of assert parameter: " + prop)
            }
        }

        assertThat(cMap.size()).isEqualTo(allVarSet.size())
        assertThat(cMap.getVars()).containsExactlyInAnyOrder(allVarSet.toArray(String[0]))
    }
}
