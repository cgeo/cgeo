package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.StatusGeocacheFilter;
import cgeo.geocaching.ui.ButtonToggleGroup;
import cgeo.geocaching.ui.ViewUtils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

public class StatusFilterViewHolder extends BaseFilterViewHolder<StatusGeocacheFilter> {

    private CheckBox excludeActive = null;
    private CheckBox excludeDisabled = null;
    private CheckBox excludeArchived = null;

    private ButtonToggleGroup statusOwn = null;
    private ButtonToggleGroup statusFound = null;


    @Override
    public View createView() {
        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);
        statusFound = createGroup(ll, StatusGeocacheFilter.StatusType.FOUND);
        statusOwn = createGroup(ll, StatusGeocacheFilter.StatusType.OWN);
        ButtonToggleGroup.alignWidths(statusOwn, statusFound);

        excludeActive = ViewUtils.addCheckboxItem(getActivity(), ll, R.string.cache_filter_status_exclude_active, R.drawable.ic_menu_circle);
        excludeDisabled = ViewUtils.addCheckboxItem(getActivity(), ll, R.string.cache_filter_status_exclude_disabled, R.drawable.ic_menu_disabled);
        excludeArchived = ViewUtils.addCheckboxItem(getActivity(), ll, R.string.cache_filter_status_exclude_archived, R.drawable.ic_menu_archived);
        excludeArchived.setChecked(true);
        return ll;
    }
    private ButtonToggleGroup createGroup(final LinearLayout ll, final StatusGeocacheFilter.StatusType statusType) {
        final ButtonToggleGroup tgb = new ButtonToggleGroup(getActivity());
        tgb.setUseRelativeWidth(true);
        tgb.addButtons(statusType.allId, statusType.noId, statusType.yesId);
        final LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, ViewUtils.dpToPixel(5), 0, ViewUtils.dpToPixel(5));
        ll.addView(tgb, llp);
        return tgb;
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

    private void setFromBoolean(final ButtonToggleGroup btg, final Boolean status) {
        btg.setCheckedButtonByIndex(status == null ? 0 : (status ? 2 : 1), true);
    }

   private Boolean getFromGroup(final ButtonToggleGroup btg) {
        switch (btg.getCheckedButtonIndex()) {
            case 1: return false;
            case 2: return true;
            case 0:
            default: return null;
        }    }
}
