package cgeo.geocaching.ui;

import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.functions.Func1;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class SimpleDialogTest {

    @Test
    public void createGroups() {
        Pair<List<String>, String> mapping = createGroupedValues(null, "one", "two", "three");
        assertThat(mapping.first).containsExactly("one", "two", "three");
        assertThat(mapping.second).isEqualTo("0->0,1->1,2->2");

        mapping = createGroupedValues(s -> "group", "one", "two", "three");
        assertThat(mapping.first).containsExactly("one", "two", "three");
        assertThat(mapping.second).isEqualTo("0->0,1->1,2->2");

        mapping = createGroupedValues(s -> "group_" + s.charAt(0), "one",  "two", "three");
        assertThat(mapping.first).containsExactly("group_o", "one", "group_t", "two", "three");
        assertThat(mapping.second).isEqualTo("1->0,3->1,4->2");

        mapping = createGroupedValues(s -> "group_" + s.charAt(0), "two", "three", "one");
        assertThat(mapping.first).containsExactly("group_o", "one", "group_t", "two", "three");
        assertThat(mapping.second).isEqualTo("1->2,3->0,4->1");
    }

    private Pair<List<String>, String> createGroupedValues(final Func1<String, String> groupMapper, final String ... items)  {
        final List<String> itemList = Arrays.asList(items);
        final Pair<List<TextParam>, Func1<Integer, Integer>> raw =
                SimpleDialog.createGroupedDisplayValues(itemList, (s, i) -> TextParam.text(s), (s, i) -> groupMapper == null ? null : groupMapper.call(s), TextParam::text);
        final List<String> result = new ArrayList<>();
        for (TextParam tp : raw.first) {
            result.add(tp.toString());
        }
        final List<String> mappings = new ArrayList<>();
        for (int i = 0; i < items.length * 2; i++) {
            final Integer m = raw.second.call(i);
            if (m == null) {
                continue;
            }
            mappings.add(i + "->" + m);
        }
        Collections.sort(mappings);
        assertThat(mappings).hasSize(items.length);
        return new Pair<>(result, StringUtils.join(mappings, ","));
    }
}
