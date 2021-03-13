package cgeo.geocaching.filters.gui;

import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;

import android.content.Context;
import android.view.View;

public abstract class BaseFilterViewHolder<T extends IGeocacheFilter> implements IFilterViewHolder<T> {

    private GeocacheFilterType type;
    private Context context;
    private View view;

    public GeocacheFilterType getType() {
        return type;
    }

    public Context getContext() {
        return context;
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
    public void init(final GeocacheFilterType type, final Context context) {
        this.type = type;
        this.context = context;
    }

    protected T createFilter() {
        return (T) getType().create();
    }

}
