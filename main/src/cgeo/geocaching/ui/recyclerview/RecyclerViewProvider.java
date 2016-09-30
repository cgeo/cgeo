package cgeo.geocaching.ui.recyclerview;

import android.app.Activity;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import butterknife.ButterKnife;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

public final class RecyclerViewProvider {

    private RecyclerViewProvider() {
        // utility class
    }

    public static RecyclerView provideRecyclerView(@NonNull final Activity context, @IntegerRes final int viewId, final boolean fixedSize) {
        final RecyclerView view = ButterKnife.findById(context, viewId);
        view.setHasFixedSize(fixedSize);
        view.addItemDecoration(new HorizontalDividerItemDecoration.Builder(context).build());
        view.setLayoutManager(new LinearLayoutManager(context));
        view.setItemAnimator(new DefaultItemAnimator());

        return view;
    }
}
