package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.CacheFilterCheckboxItemBinding;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.StringRes;

public class FilterGuiUtils {

    private FilterGuiUtils() {
        //no instance
    }

    public static CheckBox addCheckboxProperty(final Activity activity, final ViewGroup viewGroup, @StringRes final int textId, final int iconId) {
        return addCheckboxProperty(activity, viewGroup, textId, iconId, 0);
    }

    public static  CheckBox addCheckboxProperty(final Activity activity, final ViewGroup viewGroup, @StringRes final int textId, final int iconId, @StringRes final int infoTextId) {
        return addCheckboxProperty(activity, viewGroup, activity.getString(textId), iconId, infoTextId);
    }

    public static  CheckBox addCheckboxProperty(final Activity activity, final ViewGroup viewGroup, final String text, final int iconId, @StringRes final int infoTextId) {

        final View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cache_filter_checkbox_item, viewGroup, false);
        final CacheFilterCheckboxItemBinding itemBinding = CacheFilterCheckboxItemBinding.bind(itemView);
        itemBinding.itemText.setText(text);
        if (iconId > -1) {
            itemBinding.itemIcon.setImageResource(iconId);
        }
        if (infoTextId != 0) {
            itemBinding.itemInfo.setVisibility(View.VISIBLE);
            itemBinding.itemInfo.setOnClickListener(v -> Dialogs.message(activity, infoTextId));
        }
        viewGroup.addView(itemView);
        itemBinding.itemIcon.setOnClickListener(v -> itemBinding.itemCheckbox.toggle());
        itemBinding.itemText.setOnClickListener(v -> itemBinding.itemCheckbox.toggle());

        return itemBinding.itemCheckbox;
    }


}
