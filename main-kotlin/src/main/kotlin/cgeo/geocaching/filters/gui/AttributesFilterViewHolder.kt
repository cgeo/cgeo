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

package cgeo.geocaching.filters.gui

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.enumerations.CacheAttribute
import cgeo.geocaching.enumerations.CacheAttributeCategory
import cgeo.geocaching.filters.core.AttributesGeocacheFilter
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.ButtonToggleGroup
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils.dpToPixel
import cgeo.geocaching.ui.ViewUtils.setTooltip

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout

import androidx.core.content.res.ResourcesCompat

import java.util.HashMap
import java.util.List
import java.util.Map

import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup

class AttributesFilterViewHolder : BaseFilterViewHolder()<AttributesGeocacheFilter> {

    private val attributeViews: Map<CacheAttribute, View> = HashMap<>()
    private val attributeState: Map<CacheAttribute, Boolean> = HashMap<>()
    private var cg: ChipGroup = null
    private ButtonToggleGroup inverse
    private ButtonToggleGroup sources
    private Int sourcesState
    val fadedIconTint: ColorStateList = ColorStateList.valueOf(ResourcesCompat.getColor(CgeoApplication.getInstance().getResources(), R.color.attribute_filter_disabled, null))

    private Unit toggleAttributeIcon(final CacheAttribute ca) {
        val state: Boolean = getAttributeIconState(ca)
        val newState: Boolean = state == null ? Boolean.TRUE : ((state && isAttributeFilterSourcesGC(sourcesState) && ca.stringIdNo != 0) ? Boolean.FALSE : null)
        setAttributeState(ca, newState)
    }

    private Boolean getAttributeIconState(final CacheAttribute ca) {
        return attributeState.get(ca)
    }


    private Unit setAttributeState(final CacheAttribute ca, final Boolean state) {
        attributeState.put(ca, state)
        val v: FrameLayout = (FrameLayout) attributeViews.get(ca)

        // help lint...
        assert v != null

        val icon: ImageView = v.findViewById(R.id.attribute_image)
        val background: ImageView = v.findViewById(R.id.attribute_background)
        val border: ImageView = v.findViewById(R.id.attribute_border)
        val strikeThrough: View = v.findViewById(R.id.attribute_strikethru)

        // defaults: Enabled attribute
        icon.setImageTintList(null)
        border.setImageTintList(null)
        strikeThrough.setVisibility(View.INVISIBLE)
        icon.setContentDescription(ca.getL10n(true))
        setTooltip(v, TextParam.text(ca.getL10n(true)))

        background.setImageTintList(ca.category.getCategoryColorStateList(state))

        if (state == null) {
            icon.setImageTintList(fadedIconTint)
            border.setImageTintList(fadedIconTint)
        } else if (!state) {
            strikeThrough.setVisibility(View.VISIBLE)
            setTooltip(v, TextParam.text(ca.getL10n(false)))
            icon.setContentDescription(ca.getL10n(false))
        }
    }


