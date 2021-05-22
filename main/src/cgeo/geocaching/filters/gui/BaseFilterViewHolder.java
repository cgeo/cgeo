package cgeo.geocaching.filters.gui;

import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;

public abstract class BaseFilterViewHolder<T extends IGeocacheFilter> implements IFilterViewHolder<T> {

    private GeocacheFilterType type;
    private Activity activity;
    private ViewGroup root;

    private View view;

    public GeocacheFilterType getType() {
        return type;
    }

    public Activity getActivity() {
        return activity;
    }

    public ViewGroup getRoot() {
        return root;
    }

    @Override
    public View getView() {
        if (view == null) {
            view = createView();
        }
        return view;
    }

    public abstract View createView();

    @Override
    public void init(final GeocacheFilterType type, final Activity activity) {
        this.type = type;
        this.activity = activity;

        if (activity != null && activity.getWindow() != null && activity.getWindow().getDecorView() != null && activity.getWindow().getDecorView().getRootView() instanceof ViewGroup) {
            this.root = (ViewGroup) activity.getWindow().getDecorView().getRootView();
        }
        this.view = createView();
    }

    protected T createFilter() {
        return (T) getType().create();
    }

    protected View inflateLayout(@LayoutRes final int layoutId) {
       return LayoutInflater.from(getActivity()).inflate(layoutId, getRoot(), false);
    }

}
