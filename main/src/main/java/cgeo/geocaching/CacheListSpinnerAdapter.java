package cgeo.geocaching;

import cgeo.geocaching.list.AbstractList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.ui.TextParam;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

class CacheListSpinnerAdapter extends ArrayAdapter<AbstractList> {

    static class ViewHolder {
        TextView title;
        TextView subtitle;
    }

    private final CacheListActivity cacheListActivity;

    CacheListSpinnerAdapter(final CacheListActivity context, final int resource) {
        super(context, resource);
        cacheListActivity = context;
    }


    @Override
    public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
        return getCustomView(position, convertView, parent, R.layout.cachelist_spinner_actionbar);
    }


    @Override
    public View getDropDownView(final int position, final View convertView, @NonNull final ViewGroup parent) {
        return getCustomView(position, convertView, parent, R.layout.cachelist_spinner_dropdownitem);
    }

    public View getCustomView(final int position, final View convertView, final ViewGroup parent, final @LayoutRes int layoutRes) {

        View resultView = convertView;
        final LayoutInflater inflater = LayoutInflater.from(cacheListActivity);

        final CacheListSpinnerAdapter.ViewHolder holder;
        if (resultView == null) {
            resultView = inflater.inflate(layoutRes, parent, false);
            holder = new ViewHolder();
            holder.title = resultView.findViewById(android.R.id.text1);
            holder.subtitle = resultView.findViewById(android.R.id.text2);

            resultView.setTag(holder);
        } else {
            holder = (CacheListSpinnerAdapter.ViewHolder) resultView.getTag();
        }

        final AbstractList list = getItem(position);
        TextParam.text(list.getTitle()).setImage(StoredList.UserInterface.getImageForList(list)).applyTo(holder.title);
        if (list.getNumberOfCaches() >= 0) {
            holder.subtitle.setVisibility(View.VISIBLE);
            holder.subtitle.setText(cacheListActivity.getCacheListSubtitle(list));
        } else {
            holder.subtitle.setVisibility(View.GONE);
        }

        return resultView;
    }
}
