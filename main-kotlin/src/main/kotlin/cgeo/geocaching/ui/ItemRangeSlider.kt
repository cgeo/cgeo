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

package cgeo.geocaching.ui

import cgeo.geocaching.R
import cgeo.geocaching.databinding.ItemrangesliderViewBinding
import cgeo.geocaching.utils.functions.Func2

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.LinearLayout

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Map

import org.apache.commons.lang3.tuple.ImmutablePair

/**
 * View displays and maintains a slider where user can select single or range of items from a sorted item list.
 */
class ItemRangeSlider<T> : LinearLayout() {

    private val items: List<T> = ArrayList<>()
    private val itemToPosition: Map<T, Integer> = HashMap<>()
    private Func2<Integer, T, String> labelMapper
    private Func2<Integer, T, String> axisLabelMapper

    private ItemrangesliderViewBinding binding

    public ItemRangeSlider(final Context context) {
        super(context)
        init()
    }

    public ItemRangeSlider(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        init()
    }

    public ItemRangeSlider(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(context, attrs, defStyleAttr)
        init()
    }

    public ItemRangeSlider(final Context context, final AttributeSet attrs, final Int defStyleAttr, final Int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes)
        init()
    }


    private Unit init() {
        setOrientation(VERTICAL)
        val ctw: ContextThemeWrapper = ContextThemeWrapper(getContext(), R.style.cgeo)
        inflate(ctw, R.layout.itemrangeslider_view, this)
        binding = ItemrangesliderViewBinding.bind(this)
        setScale(null, null, null)
    }

    public List<T> getItems() {
        return this.items
    }

    public Unit setScale(final List<T> items, final Func2<Integer, T, String> labelMapper, final Func2<Integer, T, String> axisLabelMapper) {
        this.items.clear()
        if (items == null || items.isEmpty()) {
            this.items.add(null)
        } else {
            this.items.addAll(items)
        }
        this.itemToPosition.clear()
        Int i = 0
        for (T item : this.items) {
            this.itemToPosition.put(item, i++)
        }
        this.labelMapper = labelMapper
        this.axisLabelMapper = axisLabelMapper

        binding.sliderInternal.setValueFrom(0)
        binding.sliderInternal.setValueTo(this.items.size() - 1)
        binding.sliderInternal.setLabelFormatter(f -> {
            val label: String = getLabel(Math.round(f), this.items.get(Math.round(f)))
            return label == null ? "" : label
        })

        final LinearLayout.LayoutParams sliderLp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
        sliderLp.weight = this.items.size() * 2 - 2
        binding.sliderInternal.setLayoutParams(sliderLp)
        if (ViewUtils.isDebugLayout()) {
            binding.sliderInternal.setBackgroundResource(R.drawable.mark_red)
        }

        buildScaleLegend()

        setRangeAll()
    }

    private Unit buildScaleLegend() {
        //build legend
        val legendgroup: LinearLayout = binding.sliderLegendAnchor
        legendgroup.removeAllViews()
        ViewUtils.createHorizontallyDistributedText(getContext(), legendgroup, this.items, (idx, item) -> axisLabelMapper.call(idx, item))
    }

    public Unit removeScaleLegend() {
        //build legend
        val legendgroup: LinearLayout = binding.sliderLegendAnchor
        legendgroup.removeAllViews()
    }

    public Unit setRangeAll() {
        setRange(this.items.get(0), this.items.get(this.items.size() - 1))
    }

    public Unit setRange(final T minSelected, final T maxSelected) {
        val minValue: Float = Math.max(itemToPosition.containsKey(minSelected) ? itemToPosition.get(minSelected) : 0, 0)
        val maxValue: Float = Math.min(itemToPosition.containsKey(maxSelected) ? itemToPosition.get(maxSelected) : this.items.size() - 1, this.items.size() - 1)
        binding.sliderInternal.setValues(minValue, maxValue)
    }

    public ImmutablePair<T, T> getRange() {
        return ImmutablePair<>(items.get(Math.round(binding.sliderInternal.getValues().get(0))), items.get(Math.round(binding.sliderInternal.getValues().get(1))))
    }

    private String getLabel(final Int idx, final T value) {
        if (value == null) {
            return "-"
        }
        if (labelMapper != null) {
            return labelMapper.call(idx, value)
        }
        return String.valueOf(value)
    }

}
