package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.PocketQuery;
import cgeo.geocaching.ui.AbstractViewHolder;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import butterknife.Bind;

public class PocketQueryListAdapter extends ArrayAdapter<PocketQuery> {

    final private LayoutInflater inflater;

    protected static final class ViewHolder extends AbstractViewHolder {
        @Bind(R.id.label) protected TextView label;
        @Bind(R.id.caches) protected TextView caches;

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

        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                final Activity activity = (Activity) v.getContext();
                CacheListActivity.startActivityPocket(activity, pocketQuery);
            }
        });

        holder.label.setText(pocketQuery.getName());
        holder.label.setCompoundDrawablesWithIntrinsicBounds(pocketQuery.getIcon(), 0, 0, 0);
        final int caches = pocketQuery.getMaxCaches();
        holder.caches.setText(caches >= 0 ? CgeoApplication.getInstance().getResources().getQuantityString(R.plurals.cache_counts, caches, caches) : StringUtils.EMPTY);

        return view;
    }

}