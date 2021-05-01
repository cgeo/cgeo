package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.filters.core.AttributesGeocacheFilter;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.res.ResourcesCompat;

import java.util.HashMap;
import java.util.Map;

import com.google.android.material.chip.ChipGroup;

public class AttributesFilterViewHolder extends BaseFilterViewHolder<AttributesGeocacheFilter> {

    private final Map<CacheAttribute, View> attributeViews = new HashMap<>();
    private final Map<CacheAttribute, Boolean> attributeState = new HashMap<>();


    private void toggleAttributeIcon(final CacheAttribute ca) {
        final Boolean state = getAttributeIconState(ca);
        final Boolean newState = state == null ? Boolean.TRUE : (state ? Boolean.FALSE : null);
        setAttributeIcon(ca, newState);
    }

    private Boolean getAttributeIconState(final CacheAttribute ca) {
        return attributeState.get(ca);
    }


    private void setAttributeIcon(final CacheAttribute ca, final Boolean state) {
        attributeState.put(ca, state);
        final FrameLayout v = (FrameLayout) attributeViews.get(ca);
        final ImageView icon = (ImageView) v.getChildAt(0);
        final View strikeThrough = v.getChildAt(1);

        if (state == null) {
            icon.setColorFilter(Color.argb(150, 200, 200, 200));
            strikeThrough.setVisibility(View.INVISIBLE);
        } else if (state) {
            icon.clearColorFilter();
            strikeThrough.setVisibility(View.INVISIBLE);
        } else {
            icon.clearColorFilter();
            strikeThrough.setVisibility(View.VISIBLE);
        }

    }

     @Override
     public View createView() {

        final ChipGroup cg = new ChipGroup(getActivity());
        cg.setChipSpacing(dpToPixel(10));

        for (CacheAttribute ca: CacheAttribute.values()) {
            final View view = createAttributeIcon(ca);
            view.setOnClickListener(v -> {
                toggleAttributeIcon(ca);
            });
            this.attributeViews.put(ca, view);
            this.attributeState.put(ca, null);
            cg.addView(view);
        }

        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);
        final LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(20), 0, dpToPixel(20));
        ll.addView(cg, llp);

        return ll;
    }

    private int dpToPixel(final float dp)  {
        return (int) (dp * ((float) getActivity().getResources().getDisplayMetrics().density));
    }

    @Override
    public void setViewFromFilter(final AttributesGeocacheFilter filter) {
        for (Map.Entry<CacheAttribute, Boolean> entry : filter.getAttributes().entrySet()) {
            setAttributeIcon(entry.getKey(), entry.getValue());
        }
        filter.setAttributes(this.attributeState);
    }

    @Override
    public AttributesGeocacheFilter createFilterFromView() {
        final AttributesGeocacheFilter filter = createFilter();
        filter.setAttributes(this.attributeState);
        return filter;
    }

    public View createAttributeIcon(final CacheAttribute ca) {
        final FrameLayout attributeLayout  = (FrameLayout) inflateLayout(R.layout.attribute_image);

        final ImageView imageView = (ImageView) attributeLayout.getChildAt(0);

        imageView.setImageDrawable(ResourcesCompat.getDrawable(getActivity().getResources(), ca.drawableId, null));
        imageView.setColorFilter(Color.argb(150, 200, 200, 200));

        // generate invisible strike through image with same properties as attribute image and place as second
        final ImageView strikeThroughImage = new ImageView(getActivity());
        strikeThroughImage.setLayoutParams(imageView.getLayoutParams());
        strikeThroughImage.setImageDrawable(ResourcesCompat.getDrawable(getActivity().getResources(), R.drawable.attribute__strikethru, null));
        strikeThroughImage.setVisibility(View.INVISIBLE);
        attributeLayout.addView(strikeThroughImage);

        return attributeLayout;
    }


}
