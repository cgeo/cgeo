package cgeo.geocaching.utils.calc;

import static cgeo.geocaching.utils.calc.CalculatorMap.State.CYCLE;
import static cgeo.geocaching.utils.calc.CalculatorMap.State.ERROR;
import static cgeo.geocaching.utils.calc.CalculatorMap.State.OK;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

public class CalculatorMapTest {

    private CalculatorMap createTestMap() {
        final CalculatorMap cMap = new CalculatorMap();
        assertCalculatorMap(cMap, true);
        cMap.put("A", "2");
        assertCalculatorMap(cMap, true, "A", 2d);
        cMap.put("B", "A+3");
        assertCalculatorMap(cMap, true, "A", 2d, "B", 5d);
        cMap.put("C", "A+1");
        cMap.put("D", "B+C");
        assertCalculatorMap(cMap, true, "A", 2d, "B", 5d, "C", 3d, "D", 8d);
        return cMap;
    }

    @Test
    public void simpleRemove() {
        final CalculatorMap cMap = createTestMap();
        cMap.remove("A");
        assertCalculatorMap(cMap, true, "A", ERROR, "B", ERROR, "C", ERROR, "D", ERROR);
        cMap.put("A", "2");
        assertCalculatorMap(cMap, true, "A", 2d, "B", 5d, "C", 3d, "D", 8d);
    }

    @Test
    public void cycleSimple() {
        final CalculatorMap cMap = createTestMap();
        cMap.put("A", "D");
        assertCalculatorMap(cMap, true, "A", CYCLE, "e:A->D",
            "B", CYCLE, "e:B->A->D->B",
            "C", CYCLE, "e:C->A->D->C",
            "D", CYCLE);
        cMap.remove("D");
        cMap.put("D", "1");
        assertCalculatorMap(cMap, true,  "A", 1d, "B", 4d, "C", 2d, "D", 1d);
    }

    @Test
    public void cycleComplex() {
        final CalculatorMap cMap = new CalculatorMap();
        cMap.put("D", "B+C");
        cMap.put("B", "A+3");
        cMap.put("A", "1");
        cMap.put("C", "B+D");
        cMap.put("E", "C+D+5");

        assertCalculatorMap(cMap, true, "A", 1d, "B", 4d, "C", CYCLE, "e:C->D", "D", CYCLE, "e:C->D", "E", ERROR, "e:missing");

        cMap.put("C", "B+1");
        assertCalculatorMap(cMap, true, "A", 1d, "B", 4d, "C", 5d, "D", 9d, "E", 19d);
    }

    @Test
    public void cycleTwoCycles() {
        final CalculatorMap cMap = new CalculatorMap();
        cMap.put("A", "B");
        cMap.put("B", "A+2");
        cMap.put("C", "B");
        assertCalculatorMap(cMap, true, "A", CYCLE, "e:B->A", "B", CYCLE, "e:B->A", "C", ERROR, "e:missing", "e:B");

        cMap.put("D", "C+E");
        cMap.put("E", "C+D");
        assertCalculatorMap(cMap, true, "A", CYCLE, "e:B->A", "B", CYCLE, "e:B->A", "C", ERROR,
            "D", CYCLE, "e:D->E", "E", CYCLE, "e:D->E");

        cMap.put("A", "3");
        assertCalculatorMap(cMap, true, "A", 3d, "B", 5d, "C", 5d, "D", CYCLE, "e:D->E", "E", CYCLE, "e:D->E");
    }

    @Test
    public void cycleSelf() {
        final CalculatorMap cMap = new CalculatorMap();
        cMap.put("A", "A+2");
        assertCalculatorMap(cMap, true, "A", CYCLE, "e:A->A");
    }

    @Test
    public void autoAddRemove() {
        final CalculatorMap cMap = new CalculatorMap();
        cMap.put("D", "A+B");
        assertCalculatorMap(cMap, true, "A", ERROR, "B", ERROR, "D", ERROR);

        assertThat(cMap.getVars()).containsExactlyInAnyOrder("A", "B", "D");
        cMap.put("A", "5");
        cMap.put("B", "6");
        assertCalculatorMap(cMap, true, "A", 5d, "B", 6d, "D", 11d);
        cMap.remove("A");
        cMap.remove("B");
        assertCalculatorMap(cMap, true, "A", ERROR, "B", ERROR, "D", ERROR);

        cMap.remove("D");
        assertCalculatorMap(cMap, true);
        assertThat(cMap.size()).isEqualTo(0);
    }

    private void assertCalculatorMap(final CalculatorMap cMap, final boolean allVars, final Object ... propertiesToAssert) {
        String var = null;
        String assertAs = null;
        final Set<String> allVarSet = new HashSet<>();
        for (Object prop : propertiesToAssert) {
            if (prop instanceof String) {
                if (((String) prop).startsWith("e:")) {
                    final String cont = ((String) prop).substring(2);
                    assertThat(cMap.get(var).getError()).as(assertAs + " error contains '" + cont).contains(cont);
                } else {
                    var = (String) prop;
                    allVarSet.add(var);
                    assertAs = "var '" + var + "' expects ";
                }
            } else if (var == null) {
                fail("no var to assert");
            } else if (prop instanceof Double) {
                assertThat(cMap.get(var).getResult()).as(assertAs + "result " + prop).isEqualTo(prop);
                assertThat(cMap.get(var).getState()).as(assertAs + "state OK").isEqualTo(OK);
                assertThat(cMap.get(var).getError()).as(assertAs + "no error").isNull();
            } else if (prop instanceof CalculatorMap.State) {
                assertThat(cMap.get(var).getState()).as(assertAs + "state " + prop).isEqualTo(prop);
                if (OK.equals(prop)) {
                    assertThat(cMap.get(var).getResult()).as(assertAs + "nonempty result for state " + prop).isNotNull();
                    assertThat(cMap.get(var).getError()).as(assertAs + "no error for state " + prop).isNull();
                } else {
                    assertThat(cMap.get(var).getResult()).as(assertAs + "empty result for state " + prop).isNull();
                    assertThat(cMap.get(var).getError()).as(assertAs + "error message for state " + prop).isNotNull();
                }
            } else {
                fail("Wrong type of assert parameter: " + prop);
            }
        }

        if (allVars) {
            assertThat(cMap.size()).isEqualTo(allVarSet.size());
            assertThat(cMap.getVars()).containsExactlyInAnyOrder(allVarSet.toArray(new String[0]));
        } else {
            assertThat(cMap.size()).isGreaterThanOrEqualTo(allVarSet.size());
            assertThat(cMap.getVars()).containsAll(allVarSet);
        }

    }
}
