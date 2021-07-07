package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.databinding.PocketqueryItemBinding;
import cgeo.geocaching.models.PocketQuery;
import cgeo.geocaching.storage.extension.PocketQueryHistory;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.utils.Formatter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.StringUtils;

class PocketQueryListAdapter extends RecyclerView.Adapter<PocketQueryListAdapter.ViewHolder> {

    @NonNull private final PocketQueryListActivity activity;

    protected static final class ViewHolder extends AbstractRecyclerViewHolder {
        private final PocketqueryItemBinding binding;

        ViewHolder(final View view) {
            super(view);
            binding = PocketqueryItemBinding.bind(view);
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
        viewHolder.binding.cachelist.setOnClickListener(view1 -> CacheListActivity.startActivityPocket(view1.getContext(), activity.getQueries().get(viewHolder.getAdapterPosition())));
        viewHolder.binding.download.setOnClickListener(v -> {
            final PocketQuery pocketQuery = activity.getQueries().get(viewHolder.getAdapterPosition());
            PocketQueryHistory.updateLastDownload(pocketQuery);
            notifyDataSetChanged();

            if (activity.getStartDownload()) {
                CacheListActivity.startActivityPocketDownload(view.getContext(), pocketQuery);
            } else {
                activity.returnResult(pocketQuery);
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final PocketQuery pocketQuery = activity.getQueries().get(position);
        holder.binding.download.setVisibility(pocketQuery.isDownloadable() ? View.VISIBLE : View.GONE);
        holder.binding.cachelist.setVisibility(activity.onlyDownloadable() || pocketQuery.isBookmarkList() ? View.GONE : View.VISIBLE); // Currency, we aren't able to parse bookmark lists without download
        holder.binding.label.setText(pocketQuery.getName());
        final String info = Formatter.formatPocketQueryInfo(pocketQuery);
        holder.binding.info.setVisibility(StringUtils.isNotBlank(info) ? View.VISIBLE : View.GONE);
        holder.binding.info.setText(info);
    }

}
