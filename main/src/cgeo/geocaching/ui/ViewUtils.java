package cgeo.geocaching.ui;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.databinding.CheckboxItemBinding;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.functions.Func2;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;

import java.util.List;

public class ViewUtils {

    //if this flag is true, then layouts will be generated with background colors, for layout checking use cases
    private static final boolean DEBUG_LAYOUT = false;

    private static final Context APP_CONTEXT = CgeoApplication.getInstance() == null ? null : CgeoApplication.getInstance().getApplicationContext();
    private static final Resources APP_RESSOURCES = APP_CONTEXT == null ? null : APP_CONTEXT.getResources();

    private ViewUtils() {
        //no instance
    }

    public static boolean isDebugLayout() {
        return DEBUG_LAYOUT;
    }

    public static int dpToPixel(final float dp)  {
        return (int) (dp * (APP_RESSOURCES == null ? 20f : APP_RESSOURCES.getDisplayMetrics().density));
    }

    public static void setTooltip(final View view, @StringRes final int textId) {
        setTooltip(view, LocalizationUtils.getString(textId));
    }

    public static void setTooltip(final View view, final String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.setTooltipText(text);
        }
    }


    public static <T> ViewGroup createHorizontallyDistributedText(final Context ctx, final LinearLayout root, final List<T> items, final Func2<Integer, T, String> itemTextMapper) {
        return createHorizontallyDistributedViews(ctx, root, items, (idx, item) -> {
            final String itemText = item == null ? null : itemTextMapper.call(idx, item);
            if (itemText != null) {
                final TextView tv = new TextView(ctx);
                tv.setText(itemText);
                tv.setMaxLines(1);
                if (APP_RESSOURCES != null) {
                    tv.setTextColor(APP_RESSOURCES.getColor(Settings.isLightSkin() ? android.R.color.black : android.R.color.white));
                }
                if (DEBUG_LAYOUT) {
                    tv.setBackgroundResource(R.drawable.mark_orange);
                }
                return tv;
            }
            return null;
        });
    }

    public static <T> ViewGroup createHorizontallyDistributedViews(final Context ctx, final LinearLayout root, final List<T> items, final Func2<Integer, T, View> viewCreator) {

        final LinearLayout viewGroup = root == null ? new LinearLayout(ctx) : root;

        int idx = 0;
        for (T item : items) {
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.weight = 1;

            final RelativeLayout rl = new RelativeLayout(ctx);
            if (DEBUG_LAYOUT) {
                rl.setBackgroundResource(R.drawable.mark_green);
            }
            viewGroup.addView(rl, lp);

            final RelativeLayout.LayoutParams rp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            rp.addRule(RelativeLayout.CENTER_IN_PARENT);

            final View itemView = viewCreator.call(idx, item);
            if (itemView != null) {
                rl.addView(itemView, rp);
            }
            idx++;
        }
        return viewGroup;
    }

    public static  CheckBox addCheckboxItem(final Activity activity, final ViewGroup viewGroup, @StringRes final int textId, final int iconId, @StringRes final int infoTextId) {
        return addCheckboxItem(activity, viewGroup, activity.getString(textId), iconId, infoTextId);
    }

    public static CheckBox addCheckboxItem(final Activity activity, final ViewGroup viewGroup, @StringRes final int textId, final int iconId) {
        return addCheckboxItem(activity, viewGroup, textId, iconId, 0);
    }

    public static CheckBox addCheckboxItem(final Activity activity, final ViewGroup viewGroup, final String text, final int iconId, @StringRes final int infoTextId) {

        final View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.checkbox_item, viewGroup, false);
        final CheckboxItemBinding itemBinding = CheckboxItemBinding.bind(itemView);
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

    /**
     * Sets ListView height dynamically based on the height of the items.
     *
     * @param listView to be resized
     * @return true if the listView is successfully resized, false otherwise
     */
    public static boolean setListViewHeightBasedOnItems(final ListView listView) {
        final ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter != null) {

            final int numberOfItems = listAdapter.getCount();

            // Get total height of all items.
            int totalItemsHeight = 0;
            for (int itemPos = 0; itemPos < numberOfItems; itemPos++) {
                final View item = listAdapter.getView(itemPos, null, listView);
                item.measure(0, 0);
                totalItemsHeight += item.getMeasuredHeight();
            }

            // Get total height of all item dividers.
            final int totalDividersHeight = listView.getDividerHeight() *
                    (numberOfItems - 1);

            // Set list height.
            final ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalItemsHeight + totalDividersHeight;
            listView.setLayoutParams(params);
            listView.requestLayout();

            return true;
        }

        return false;
    }

    public static int getMinimalWidth(final Context ctx, final String text, final int styleId) {
        final TextView tv = new TextView(ctx, null, 0, styleId);
        tv.setText(text);
        tv.measure(0, 0);
        return tv.getMeasuredWidth();

    }

}
