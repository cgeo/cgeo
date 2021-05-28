package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.StatusGeocacheFilter;
import cgeo.geocaching.ui.ToggleButtonGroup;
import cgeo.geocaching.ui.ViewUtils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.List;

public class StatusFilterViewHolder extends BaseFilterViewHolder<StatusGeocacheFilter> {

    private CheckBox excludeActive = null;
    private CheckBox excludeDisabled = null;
    private CheckBox excludeArchived = null;

    private ToggleButtonGroup statusOwn = null;
    private ToggleButtonGroup statusFound = null;


    @Override
    public View createView() {
        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);
        final List<Integer> valueWidth = Arrays.asList(null,
            getMaxWidth(StatusGeocacheFilter.StatusType.FOUND.noId, StatusGeocacheFilter.StatusType.OWN.noId),
            getMaxWidth(StatusGeocacheFilter.StatusType.FOUND.yesId, StatusGeocacheFilter.StatusType.OWN.yesId));
        statusFound = createGroup(ll, valueWidth, StatusGeocacheFilter.StatusType.FOUND);
        statusOwn = createGroup(ll, valueWidth, StatusGeocacheFilter.StatusType.OWN);

        excludeActive = ViewUtils.addCheckboxItem(getActivity(), ll, R.string.cache_filter_status_exclude_active, R.drawable.ic_menu_myplaces);
        excludeDisabled = ViewUtils.addCheckboxItem(getActivity(), ll, R.string.cache_filter_status_exclude_disabled, R.drawable.ic_menu_disabled);
        excludeArchived = ViewUtils.addCheckboxItem(getActivity(), ll, R.string.cache_filter_status_exclude_archived, R.drawable.ic_menu_archived);
        excludeArchived.setChecked(true);
        return ll;
    }

    private ToggleButtonGroup createGroup(final LinearLayout ll, final List<Integer> valueWidth, final StatusGeocacheFilter.StatusType statusType) {
        final ToggleButtonGroup tgb = new ToggleButtonGroup(getActivity());
        tgb.setValues(Arrays.asList(getActivity().getString(statusType.allId), getActivity().getString(statusType.noId), getActivity().getString(statusType.yesId)), valueWidth);
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
        excludeActive.setChecked(filter.isExcludeActive());
        excludeDisabled.setChecked(filter.isExcludeDisabled());
        excludeArchived.setChecked(filter.isExcludeArchived());
        setFromBoolean(statusFound, filter.getStatusFound());
        setFromBoolean(statusOwn, filter.getStatusOwn());
    }


    @Override
    public StatusGeocacheFilter createFilterFromView() {
        final StatusGeocacheFilter filter = createFilter();
        filter.setExcludeActive(excludeActive.isChecked());
        filter.setExcludeDisabled(excludeDisabled.isChecked());
        filter.setExcludeArchived(excludeArchived.isChecked());
        filter.setStatusFound(getFromGroup(statusFound));
        filter.setStatusOwn(getFromGroup(statusOwn));
        return filter;
    }

    private void setFromBoolean(final ToggleButtonGroup tbg, final Boolean status) {
        tbg.setSelectedValue(status == null ? 0 : (status ? 2 : 1));
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
