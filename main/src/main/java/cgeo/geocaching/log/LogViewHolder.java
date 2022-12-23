package cgeo.geocaching.log;

import cgeo.geocaching.databinding.LogsItemBinding;
import cgeo.geocaching.ui.AbstractViewHolder;

import android.view.View;

public class LogViewHolder extends AbstractViewHolder {
    private int position;
    protected final LogsItemBinding binding;

    public LogViewHolder(final View rowView) {
        super(rowView);
        binding = LogsItemBinding.bind(rowView);
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
     * @param position the cursor position
     */
    public void setPosition(final int position) {
        this.position = position;
    }
}
