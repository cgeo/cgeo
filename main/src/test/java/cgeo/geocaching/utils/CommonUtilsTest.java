package cgeo.geocaching.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.offset;

public class CommonUtilsTest {

    private String testField = "";

    @Test
    public void nullComparator() {
        final List<String> someList = new ArrayList<>(Arrays.asList("one", null, "two", "three"));
        someList.sort(CommonUtils.getNullHandlingComparator((s1, s2) -> -s1.compareTo(s2), true)); //sort backwards with null as first element
        assertThat(someList).as("List: " + someList).containsExactly(null, "two", "three", "one");
    }

    @Test
    public void testModulo() {
        assertThat(18 % 8.7).isEqualTo(0.6, offset(0.00001));
        assertThat(-18 % 8.7).isEqualTo(-0.6, offset(0.00001));
    }

    @Test
    public void testListSortingComparator() {
        final List<String> sorterList = Arrays.asList("apple", "bee", "peach");
        final List<String> toSort = new ArrayList<>(Arrays.asList("milk", "peach", "corn", "apple", "bean", "bee"));
        Collections.sort(toSort, CommonUtils.getListSortingComparator(null, true, sorterList));
        assertThat(toSort).as("Actual: " + toSort).containsExactly("apple", "bee", "peach",  "milk", "corn",  "bean");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getReferencedClasses() {
        //A Lambda with back reference should contain the class
        Runnable r = () -> testField = testField + "*";
        final Set<Class<? extends CommonUtilsTest>> set = CommonUtils.getReferencedClasses(r, CommonUtilsTest.class);
        assertThat(set).hasSize(1);
        assertThat(CommonUtils.first(set)).isEqualTo(CommonUtilsTest.class);

        //A Lambda without back reference should NOT contain the class
        r = CommonUtilsTest::staticMethod;
        assertThat(CommonUtils.getReferencedClasses(r, CommonUtilsTest.class)).isEmpty();

        //Searching for a super class
        final Set<Class<? extends Number>> set2 = CommonUtils.getReferencedClasses(new FindReferencesTestClass(), Number.class);
        assertThat(set2).containsExactlyInAnyOrder(Integer.class, Float.class, Number.class); // and does NOT contain Object.class although it is a field

    }

    /** @noinspection EmptyMethod*/
    private static void staticMethod() {
        //do nothing
    }

    @SuppressWarnings("unused") //fields are necessary for unit test, which is based on reflection
    private static class FindReferencesTestClass {

        private Integer int1;
        private Float float1;

        private Float float2;

        private Number num;

        private Object obj1;
    }

}
