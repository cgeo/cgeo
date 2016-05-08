package cgeo.geocaching.connector.gc;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import butterknife.BindView;
import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.models.PocketQuery;
import cgeo.geocaching.ui.AbstractViewHolder;
import cgeo.geocaching.utils.Formatter;

public class PocketQueryListAdapter extends ArrayAdapter<PocketQuery> {

    private final LayoutInflater inflater;

    protected static final class ViewHolder extends AbstractViewHolder {
        @BindView(R.id.label) TextView label;
        @BindView(R.id.download) Button download;
        @BindView(R.id.cachelist) Button cachelist;
        @BindView(R.id.info) TextView info;
        public ViewHolder(final View view) {
            super(view);
        }
    }

    public PocketQueryListAdapter(final Activity context) {
        super(context, 0);
        inflater = context.getLayoutInflater();
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final PocketQuery pocketQuery = getItem(position);

        View view = convertView;

        // holder pattern implementation
        final ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.pocketquery_item, parent, false);
            holder = new ViewHolder(view);
        } else {
            holder = (ViewHolder) view.getTag();
        }

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

        return view;
    }


}
