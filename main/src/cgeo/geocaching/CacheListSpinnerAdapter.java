package cgeo.geocaching;

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

    private final Context mContext;

    public CacheListSpinnerAdapter(Context context, int resource) {
        super(context, resource);
        mContext = context;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }


    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(final int position, final View convertView, final ViewGroup parent) {

        View resultView = convertView;
        LayoutInflater inflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        CacheListSpinnerAdapter.ViewHolder holder;
        if (resultView == null) {
            resultView = inflater.inflate(R.layout.cachelist_spinneritem, parent, false);
            holder = new ViewHolder();
            holder.title = (TextView) resultView.findViewById(android.R.id.text1);
            holder.subtitle = (TextView) resultView.findViewById(android.R.id.text2);

            resultView.setTag(holder);
        } else {
            holder = (CacheListSpinnerAdapter.ViewHolder) resultView.getTag();
        }

        AbstractList list = getItem(position);
        holder.title.setText(list.getTitle());
        if (list.getCount() >= 0) {
            holder.subtitle.setVisibility(View.VISIBLE);
            holder.subtitle.setText(CacheListActivity.getCacheNumberString(mContext.getResources(),list.getCount()));
        } else {
            holder.subtitle.setVisibility(View.GONE);
        }

        return resultView;
    }
}