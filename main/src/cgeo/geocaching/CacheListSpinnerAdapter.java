package cgeo.geocaching;

import butterknife.ButterKnife;

import cgeo.geocaching.list.AbstractList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

class CacheListSpinnerAdapter extends ArrayAdapter<AbstractList> {

    static class ViewHolder {
        TextView title;
        TextView subtitle;
    }

    private final CacheListActivity cacheListActivity;

    public CacheListSpinnerAdapter(final CacheListActivity context, final int resource) {
        super(context, resource);
        cacheListActivity = context;
    }


    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }


    @Override
    public View getDropDownView(final int position, final View convertView, final ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(final int position, final View convertView, final ViewGroup parent) {

        View resultView = convertView;
        final LayoutInflater inflater =
                (LayoutInflater) cacheListActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        final CacheListSpinnerAdapter.ViewHolder holder;
        if (resultView == null) {
            resultView = inflater.inflate(R.layout.cachelist_spinneritem, parent, false);
            holder = new ViewHolder();
            holder.title = ButterKnife.findById(resultView, android.R.id.text1);
            holder.subtitle = ButterKnife.findById(resultView, android.R.id.text2);

            resultView.setTag(holder);
        } else {
            holder = (CacheListSpinnerAdapter.ViewHolder) resultView.getTag();
        }

        final AbstractList list = getItem(position);
        holder.title.setText(list.getTitle());
        if (list.getNumberOfCaches() >= 0) {
            holder.subtitle.setVisibility(View.VISIBLE);
            holder.subtitle.setText(cacheListActivity.getCacheListSubtitle(list));
        } else {
            holder.subtitle.setVisibility(View.GONE);
        }

        return resultView;
    }
}