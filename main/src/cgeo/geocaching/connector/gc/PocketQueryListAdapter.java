package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.models.PocketQuery;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewAdapter;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.utils.Formatter;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;

class PocketQueryListAdapter extends AbstractRecyclerViewAdapter<PocketQueryListAdapter.ViewHolder> {

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
        final ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.cachelist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                CacheListActivity.startActivityPocket(view.getContext(), queries.get(viewHolder.getItemPosition()));
            }
        });

        viewHolder.download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                CacheListActivity.startActivityPocketDownload(view.getContext(), queries.get(viewHolder.getItemPosition()));
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        super.onBindViewHolder(holder, position);
        final PocketQuery pocketQuery = queries.get(position);
        holder.download.setVisibility(pocketQuery.isDownloadable() ? View.VISIBLE : View.GONE);
        holder.label.setText(pocketQuery.getName());
        holder.info.setText(Formatter.formatPocketQueryInfo(pocketQuery));
    }

}
