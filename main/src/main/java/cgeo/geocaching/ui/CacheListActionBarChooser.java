package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.list.AbstractList;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.utils.ColorUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;

import java.util.Collections;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

/**
 * GUI helper for activities dealing with cache lists
 * <br>
 * Displays a clickalbe "dropdown-like" Title and Subtitle in the action bar of an activity.
 * On click it let's the user choose a stored list and displays its data
 */
public class CacheListActionBarChooser {


    private final Activity context;
    private final Supplier<ActionBar> actionBarSupplier;
    private final Action1<Integer> onSelectAction;
    private int listId = Integer.MIN_VALUE;
    private int visibleCaches = 0;
    private boolean isLimited = false;
    private String title;
    private String subtitle;

    /**
     * @param context activity for actionbar
     * @param actionBarSupplier a supplier to get the actionbar
     * @param onSelectAction action to call when a list is selected
     */
    public CacheListActionBarChooser(final Activity context, final Supplier<ActionBar> actionBarSupplier,
                                     final Action1<Integer> onSelectAction) {
        this.context = context;
        this.actionBarSupplier = actionBarSupplier;
        this.onSelectAction = onSelectAction;
    }

    /** Sets title to a stored list's data */
    public void setList(final int listId, final int visibleCaches, final boolean isLimited) {
        this.title = null;
        this.subtitle = null;
        this.listId = listId;
        this.visibleCaches = visibleCaches;
        this.isLimited = isLimited;
        refreshActionBarTitle();
    }

    /** Sets title directly, additionally displays number of visible caches in subtitle */
    public void setDirect(final String title, final int visibleCaches) {
        this.title = title;
        this.subtitle = null;
        this.listId = Integer.MIN_VALUE;
        this.visibleCaches = visibleCaches;
        this.isLimited = false;
        refreshActionBarTitle();
    }

    /** Sets title and subtitle directly */
    public void setDirect(final String title, final String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
        this.listId = Integer.MIN_VALUE;
        this.visibleCaches = 0;
        this.isLimited = false;
        refreshActionBarTitle();
    }

    @SuppressLint("InflateParams")
    private void refreshActionBarTitle() {
        final ActionBar actionBar = this.actionBarSupplier.get();
        if (actionBar == null) {
            return;
        }

        View resultView = actionBar.getCustomView();
        if (resultView == null) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            resultView = inflater.inflate(R.layout.cachelist_chooser_actionbar, null, false);
            actionBar.setCustomView(resultView);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.getCustomView().setOnClickListener(v -> new StoredList.UserInterface(context).promptForListSelection(R.string.list_title, selectedListId -> {
                if (selectedListId != listId) {
                    this.onSelectAction.call(selectedListId);
                    this.listId = selectedListId;
                    refreshActionBarTitle();
                }
            }, false, Collections.singleton(PseudoList.NEW_LIST.id), listId, null));
        }

        final TextView titleTv = resultView.findViewById(android.R.id.text1);
        final TextView subtitleTv = resultView.findViewById(android.R.id.text2);

        final AbstractList list = AbstractList.getListById(listId);
        if (list != null) {
            final ImageParam ip = StoredList.UserInterface.getImageForList(list, false);
            if (ip.isReferencedById()) { // see #15883
                TextParam.text(list.getTitle()).setImage(ip).setImageTint(ColorUtils.colorFromResource(R.color.colorTextActionBar)).applyTo(titleTv);
            } else {
                TextParam.text(list.getTitle()).setImage(ip).applyTo(titleTv);
            }
            if (list.getNumberOfCaches() >= 0) {
                subtitleTv.setVisibility(View.VISIBLE);
                subtitleTv.setText(getCacheListSubtitle(list));
            } else {
                subtitleTv.setVisibility(View.GONE);
            }
        } else if (this.title != null) {
            titleTv.setText(this.title);
            subtitleTv.setVisibility(View.VISIBLE);
            if (this.subtitle == null) {
                subtitleTv.setText(getCacheNumber(visibleCaches));
            } else {
                subtitleTv.setText(this.subtitle);
            }
        }

    }

    private CharSequence getCacheListSubtitle(final AbstractList list) {

        final int numberOfCaches = list.getNumberOfCaches();
        if (numberOfCaches < 0) {
            return StringUtils.EMPTY;
        }

        final StringBuilder sb = new StringBuilder();
        if (visibleCaches != numberOfCaches) {
            sb.append(visibleCaches);
            if (isLimited) {
                sb.append("+");
            }
            sb.append("/");
        }
        sb.append(getCacheNumber(numberOfCaches));
        return sb.toString();
    }

    private CharSequence getCacheNumber(final int count) {
        return context.getResources().getQuantityString(R.plurals.cache_counts, count, count);
    }

}