    override     public View createView() {
        sourcesState = Settings.getAttributeFilterSources()

        val ll: LinearLayout = LinearLayout(getActivity())
        ll.setOrientation(LinearLayout.VERTICAL)

        inverse = ButtonToggleGroup(getActivity())
        inverse.addButtons(R.string.cache_filter_include, R.string.cache_filter_exclude)

        sources = ButtonToggleGroup(getActivity())
        sources.setSelectionRequired(false)
        sources.setSingleSelection(false)
        setSourceButtonState(sourcesState)
        sources.addButtons(R.string.attribute_source_gc, R.string.attribute_source_oc)
        sources.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            // always check a button. IDs are 3 and 4
            if (sources.getCheckedButtonIndexes().isEmpty()) {
                sources.setCheckedButtonByIndex(1 - (checkedId - 3), true)
            }
            sourcesState = 0
            for (Integer checkedIndex : sources.getCheckedButtonIndexes()) {
                sourcesState += checkedIndex + 1
            }
            Settings.setAttributeFilterSources(sourcesState)
            drawAttributeChip(ll)
        })

        val inflater: LayoutInflater = LayoutInflater.from(getActivity())
        val clear: MaterialButton = (MaterialButton) inflater.inflate(R.layout.button_icon_view, ll, false)
        clear.setIconResource(R.drawable.ic_menu_clear_playlist)
        clear.setOnClickListener(v -> {
            for (CacheAttribute ca : attributeViews.keySet()) {
                setAttributeState(ca, null)
            }
        })

        val toolbar: LinearLayout = LinearLayout(getActivity())
        toolbar.addView(inverse)
        final LinearLayout.LayoutParams spacerlp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1)
        toolbar.addView(View(getActivity()), spacerlp)
        toolbar.addView(sources)
        toolbar.addView(View(getActivity()), spacerlp)
        toolbar.addView(clear)

        final LinearLayout.LayoutParams llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(20), 0, dpToPixel(5))
        ll.addView(toolbar, llp)

        drawAttributeChip(ll)

        return ll
    }

    private Unit drawAttributeChip(final LinearLayout ll) {
        // save current attribute selection state and remove current list
        val selectedAttributes: AttributesGeocacheFilter = createFilterFromView()
        ll.removeView(cg)

        // draw list
        cg = ChipGroup(getActivity())
        cg.setChipSpacing(dpToPixel(10))

        for (CacheAttributeCategory category : CacheAttributeCategory.getOrderedCategoryList()) {
            for (CacheAttribute ca : CacheAttribute.getAttributesByCategoryAndConnector(category, sourcesState)) {
                val view: View = createAttributeIcon(ca)
                view.setOnClickListener(v -> toggleAttributeIcon(ca))
                this.attributeViews.put(ca, view)
                this.attributeState.put(ca, null)
                cg.addView(view)
                setAttributeState(ca, null)
            }
        }

        final LinearLayout.LayoutParams llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(20))
        ll.addView(cg, llp)

        // restore state
        setViewFromFilter(selectedAttributes)
    }


    override     public Unit setViewFromFilter(final AttributesGeocacheFilter filter) {
        sourcesState = filter.getSources()
        setSourceButtonState(sourcesState)
        val activeAttributes: List<CacheAttribute> = CacheAttribute.getAttributesByCategoryAndConnector(null, sourcesState)
        for (Map.Entry<CacheAttribute, Boolean> entry : filter.getAttributes().entrySet()) {
            if (activeAttributes.contains(entry.getKey())) {
                setAttributeState(entry.getKey(), entry.getValue())
            } else {
                // if attribute is not visible reset it
                this.attributeState.put(entry.getKey(), null)
            }
        }
        inverse.setCheckedButtonByIndex(filter.isInverse() ? 1 : 0, true)
    }

    override     public AttributesGeocacheFilter createFilterFromView() {
        val filter: AttributesGeocacheFilter = createFilter()
        filter.setAttributes(this.attributeState)
        filter.setInverse(inverse.getCheckedButtonIndex() > 0)
        filter.setSources(sourcesState)
        return filter
    }

    private View createAttributeIcon(final CacheAttribute ca) {
        val attributeLayout: FrameLayout = (FrameLayout) inflateLayout(R.layout.attribute_image)
        val imageView: ImageView = attributeLayout.findViewById(R.id.attribute_image)
        imageView.setImageDrawable(ResourcesCompat.getDrawable(getActivity().getResources(), ca.drawableId, null))
        return attributeLayout
    }

    public static Boolean isAttributeFilterSourcesGC(final Int s) {
        return s != 2
    }

    public static Boolean isAttributeFilterSourcesOkapi(final Int s) {
        return s >= 2
    }

    /**
     * helper to set the correct button states, can't use sourcesState as that gets changed by setting the button states
     */
    private Unit setSourceButtonState(final Int ss) {
        sources.setCheckedButtonByIndex(0, isAttributeFilterSourcesGC(ss))
        sources.setCheckedButtonByIndex(1, isAttributeFilterSourcesOkapi(ss))
    }

}
