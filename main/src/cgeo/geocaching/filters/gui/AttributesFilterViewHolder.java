package cgeo.geocaching.filters.gui;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheAttributeCategory;
import cgeo.geocaching.filters.core.AttributesGeocacheFilter;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ButtonToggleGroup;
import cgeo.geocaching.ui.TextParam;
import static cgeo.geocaching.ui.ViewUtils.dpToPixel;
import static cgeo.geocaching.ui.ViewUtils.setTooltip;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.res.ResourcesCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

public class AttributesFilterViewHolder extends BaseFilterViewHolder<AttributesGeocacheFilter> {

    private final Map<CacheAttribute, View> attributeViews = new HashMap<>();
    private final Map<CacheAttribute, Boolean> attributeState = new HashMap<>();
    private ChipGroup cg = null;
    private ButtonToggleGroup inverse;
    private ButtonToggleGroup sources;
    private int sourcesState;
    final ColorStateList fadedIconTint = ColorStateList.valueOf(ResourcesCompat.getColor(CgeoApplication.getInstance().getResources(), R.color.attribute_filter_disabled, null));

    private void toggleAttributeIcon(final CacheAttribute ca) {
        final Boolean state = getAttributeIconState(ca);
        final Boolean newState = state == null ? Boolean.TRUE : (state ? Boolean.FALSE : null);
        setAttributeState(ca, newState);
    }

    private Boolean getAttributeIconState(final CacheAttribute ca) {
        return attributeState.get(ca);
    }


    private void setAttributeState(final CacheAttribute ca, final Boolean state) {
        attributeState.put(ca, state);
        final FrameLayout v = (FrameLayout) attributeViews.get(ca);

        // help lint...
        assert v != null;

        final ImageView icon = v.findViewById(R.id.attribute_image);
        final ImageView background = v.findViewById(R.id.attribute_background);
        final ImageView border = v.findViewById(R.id.attribute_border);
        final View strikeThrough = v.findViewById(R.id.attribute_strikethru);

        // defaults: Enabled attribute
        icon.setImageTintList(null);
        border.setImageTintList(null);
        strikeThrough.setVisibility(View.INVISIBLE);
        icon.setContentDescription(ca.getL10n(true));
        setTooltip(v, TextParam.text(ca.getL10n(true)));

        background.setImageTintList(ca.category.getCategoryColorStateList(state));

        if (state == null) {
            icon.setImageTintList(fadedIconTint);
            border.setImageTintList(fadedIconTint);
        } else if (!state) {
            strikeThrough.setVisibility(View.VISIBLE);
            setTooltip(v, TextParam.text(ca.getL10n(false)));
            icon.setContentDescription(ca.getL10n(false));
        }
    }


    @Override
    public View createView() {
        sourcesState = Settings.getAttributeFilterSources();

        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        inverse = new ButtonToggleGroup(getActivity());
        inverse.addButtons(R.string.cache_filter_include, R.string.cache_filter_exclude);

        sources = new ButtonToggleGroup(getActivity());
        sources.setSelectionRequired(false);
        sources.setSingleSelection(false);
        setSourceButtonState(sourcesState);
        sources.addButtons(R.string.attribute_source_gc, R.string.attribute_source_oc);
        sources.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            // always check a button. IDs are 3 and 4
            if (sources.getCheckedButtonIndexes().size() == 0) {
                sources.setCheckedButtonByIndex(1 - (checkedId - 3), true);
            }
            sourcesState = 0;
            for (Integer checkedIndex : sources.getCheckedButtonIndexes()) {
                sourcesState += checkedIndex + 1;
            }
            Settings.setAttributeFilterSources(sourcesState);
            drawAttributeChip(ll);
        });

        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        final MaterialButton clear = (MaterialButton) inflater.inflate(R.layout.button_icon_view, ll, false);
        clear.setIconResource(R.drawable.ic_menu_clear_playlist);
        clear.setOnClickListener(v -> {
            for (CacheAttribute ca : attributeViews.keySet()) {
                setAttributeState(ca, null);
            }
        });

        final LinearLayout toolbar = new LinearLayout(getActivity());
        toolbar.addView(inverse);
        final LinearLayout.LayoutParams spacerlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        toolbar.addView(new View(getActivity()), spacerlp);
        toolbar.addView(sources);
        toolbar.addView(new View(getActivity()), spacerlp);
        toolbar.addView(clear);

        final LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(20), 0, dpToPixel(5));
        ll.addView(toolbar, llp);

        drawAttributeChip(ll);

        return ll;
    }

    private void drawAttributeChip(final LinearLayout ll) {
        // save current attribute selection state and remove current list
        final AttributesGeocacheFilter selectedAttributes = createFilterFromView();
        ll.removeView(cg);

        // draw new list
        cg = new ChipGroup(getActivity());
        cg.setChipSpacing(dpToPixel(10));

        for (CacheAttributeCategory category : CacheAttributeCategory.getOrderedCategoryList()) {
            for (CacheAttribute ca : CacheAttribute.getAttributesByCategoryAndConnector(category, sourcesState)) {
                final View view = createAttributeIcon(ca);
                view.setOnClickListener(v -> toggleAttributeIcon(ca));
                this.attributeViews.put(ca, view);
                this.attributeState.put(ca, null);
                cg.addView(view);
                setAttributeState(ca, null);
            }
        }

        final LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(20));
        ll.addView(cg, llp);

        // restore state
        setViewFromFilter(selectedAttributes);
    }


    @Override
    public void setViewFromFilter(final AttributesGeocacheFilter filter) {
        sourcesState = filter.getSources();
        setSourceButtonState(sourcesState);
        final List<CacheAttribute> activeAttributes = CacheAttribute.getAttributesByCategoryAndConnector(null, sourcesState);
        for (Map.Entry<CacheAttribute, Boolean> entry : filter.getAttributes().entrySet()) {
            if (activeAttributes.contains(entry.getKey())) {
                setAttributeState(entry.getKey(), entry.getValue());
            } else {
                // if attribute is not visible reset it
                this.attributeState.put(entry.getKey(), null);
            }
        }
        inverse.setCheckedButtonByIndex(filter.isInverse() ? 1 : 0, true);
    }

    @Override
    public AttributesGeocacheFilter createFilterFromView() {
        final AttributesGeocacheFilter filter = createFilter();
        filter.setAttributes(this.attributeState);
        filter.setInverse(inverse.getCheckedButtonIndex() > 0);
        filter.setSources(sourcesState);
        return filter;
    }

    private View createAttributeIcon(final CacheAttribute ca) {
        final FrameLayout attributeLayout = (FrameLayout) inflateLayout(R.layout.attribute_image);
        final ImageView imageView = attributeLayout.findViewById(R.id.attribute_image);
        imageView.setImageDrawable(ResourcesCompat.getDrawable(getActivity().getResources(), ca.drawableId, null));
        return attributeLayout;
    }

    public static boolean isAttributeFilterSourcesGC(final int s) {
        return s != 2;
    }

    public static boolean isAttributeFilterSourcesOkapi(final int s) {
        return s >= 2;
    }

    /**
     * helper to set the correct button states, can't use sourcesState as that gets changed by setting the button states
     */
    private void setSourceButtonState(final int ss) {
        sources.setCheckedButtonByIndex(0, isAttributeFilterSourcesGC(ss));
        sources.setCheckedButtonByIndex(1, isAttributeFilterSourcesOkapi(ss));
    }

}
