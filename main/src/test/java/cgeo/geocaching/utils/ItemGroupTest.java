package cgeo.geocaching.utils;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ItemGroupTest {

    private static final Function<String, String> GROUPER = str -> {
      final int idx = str == null ? -1 : str.lastIndexOf("-");
      return idx > 0 ? str.substring(0, idx) : null;
    };

    private static final Comparator<Object> COMP = (o1, o2) -> Objects.toString(o1).compareTo(Objects.toString(o2));

    @Test
    public void simple() {
        //Original Index of test data:          0       1      2        3        4         5       6        7
        final List<String> data = Arrays.asList("blue", "red", "green", "x-red", "yellow", "gray", "brown", "x-pink");
        createAndAssert(data, GROUPER, GROUPER, COMP, ig -> ig.getSize() >= 2,
                "<empty>:[blue, brown, gray, green, red, x:[x-pink, x-red], yellow]");
    }

    @Test
    public void nogroups() {
        final List<String> data = Arrays.asList("blue", "red", "green");
        createAndAssert(data, null, null, null, null,
                "<empty>:[blue, red, green]");
    }

    @Test
    public void multi() {
        //Original Index of test data:          0       1      2        3        4         5       6        7
        final List<String> data = Arrays.asList("color-dark-red", "color-dark-blue", "color-light-red", "color-light-blue", "form-roundy-circle", "form-roundy-oval", "form-edgy-square");
        createAndAssert(data, GROUPER, GROUPER, COMP, ig -> ig.getSize() >= 2,
"<empty>:[color:[color-dark:[color-dark-blue, color-dark-red], color-light:[color-light-blue, color-light-red]], form:[form-edgy-square, form-roundy:[form-roundy-circle, form-roundy-oval]]]");
    }

    private static <T, G> ItemGroup<T, G> createAndAssert(final Iterable<T> items,
                                                          @Nullable final Function<T, G> groupMapper,
                                                          @Nullable final Function<G, G> groupGroupMapper,
                                                          @Nullable final Comparator<Object> itemOrder,
                                                          @Nullable final Predicate<ItemGroup<T, G>> pruner,
                                                          @Nullable final String expectedString) {
        final ItemGroup<T, G> result = ItemGroup.create(items, groupMapper, groupGroupMapper, itemOrder, pruner);

        if (expectedString != null) {
            assertThat(result.toString()).isEqualTo(expectedString);
        }

        final String desc = "{result: " + result + "}";

        //base consistency checks
        assertThat(result).isNotNull();
        assertThat(result.getParent()).isNull();
        assertThat(result.getGroup()).isNull();
        final Set<T> foundItems = new HashSet<>();
        assertItemGroup(desc, result, 0, null, new HashSet<>(), foundItems);
        assertThat(foundItems).containsExactlyInAnyOrderElementsOf(items);
        return result;
    }

    private static <T, G> void assertItemGroup(final String desc, final ItemGroup<T, G> item, final int level, final ItemGroup<T, G> parent, final Set<G> foundGroups, final Set<T> foundItems) {

        final String itemDesc = desc + "{group: " + item.getGroup() + "} ";
        assertThat(item.getItemsAndGroups()).as(itemDesc + "items").containsAll(item.getItems());
        assertThat(item.getItemsAndGroups()).as(itemDesc + "groups").containsAll(item.getGroups());
        assertThat(item.getItemsAndGroups().size()).as(itemDesc + "size").isEqualTo(item.getItems().size() + item.getGroups().size());
        assertThat(item.getParent()).isSameAs(parent);

        assertThat(item.getLevel()).as(itemDesc + "level").isEqualTo(level);

        assertThat(foundGroups).as(itemDesc + "duplicateGroup").doesNotContain(item.getGroup());
        foundGroups.add(item.getGroup());

        if (!item.getItems().isEmpty()) {
            assertThat(foundItems).as(itemDesc + "duplicateItems").doesNotContainAnyElementsOf(item.getItems());
            foundItems.addAll(item.getItems());
        }

        for (ItemGroup<T, G> child : item.getGroups()) {
            assertItemGroup(desc, child, level + 1, item, foundGroups, foundItems);
        }
    }
}
