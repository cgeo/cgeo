package cgeo.geocaching.ui.recyclerview;

import cgeo.geocaching.utils.TextUtils;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ManagedListAdapterTest {

    public static class TestHolder extends RecyclerView.ViewHolder {

        public TestHolder(@NonNull final View itemView) {
            super(itemView);
        }
    }

    public static class TestManagedListAdapter extends ManagedListAdapter<String, TestHolder> {

        protected TestManagedListAdapter() {
            super(new Config(null).setNotifyOnEvents(false));
        }

        @NonNull
        @Override
        public TestHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            return new TestHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull final TestHolder holder, final int position) {
            //empty on purpose
        }
    }

    @Test
    public void simpleAddRemove() {
        final TestManagedListAdapter mla = new TestManagedListAdapter();
        assertThat(mla.getItems()).isEmpty();
        mla.addItem(0, "test");
        assertThat(mla.getItems().get(0)).isEqualTo("test");
        mla.removeItem(0);
        assertThat(mla.getItems()).isEmpty();
    }

    @Test
    public void filterAddRemove() {
        final TestManagedListAdapter mla = new TestManagedListAdapter();
        mla.setFilter(s -> s.startsWith("g"), true);
        mla.addItem(0, "blue");
        assertThat(mla.getDebugString()).isEqualTo("[]|[blue]|{}");
        mla.addItem(0, "gray");
        assertThat(mla.getDebugString()).isEqualTo("[gray]|[gray, blue]|{0=0}");
        mla.addItem(1, "green");
        assertThat(mla.getDebugString()).isEqualTo("[gray, green]|[gray, green, blue]|{1=1, 0=0}");
        mla.addItem(1, "red");
        assertThat(mla.getDebugString()).isEqualTo("[gray, green]|[gray, red, green, blue]|{1=2, 0=0}");

        assertThat(mla.getItems()).containsExactly("gray", "green");
        assertThat(mla.getOriginalItems()).containsExactly("gray", "red", "green", "blue");

        mla.setFilter(null, true);
        assertThat(mla.getDebugString()).isEqualTo("[gray, red, green, blue]|[gray, red, green, blue]|{3=3, 2=2, 1=1, 0=0}");
        assertThat(mla.getItems()).containsExactly("gray", "red", "green", "blue");
        assertThat(mla.getOriginalItems()).containsExactly("gray", "red", "green", "blue");

        mla.setFilter(s -> s.startsWith("g"), true);
        assertThat(mla.getDebugString()).isEqualTo("[gray, green]|[gray, red, green, blue]|{1=2, 0=0}");

        mla.removeItem(0);
        assertThat(mla.getDebugString()).isEqualTo("[green]|[red, green, blue]|{0=1}");
        assertThat(mla.getItems()).containsExactly("green");
        assertThat(mla.getOriginalItems()).containsExactly("red", "green", "blue");
    }

    @Test
    public void filterAddMultiple() {

        final TestManagedListAdapter mla = new TestManagedListAdapter();
        mla.setFilter(s -> s.startsWith("g"), true);
        mla.addItems(Arrays.asList("red", "gray", "blue", "green"));
        assertThat(mla.getDebugString()).isEqualTo("[gray, green]|[red, gray, blue, green]|{1=3, 0=1}");

        mla.addItems(1, Arrays.asList("yellow", "grape"));
        assertThat(mla.getDebugString()).isEqualTo("[gray, grape, green]|[red, gray, yellow, grape, blue, green]|{2=5, 1=3, 0=1}");

        mla.addItems(Arrays.asList("grump", "brown"));
        assertThat(mla.getDebugString()).isEqualTo("[gray, grape, green, grump]|[red, gray, yellow, grape, blue, green, grump, brown]|{3=6, 2=5, 1=3, 0=1}");
    }

    @Test
    public void filterAddOrder() {
        final TestManagedListAdapter mla = new TestManagedListAdapter();
        mla.setOriginalItemListInsertOrderer(String::compareTo);
        mla.addItems(Arrays.asList("A", "C"));
        mla.setFilter(s -> false);
        assertThat(mla.getDebugString()).isEqualTo("[]|[A, C]|{}");

        //add var M at 0 -> should be BEHIND A and B
        mla.addItem(0, "M");
        assertThat(mla.getDebugString()).isEqualTo("[]|[A, C, M]|{}");

        //make M visible, add var B at pos 0 -> should be sorted inbetween 0 and 1 and result in A,B,C,M
        mla.setFilter("M"::equals);
        mla.addItem(0, "B");
        assertThat(mla.getDebugString()).isEqualTo("[M]|[A, B, C, M]|{0=3}");

        //add var D at end (after visible M) -> should be put BEHIND M (NOT sorted in!)
        mla.addItem(1, "D");
        assertThat(mla.getDebugString()).isEqualTo("[M]|[A, B, C, M, D]|{0=3}");

    }


    @Test
    public void filterSwapSort() {

        final TestManagedListAdapter mla = new TestManagedListAdapter();
        mla.setFilter(s -> s.startsWith("g"), true);
        mla.addItems(Arrays.asList("red", "gray", "blue", "green"));
        assertThat(mla.getDebugString()).isEqualTo("[gray, green]|[red, gray, blue, green]|{1=3, 0=1}");

        mla.swapItems(0, 1);
        assertThat(mla.getDebugString()).isEqualTo("[green, gray]|[red, green, blue, gray]|{1=3, 0=1}");

        mla.sortItems(TextUtils.COLLATOR::compare);
        assertThat(mla.getDebugString()).isEqualTo("[gray, green]|[blue, gray, green, red]|{1=2, 0=1}");
    }
}
