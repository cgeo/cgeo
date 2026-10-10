package cgeo.geocaching.ui.recyclerview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewWithHeader extends RecyclerView {

    private final List<View> mHeaderViews = new ArrayList<>();
    @SuppressWarnings("rawtypes")
    private Adapter mAdapter;

    public RecyclerViewWithHeader(final @NonNull Context context) {
        super(context);
    }

    public RecyclerViewWithHeader(final @NonNull Context context, final @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerViewWithHeader(final @NonNull Context context, final @Nullable AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /** adds header view. Must be called before setAdapter() */
    @SuppressLint("NotifyDataSetChanged")
    public void addHeaderView(final @NonNull View v) {
        if (!mHeaderViews.contains(v)) {
            mHeaderViews.add(v);
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    /** removes header view */
    @SuppressLint("NotifyDataSetChanged")
    public void removeHeaderView(final @NonNull View v) {
        if (mHeaderViews.remove(v)) {
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void setAdapter(final @Nullable Adapter adapter) {
        if (adapter == null) {
            super.setAdapter(null);
            mAdapter = null;
            return;
        }

        // wrapping the original adapter
        mAdapter = new HeaderViewAdapter(mHeaderViews, adapter);
        super.setAdapter(mAdapter);
    }

    /** returns the original adapter, not the wrapper */
    @Nullable
    @Override
    @SuppressWarnings("rawtypes")
    public Adapter getAdapter() {
        if (mAdapter instanceof HeaderViewAdapter) {
            return ((HeaderViewAdapter) mAdapter).getWrappedAdapter();
        }
        return mAdapter;
    }

    // =============================================================================================
    // inner wrapper adapter
    // =============================================================================================
    @SuppressWarnings("rawtypes")
    private static class HeaderViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_HEADER_BASE = -1000;
        private final List<View> headerViews;
        private final RecyclerView.Adapter wrappedAdapter;

        HeaderViewAdapter(final List<View> headerViews, final RecyclerView.Adapter wrappedAdapter) {
            this.headerViews = headerViews;
            this.wrappedAdapter = wrappedAdapter;

            // forwards change notifications from original adapter to wrapper
            this.wrappedAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @SuppressLint("NotifyDataSetChanged")
                @Override
                public void onChanged() {
                    notifyDataSetChanged();
                }

                @Override
                public void onItemRangeChanged(final int positionStart, final int itemCount) {
                    notifyItemRangeChanged(positionStart + headerViews.size(), itemCount);
                }

                @Override
                public void onItemRangeChanged(final int positionStart, final int itemCount, final @Nullable Object payload) {
                    notifyItemRangeChanged(positionStart + headerViews.size(), itemCount, payload);
                }

                @Override
                public void onItemRangeInserted(final int positionStart, final int itemCount) {
                    notifyItemRangeInserted(positionStart + headerViews.size(), itemCount);
                }

                @Override
                public void onItemRangeRemoved(final int positionStart, final int itemCount) {
                    notifyItemRangeRemoved(positionStart + headerViews.size(), itemCount);
                }

                @SuppressLint("NotifyDataSetChanged")
                @Override
                public void onItemRangeMoved(final int fromPosition, final int toPosition, final int itemCount) {
                    notifyDataSetChanged();
                }
            });
        }

        public RecyclerView.Adapter getWrappedAdapter() {
            return wrappedAdapter;
        }

        @Override
        public int getItemViewType(final int position) {
            final int numHeaders = headerViews.size();
            if (position < numHeaders) {
                // each header gets its own (negative) ViewType id
                return TYPE_HEADER_BASE - position;
            }
            // for regular items ask wrapped adapter
            final int adjPosition = position - numHeaders;
            final int adapterViewType = wrappedAdapter.getItemViewType(adjPosition);
            if (adapterViewType < 0) {
                throw new IllegalArgumentException("base adapter must not return negative values for ViewType!");
            }
            return adapterViewType;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(final @NonNull ViewGroup parent, final int viewType) {
            if (viewType <= TYPE_HEADER_BASE) {
                final int headerIndex = TYPE_HEADER_BASE - viewType;
                final View headerView = headerViews.get(headerIndex);
                return new StaticViewHolder(headerView);
            }
            return wrappedAdapter.onCreateViewHolder(parent, viewType);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onBindViewHolder(final @NonNull RecyclerView.ViewHolder holder, final int position) {
            final int numHeaders = headerViews.size();
            if (position >= numHeaders) {
                final int adjPosition = position - numHeaders;
                wrappedAdapter.onBindViewHolder(holder, adjPosition);
            }
            // header views don't need view binding
        }

        @Override
        public int getItemCount() {
            return headerViews.size() + wrappedAdapter.getItemCount();
        }

        private static class StaticViewHolder extends RecyclerView.ViewHolder {
            StaticViewHolder(final @NonNull View itemView) {
                super(itemView);
            }
        }
    }

}
