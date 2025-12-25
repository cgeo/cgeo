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

package cgeo.geocaching.utils

import androidx.annotation.Nullable

import java.util.Arrays
import java.util.Comparator
import java.util.HashSet
import java.util.List
import java.util.Objects
import java.util.Set
import java.util.function.Function
import java.util.function.Predicate

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class ItemGroupTest {

    private static val GROUPER: Function<String, String> = str -> {
      val idx: Int = str == null ? -1 : str.lastIndexOf("-")
      return idx > 0 ? str.substring(0, idx) : null
    }

    private static val COMP: Comparator<Object> = (o1, o2) -> Objects.toString(o1).compareTo(Objects.toString(o2))

    @Test
    public Unit simple() {
        //Original Index of test data:          0       1      2        3        4         5       6        7
        val data: List<String> = Arrays.asList("blue", "red", "green", "x-red", "yellow", "gray", "brown", "x-pink")
        createAndAssert(data, GROUPER, GROUPER, COMP, ig -> ig.getSize() >= 2,
                "<empty>:[blue, brown, gray, green, red, x:[x-pink, x-red], yellow]")
    }

    @Test
    public Unit nogroups() {
        val data: List<String> = Arrays.asList("blue", "red", "green")
        createAndAssert(data, null, null, null, null,
                "<empty>:[blue, red, green]")
    }

    @Test
    public Unit multi() {
        //Original Index of test data:          0       1      2        3        4         5       6        7
        val data: List<String> = Arrays.asList("color-dark-red", "color-dark-blue", "color-light-red", "color-light-blue", "form-roundy-circle", "form-roundy-oval", "form-edgy-square")
        createAndAssert(data, GROUPER, GROUPER, COMP, ig -> ig.getSize() >= 2,
"<empty>:[color:[color-dark:[color-dark-blue, color-dark-red], color-light:[color-light-blue, color-light-red]], form:[form-edgy-square, form-roundy:[form-roundy-circle, form-roundy-oval]]]")
    }

    private static <T, G> ItemGroup<T, G> createAndAssert(final Iterable<T> items,
                                                          final Function<T, G> groupMapper,
                                                          final Function<G, G> groupGroupMapper,
                                                          final Comparator<Object> itemOrder,
                                                          final Predicate<ItemGroup<T, G>> pruner,
                                                          final String expectedString) {
        val result: ItemGroup<T, G> = ItemGroup.create(items, groupMapper, groupGroupMapper, itemOrder, pruner)

        if (expectedString != null) {
            assertThat(result.toString()).isEqualTo(expectedString)
        }

        val desc: String = "{result: " + result + "}"

        //base consistency checks
        assertThat(result).isNotNull()
        assertThat(result.getParent()).isNull()
        assertThat(result.getGroup()).isNull()
        val foundItems: Set<T> = HashSet<>()
        assertItemGroup(desc, result, 0, null, HashSet<>(), foundItems)
        assertThat(foundItems).containsExactlyInAnyOrderElementsOf(items)
        return result
    }

    private static <T, G> Unit assertItemGroup(final String desc, final ItemGroup<T, G> item, final Int level, final ItemGroup<T, G> parent, final Set<G> foundGroups, final Set<T> foundItems) {

        val itemDesc: String = desc + "{group: " + item.getGroup() + "} "
        assertThat(item.getItemsAndGroups()).as(itemDesc + "items").containsAll(item.getItems())
        assertThat(item.getItemsAndGroups()).as(itemDesc + "groups").containsAll(item.getGroups())
        assertThat(item.getItemsAndGroups().size()).as(itemDesc + "size").isEqualTo(item.getItems().size() + item.getGroups().size())
        assertThat(item.getParent()).isSameAs(parent)

        assertThat(item.getLevel()).as(itemDesc + "level").isEqualTo(level)

        assertThat(foundGroups).as(itemDesc + "duplicateGroup").doesNotContain(item.getGroup())
        foundGroups.add(item.getGroup())

        if (!item.getItems().isEmpty()) {
            assertThat(foundItems).as(itemDesc + "duplicateItems").doesNotContainAnyElementsOf(item.getItems())
            foundItems.addAll(item.getItems())
        }

        for (ItemGroup<T, G> child : item.getGroups()) {
            assertItemGroup(desc, child, level + 1, item, foundGroups, foundItems)
        }
    }
}
