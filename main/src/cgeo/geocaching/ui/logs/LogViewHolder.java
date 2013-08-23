package cgeo.geocaching.ui.logs;

import butterknife.InjectView;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.AbstractViewHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class LogViewHolder extends AbstractViewHolder {
    @InjectView(R.id.added) protected TextView date ;
    @InjectView(R.id.type) protected TextView type;
    @InjectView(R.id.author) protected TextView author;
    @InjectView(R.id.count_or_location) protected TextView countOrLocation;
    @InjectView(R.id.log) protected TextView text;
    @InjectView(R.id.log_images) protected TextView images;
    @InjectView(R.id.log_mark) protected ImageView marker;

    private int position;

    public LogViewHolder(View rowView) {
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