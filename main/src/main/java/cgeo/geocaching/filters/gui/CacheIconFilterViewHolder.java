package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.CacheIconGeocacheFilter;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.EmojiUtils;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.button.MaterialButton;

public class CacheIconFilterViewHolder extends BaseFilterViewHolder<CacheIconGeocacheFilter> {

    private final List<String> selectedIcons = new ArrayList<>();
    private LinearLayout iconList;
    private LinearLayout view;

    @Override
    public View createView() {
        view = new LinearLayout(getActivity());
        view.setOrientation(LinearLayout.VERTICAL);
        iconList = new LinearLayout(getActivity());
        iconList.setOrientation(LinearLayout.VERTICAL);
        view.addView(iconList, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        final MaterialButton selectButton = (MaterialButton) ViewUtils.createButton(getActivity(), view, TextParam.text(""), R.layout.button_icon_view);
        selectButton.setIconResource(R.drawable.ic_menu_add);
        selectButton.setContentDescription(getActivity().getString(R.string.log_add));
        selectButton.setOnClickListener(v -> EmojiUtils.selectEmojiPopup(getActivity(), null, false, null, this::addIcon));
        view.addView(selectButton);
        refreshIcons();
        return view;
    }

    @Override
    public void setViewFromFilter(final CacheIconGeocacheFilter filter) {
        selectedIcons.clear();
        selectedIcons.addAll(filter.getIcons());
        refreshIcons();
    }

    @Override
    public CacheIconGeocacheFilter createFilterFromView() {
        final CacheIconGeocacheFilter filter = createFilter();
        filter.setIcons(selectedIcons);
        return filter;
    }

    private void addIcon(final String icon) {
        if (icon != null && !icon.isEmpty() && !selectedIcons.contains(icon)) {
            selectedIcons.add(icon);
            refreshIcons();
        }
    }

    private void refreshIcons() {
        if (iconList == null) {
            return;
        }
        iconList.removeAllViews();
        for (String icon : selectedIcons) {
            final LinearLayout row = new LinearLayout(getActivity());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            final TextView iconView = ViewUtils.createTextItem(getActivity(), R.style.alertDialogTitleTextStyle, TextParam.text(icon));
            row.addView(iconView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            final MaterialButton deleteButton = (MaterialButton) ViewUtils.createButton(getActivity(), row, TextParam.text(""), R.layout.button_icon_view);
            deleteButton.setIconResource(R.drawable.ic_menu_delete);
            deleteButton.setContentDescription(getActivity().getString(R.string.delete));
            deleteButton.setOnClickListener(v -> {
                selectedIcons.remove(icon);
                refreshIcons();
            });
            row.addView(deleteButton);
            iconList.addView(row);
        }
    }
}
