package cgeo.geocaching.ui.recyclerview;

import android.app.Activity;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public final class RecyclerViewProvider {

    private RecyclerViewProvider() {
        // utility class
    }

    public static RecyclerView provideRecyclerView(@NonNull final Activity context, @IdRes final int viewId, final boolean fixedSize, final boolean showDivider) {
        final RecyclerView view = context.findViewById(viewId);
        configureView(context, view, fixedSize, showDivider);
        return view;
    }

    private static void configureView(final Activity context, final RecyclerView view, final boolean fixedSize, final boolean showDivider) {
        view.setHasFixedSize(fixedSize);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        view.setLayoutManager(layoutManager);
        if (showDivider) {
            view.addItemDecoration(new DividerItemDecoration(context, layoutManager.getOrientation()));
        }
        view.setItemAnimator(new DefaultItemAnimator());
    }

    public static void provideRecyclerView(@NonNull final Activity context, final RecyclerView view, final boolean fixedSize, final boolean showDivider) {
        configureView(context, view, fixedSize, showDivider);
    }
}
