package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.models.PocketQuery;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.utils.Formatter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;

class PocketQueryListAdapter extends RecyclerView.Adapter<PocketQueryListAdapter.ViewHolder> {

    @NonNull private final PocketQueryListActivity activity;

    protected static final class ViewHolder extends AbstractRecyclerViewHolder {
        @BindView(R.id.label) TextView label;
        @BindView(R.id.download) Button download;
        @BindView(R.id.cachelist) Button cachelist;
        @BindView(R.id.info) TextView info;

        ViewHolder(final View view) {
            super(view);
        }
    }

    PocketQueryListAdapter(@NonNull final PocketQueryListActivity pocketQueryListActivity) {
        this.activity = pocketQueryListActivity;
    }

    @Override
    public int getItemCount() {
        return activity.getQueries().size();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pocketquery_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.cachelist.setOnClickListener(view1 -> CacheListActivity.startActivityPocket(view1.getContext(), activity.getQueries().get(viewHolder.getAdapterPosition())));
        viewHolder.cachelist.setVisibility(activity.onlyDownloadable() ? View.GONE : View.VISIBLE);

        viewHolder.download.setOnClickListener(v -> {
            final PocketQuery pocketQuery = activity.getQueries().get(viewHolder.getAdapterPosition());
            if (activity.onlyDownloadable()) {
                activity.returnResult(pocketQuery);
            } else {
                CacheListActivity.startActivityPocketDownload(view.getContext(), pocketQuery);
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final PocketQuery pocketQuery = activity.getQueries().get(position);
        holder.download.setVisibility(pocketQuery.isDownloadable() ? View.VISIBLE : View.GONE);
        holder.label.setText(pocketQuery.getName());
        holder.info.setText(Formatter.formatPocketQueryInfo(pocketQuery));
    }

}
