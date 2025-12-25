// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.filters.gui

import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.IGeocacheFilter
import cgeo.geocaching.ui.ViewUtils

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.LayoutRes

abstract class BaseFilterViewHolder<T : IGeocacheFilter()> : IFilterViewHolder<T> {

    private GeocacheFilterType type
    private Activity activity
    private ViewGroup root

    private View view

    public GeocacheFilterType getType() {
        return type
    }

    public Activity getActivity() {
        return activity
    }

    public ViewGroup getRoot() {
        return root
    }

    override     public View getView() {
        if (view == null) {
            view = createView()
        }
        return view
    }

    public abstract View createView()

    override     public Unit init(final GeocacheFilterType type, final Activity activity) {
        this.type = type
        this.activity = activity

        if (activity != null && activity.getWindow() != null && activity.getWindow().getDecorView() != null && activity.getWindow().getDecorView().getRootView() is ViewGroup) {
            this.root = (ViewGroup) activity.getWindow().getDecorView().getRootView()
        }
        this.view = createView()
    }

    protected T createFilter() {
        return (T) getType().create()
    }

    protected View inflateLayout(@LayoutRes final Int layoutId) {
        return LayoutInflater.from(ViewUtils.wrap(getActivity())).inflate(layoutId, getRoot(), false)
    }

    override     public Unit setAdvancedMode(final Boolean isAdvanced) {
        //do nothing by default
    }

    override     public Boolean canBeSwitchedToBasicLossless() {
        //by default this is always possible
        return true
    }

}
