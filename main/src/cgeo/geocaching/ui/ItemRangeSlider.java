package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.ItemrangesliderViewBinding;
import cgeo.geocaching.utils.functions.Func2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * View displays and maintains a slider where user can select single or range of items from a sorted item list.
 */
public class ItemRangeSlider<T> extends LinearLayout {

    private final List<T> items = new ArrayList<>();
    private final Map<T, Integer> itemToPosition = new HashMap<>();
    private Func2<Integer, T, String> labelMapper;
    private Func2<Integer, T, String> axisLabelMapper;

    private ItemrangesliderViewBinding binding;

    public ItemRangeSlider(final Context context) {
        super(context);
        init();
    }

    public ItemRangeSlider(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ItemRangeSlider(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ItemRangeSlider(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    private void init() {
        setOrientation(VERTICAL);
        final ContextThemeWrapper ctw = new ContextThemeWrapper(getContext(), R.style.cgeo);
        inflate(ctw, R.layout.itemrangeslider_view, this);
        binding = ItemrangesliderViewBinding.bind(this);
        setScale(null, null, null);
    }

    @NonNull
    public List<T> getItems() {
        return this.items;
    }

    public void setScale(final List<T> items, final Func2<Integer, T, String> labelMapper, final Func2<Integer, T, String> axisLabelMapper) {
        this.items.clear();
        if (items == null || items.isEmpty()) {
            this.items.add(null);
        } else {
            this.items.addAll(items);
        }
        this.itemToPosition.clear();
        int i = 0;
        for (T item : this.items) {
            this.itemToPosition.put(item, i++);
        }
        this.labelMapper = labelMapper;
        this.axisLabelMapper = axisLabelMapper;

        binding.sliderInternal.setValueFrom(0);
        binding.sliderInternal.setValueTo(this.items.size() - 1);
        binding.sliderInternal.setLabelFormatter(f -> {
            final String label = getLabel(Math.round(f), this.items.get(Math.round(f)));
            return label == null ? "" : label;
        });

        final LinearLayout.LayoutParams sliderLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        sliderLp.weight = this.items.size() * 2 - 2;
        binding.sliderInternal.setLayoutParams(sliderLp);
        if (ViewUtils.isDebugLayout()) {
            binding.sliderInternal.setBackgroundResource(R.drawable.mark_red);
        }

        buildScaleLegend();

        setRangeAll();
    }

    private void buildScaleLegend() {
        //build legend
        final LinearLayout legendgroup = binding.sliderLegendAnchor;
        legendgroup.removeAllViews();
        ViewUtils.createHorizontallyDistributedText(getContext(), legendgroup, this.items, (idx, item) -> axisLabelMapper.call(idx, item));
    }

    public void removeScaleLegend() {
        //build legend
        final LinearLayout legendgroup = binding.sliderLegendAnchor;
        legendgroup.removeAllViews();
    }

    public void setRangeAll() {
        setRange(this.items.get(0), this.items.get(this.items.size() - 1));
    }

    public void setRange(final T minSelected, final T maxSelected) {
        final float minValue = Math.max(itemToPosition.containsKey(minSelected) ? itemToPosition.get(minSelected) : 0, 0);
        final float maxValue = Math.min(itemToPosition.containsKey(maxSelected) ? itemToPosition.get(maxSelected) : this.items.size() - 1, this.items.size() - 1);
        binding.sliderInternal.setValues(minValue, maxValue);
    }

    public ImmutablePair<T, T> getRange() {
        return new ImmutablePair<>(items.get(Math.round(binding.sliderInternal.getValues().get(0))), items.get(Math.round(binding.sliderInternal.getValues().get(1))));
    }

    private String getLabel(final int idx, final T value) {
        if (value == null) {
            return "-";
        }
        if (labelMapper != null) {
            return labelMapper.call(idx, value);
        }
        return String.valueOf(value);
    }

}
