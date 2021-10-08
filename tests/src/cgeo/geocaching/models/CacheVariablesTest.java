package cgeo.geocaching.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CacheVariablesTest {

    private CacheVariables createTestInstance() {
        return new CacheVariables("GCFAKE1", false);
    }

    private CacheVariables createInstanceWithStandardFill() {
        final CacheVariables cv = createTestInstance();
        cv.addVariable("a", "1", 5);
        cv.addVariable("b", "a", 5);
        cv.addVariable("c", "b", 5);
        assertCacheVariables(cv, "a", "f:1", "b", "f:a", "c", "f:b");
        return cv;
    }

    @Test
    public void simpleAdd() {
        final CacheVariables cv = createTestInstance();
        assertCacheVariables(cv);
        cv.addVariable("a", "2", 0);
        assertCacheVariables(cv, "a", "f:2");
        cv.addVariable("b", "3", 0);
        assertCacheVariables(cv, "b", "f:3", "a", "f:2");
        cv.addVariable("c", "3", 5);
        assertCacheVariables(cv, "b", "f:3", "a", "f:2", "c", "f:3");
    }

    @Test
    public void simpleRemove() {
        final CacheVariables cv = createInstanceWithStandardFill();
        assertCacheVariables(cv, "a", "f:1", "b", "f:a", "c", "f:b");

        cv.removeVariable("d"); //nonexisting
        assertCacheVariables(cv, "a", "f:1", "b", "f:a", "c", "f:b");

        cv.removeVariable("a");
        assertCacheVariables(cv, "b", "f:a", "c", "f:b", "m:a");
    }

    @Test
    public void simpleChange() {
        final CacheVariables cv = createInstanceWithStandardFill();
        assertCacheVariables(cv, "a", "f:1", "b", "f:a", "c", "f:b");

        cv.changeVariable("d", "5"); //nonexisting
        assertCacheVariables(cv, "a", "f:1", "b", "f:a", "c", "f:b");

        cv.changeVariable("b", "5"); //nonexisting
        assertCacheVariables(cv, "a", "f:1", "b", "f:5", "c", "f:b");
    }

    @Test
    public void addExisting() {
        final CacheVariables cv = createInstanceWithStandardFill();
        assertCacheVariables(cv, "a", "f:1", "b", "f:a", "c", "f:b");

        cv.addVariable("b", "5", 0); //add BEFORE old var
        assertCacheVariables(cv, "b", "f:5", "a", "f:1", "c", "f:b");

        cv.addVariable("b", "6", 5); ///add AFTER old var
        assertCacheVariables(cv, "a", "f:1", "c", "f:b", "b", "f:6");
    }

    @Test
    public void addUnknown() {
        final CacheVariables cv = createInstanceWithStandardFill();
        assertCacheVariables(cv, "a", "f:1", "b", "f:a", "c", "f:b");

        final String name = cv.addVariable(null, "5", 1); //add BEFORE old var
        assertThat(name).isEqualTo("_1");
        assertCacheVariables(cv, "a", "f:1", "_1", "f:5",  "b", "f:a", "c", "f:b");
    }

    @Test
    public void sort() {
        final CacheVariables cv = createInstanceWithStandardFill();
        assertCacheVariables(cv, "a", "f:1", "b", "f:a", "c", "f:b");

        cv.sortVariables((s1, s2) -> -s1.compareTo(s2)); //sort backwards
        assertCacheVariables(cv, "c", "f:b", "b", "f:a",  "a", "f:1");
    }





    /** expectedParams syntax: pure string = expected var, prefix with f: to assert formula, prefix with m: to assert missing vars */
    private void assertCacheVariables(final CacheVariables cv, final String ... expectedParams) {
        final List<String> vars = new ArrayList<>();
        final List<String> missingVars = new ArrayList<>();
        String currentVar = null;
        for (String p : expectedParams) {
            if (p.startsWith("f:")) {
                assertThat(Objects.requireNonNull(cv.getState(currentVar)).getFormula()).as("Formula for var '" + currentVar + "'").isEqualTo(p.substring(2));
            } else if (p.startsWith("m:")) {
                missingVars.add(p.substring(2));
            } else {
                currentVar = p;
                vars.add(p);
            }
        }
        assertThat(cv.getVariableList()).containsExactlyElementsOf(vars);
        assertThat(cv.getVariableSet()).containsExactlyInAnyOrderElementsOf(vars);
        assertThat(cv.getAllMissingVars()).containsExactlyInAnyOrderElementsOf(missingVars);
    }
}
