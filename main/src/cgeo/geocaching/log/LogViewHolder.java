package cgeo.geocaching.log;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.AbstractViewHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;

public class LogViewHolder extends AbstractViewHolder {
    @BindView(R.id.added) protected TextView date ;
    @BindView(R.id.type) protected TextView type;
    @BindView(R.id.author) protected TextView author;
    @BindView(R.id.count_or_location) protected TextView countOrLocation;
    @BindView(R.id.gcinfo) protected TextView gcinfo;
    @BindView(R.id.log) protected TextView text;
    @BindView(R.id.log_images) protected TextView images;
    @BindView(R.id.log_mark) protected ImageView marker;
    @BindView(R.id.detail_box) protected View detailBox;

    private int position;

    public LogViewHolder(final View rowView) {
        super(rowView);
    }

    /**
     * Read the position of the cursor pointed to by this holder. <br/>
     * This must be called by the UI thread.
     *
     * @return the cursor position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Set the position of the cursor pointed to by this holder. <br/>
     * This must be called by the UI thread.
     *
     * @param position
     *            the cursor position
     */
    public void setPosition(final int position) {
        this.position = position;
    }
}
