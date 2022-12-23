package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.LogicalGeocacheFilter;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import org.apache.commons.lang3.StringUtils;

public class LogicalFilterViewHolder extends BaseFilterViewHolder<LogicalGeocacheFilter> {

    private LogicalGeocacheFilter filter;
    private TextView textView;

    @Override
    public View createView() {
        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        final LayoutInflater inflater = LayoutInflater.from(getActivity());

        final View infoLine = inflater.inflate(R.layout.checkbox_item, ll, false);
        ((ImageView) infoLine.findViewById(R.id.item_icon)).setImageResource(R.drawable.ic_menu_filter);
        infoLine.findViewById(R.id.item_checkbox).setVisibility(View.GONE);
        textView = infoLine.findViewById(R.id.item_text);
        ll.addView(infoLine);

        final MaterialButton modify = (MaterialButton) inflater.inflate(R.layout.button_view, ll, false);
        modify.setText(R.string.cache_filter_nested_filter_modify);
        modify.setOnClickListener(v -> ((GeocacheFilterActivity) getActivity()).selectNestedFilter(this));
        ll.addView(modify);

        // restore state
        setViewFromFilter(createFilterFromView());

        return ll;
    }

    @Override
    public void setViewFromFilter(final LogicalGeocacheFilter filter) {
        this.filter = filter != null ? filter : createFilter();
        final String text = this.filter.toUserDisplayableString(0);
        textView.setText(StringUtils.isNotBlank(text) ? text : getActivity().getString(R.string.cache_filter_nested_filter_empty_filterconfic));
    }

    @Override
    public LogicalGeocacheFilter createFilterFromView() {
        return filter;
    }

}
