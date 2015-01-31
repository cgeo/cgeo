package cgeo.geocaching.compatibility;

import android.widget.TextView;

public class AndroidLevel11Emulation implements AndroidLevel11Interface {

    @Override
    public void setTextIsSelectable(final TextView textView, final boolean selectable) {
        // do nothing
    }

}
