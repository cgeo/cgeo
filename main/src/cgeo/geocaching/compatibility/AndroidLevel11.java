package cgeo.geocaching.compatibility;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AndroidLevel11 implements AndroidLevel11Interface {

    @Override
    public void setTextIsSelectable(final TextView textView, final boolean selectable) {
        textView.setTextIsSelectable(selectable);
    }

}
