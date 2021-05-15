package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.StatusGeocacheFilter;
import cgeo.geocaching.ui.ToggleButtonGroup;
import cgeo.geocaching.ui.ViewUtils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StatusFilterViewHolder extends BaseFilterViewHolder<StatusGeocacheFilter> {

    private ToggleButtonGroup statusDisabled = null;
    private ToggleButtonGroup statusArchived = null;
    private ToggleButtonGroup statusOwn = null;
    private ToggleButtonGroup statusFound = null;


    @Override
    public View createView() {
        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);
        final List<Integer> valueWidth = Arrays.asList(null,
            getMaxWidth(R.string.cache_filter_status_select_only_archived_no, R.string.cache_filter_status_select_only_disabled_no,
                R.string.cache_filter_status_select_only_found_no, R.string.cache_filter_status_select_only_own_no),
            getMaxWidth(R.string.cache_filter_status_select_only_archived_yes, R.string.cache_filter_status_select_only_disabled_yes,
                R.string.cache_filter_status_select_only_found_yes, R.string.cache_filter_status_select_only_own_yes));
        statusFound = createGroup(ll, valueWidth, R.string.cache_filter_status_select_all, R.string.cache_filter_status_select_only_found_no, R.string.cache_filter_status_select_only_found_yes);
        statusOwn = createGroup(ll, valueWidth, R.string.cache_filter_status_select_all, R.string.cache_filter_status_select_only_own_no, R.string.cache_filter_status_select_only_own_yes);
        statusDisabled = createGroup(ll, valueWidth, R.string.cache_filter_status_select_all, R.string.cache_filter_status_select_only_disabled_no, R.string.cache_filter_status_select_only_disabled_yes);
        statusArchived = createGroup(ll, valueWidth, R.string.cache_filter_status_select_all, R.string.cache_filter_status_select_only_archived_no, R.string.cache_filter_status_select_only_archived_yes);
        return ll;
    }

    private ToggleButtonGroup createGroup(final LinearLayout ll, final List<Integer> valueWidth, @StringRes final int ... valueResIds) {
        final ToggleButtonGroup tgb = new ToggleButtonGroup(getActivity());
        final List<String> values = new ArrayList<>();
        for (int valueResId : valueResIds) {
            values.add(getActivity().getString(valueResId));
        }
        tgb.setValues(values, valueWidth);
        final LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, ViewUtils.dpToPixel(5), 0, ViewUtils.dpToPixel(5));
        ll.addView(tgb, llp);
        return tgb;
    }

    private int getMaxWidth(@StringRes final int ... resIds) {
        int max = 0;
        for (int resId : resIds) {
            max = Math.max(max, ViewUtils.getMinimalWidth(getActivity(), getActivity().getString(resId), R.style.button_small));
        }
        return max;
    }

    @Override
    public void setViewFromFilter(final StatusGeocacheFilter filter) {
        setFromBoolean(statusDisabled, filter.getStatusDisabled());
        setFromBoolean(statusArchived, filter.getStatusArchived());
        setFromBoolean(statusFound, filter.getStatusFound());
        setFromBoolean(statusOwn, filter.getStatusOwn());
    }


    @Override
    public StatusGeocacheFilter createFilterFromView() {
        final StatusGeocacheFilter filter = createFilter();
        filter.setStatusDisabled(getFromGroup(statusDisabled));
        filter.setStatusArchived(getFromGroup(statusArchived));
        filter.setStatusFound(getFromGroup(statusFound));
        filter.setStatusOwn(getFromGroup(statusOwn));
        return filter;
    }

    private void setFromBoolean(final ToggleButtonGroup tbg, final Boolean status) {
        tbg.selectValue(status == null ? 0 : (status ? 2 : 1));
    }

    private Boolean getFromGroup(final ToggleButtonGroup tbg) {
        switch (tbg.getSelectedValue()) {
            case 1: return false;
            case 2: return true;
            case 0:
            default: return null;
        }
    }
}
