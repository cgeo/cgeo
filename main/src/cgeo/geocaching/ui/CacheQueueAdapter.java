package cgeo.geocaching.ui;

import cgeo.geocaching.DownloadManagerActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.EnumSet;
import java.util.List;

public class CacheQueueAdapter extends ArrayAdapter<String> {

    private static LayoutInflater inflater = null;

    public static LayoutInflater getInflater() {

        return inflater;
    }

    private DownloadManagerActivity activity;

    private static class ViewHolder {
        TextView text;
        TextView info;
        Button remove;
    }

    public CacheQueueAdapter(final DownloadManagerActivity activity, final List<String> list) {
        super(activity, 0, list);
        this.activity = activity;
    }

    @Override
    public View getView(final int position, final View rowView, final ViewGroup parent) {
        if (position > getCount()) {
            Log.w("CacheListAdapter.getView: Attempt to access missing item #" + position);
            return null;
        }
        return setView(rowView, getItem(position), true);
    }

    public View setView(final View view, final String item, final boolean remove) {
        if (item == null) {
            return null;
        }
        if (inflater == null) {
            inflater = ((Activity) getContext()).getLayoutInflater();
        }

        final ViewHolder holder;
        View v = view;

        if (v == null) {
            v = inflater.inflate(R.layout.download_queue_item, null);
            holder = new ViewHolder();
            holder.text = (TextView) v.findViewById(R.id.text);
            holder.info = (TextView) v.findViewById(R.id.info);
            holder.remove = (Button) v.findViewById(R.id.delete_from_queue);
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        final cgCache cache = cgeoapplication.getInstance().loadCache(item, EnumSet.of(LoadFlag.LOAD_CACHE_BEFORE, LoadFlag.LOAD_DB_MINIMAL));
        if (cache == null) {
            holder.text.setText(item);
            holder.text.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            holder.info.setText("");
        } else {
            if (cache.getNameSp() == null) {
                cache.setNameSp((new Spannable.Factory()).newSpannable(cache.getName()));
                if (cache.isDisabled() || cache.isArchived()) { // strike
                    cache.getNameSp().setSpan(new StrikethroughSpan(), 0, cache.getNameSp().toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            holder.text.setText(cache.getNameSp(), TextView.BufferType.SPANNABLE);
            holder.info.setText(Formatter.formatCacheInfoLong(cache, CacheListType.OFFLINE));
            holder.text.setCompoundDrawablesWithIntrinsicBounds(CacheListAdapter.getCacheIcon(cache), null, null, null);
        }

        if (remove) {
            holder.remove.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    CacheQueueAdapter.this.activity.removeCacheFromQueue(item);
                }
            });
            holder.remove.setVisibility(View.VISIBLE);
        } else {
            holder.remove.setVisibility(View.GONE);
        }
        return v;
    }
}
