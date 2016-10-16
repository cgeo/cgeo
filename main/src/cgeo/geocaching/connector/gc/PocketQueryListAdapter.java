package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.models.PocketQuery;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.utils.Formatter;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;

class PocketQueryListAdapter extends RecyclerView.Adapter<PocketQueryListAdapter.ViewHolder> {

    @NonNull private final List<PocketQuery> queries;

    protected static final class ViewHolder extends AbstractRecyclerViewHolder {
        @BindView(R.id.label) TextView label;
        @BindView(R.id.download) Button download;
        @BindView(R.id.cachelist) Button cachelist;
        @BindView(R.id.info) TextView info;

        ViewHolder(final View view) {
            super(view);
        }
    }

    PocketQueryListAdapter(@NonNull final List<PocketQuery> queries) {
        this.queries = queries;
    }

    @Override
    public int getItemCount() {
        return queries.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pocketquery_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final PocketQuery pocketQuery = queries.get(position);
        holder.cachelist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Activity activity = (Activity) v.getContext();
                CacheListActivity.startActivityPocket(activity, pocketQuery);
            }
        });

        holder.download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Activity activity = (Activity) v.getContext();
                CacheListActivity.startActivityPocketDownload(activity, pocketQuery);
            }
        });

        holder.download.setVisibility(pocketQuery.isDownloadable() ? View.VISIBLE : View.GONE);
        holder.label.setText(pocketQuery.getName());
        holder.info.setText(Formatter.formatPocketQueryInfo(pocketQuery));
    }

}
