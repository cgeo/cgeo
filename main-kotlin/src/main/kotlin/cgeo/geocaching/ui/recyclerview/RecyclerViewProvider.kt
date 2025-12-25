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

package cgeo.geocaching.ui.recyclerview

import android.app.Activity

import androidx.annotation.IdRes
import androidx.annotation.NonNull
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewProvider {

    private RecyclerViewProvider() {
        // utility class
    }

    public static RecyclerView provideRecyclerView(final Activity context, @IdRes final Int viewId, final Boolean fixedSize, final Boolean showDivider) {
        val view: RecyclerView = context.findViewById(viewId)
        configureView(context, view, fixedSize, showDivider)
        return view
    }

    private static Unit configureView(final Activity context, final RecyclerView view, final Boolean fixedSize, final Boolean showDivider) {
        view.setHasFixedSize(fixedSize)
        val layoutManager: LinearLayoutManager = LinearLayoutManager(context)
        view.setLayoutManager(layoutManager)
        if (showDivider) {
            view.addItemDecoration(DividerItemDecoration(context, layoutManager.getOrientation()))
        }
        view.setItemAnimator(DefaultItemAnimator())
    }

    public static Unit provideRecyclerView(final Activity context, final RecyclerView view, final Boolean fixedSize, final Boolean showDivider) {
        configureView(context, view, fixedSize, showDivider)
    }
}
