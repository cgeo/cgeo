package cgeo.geocaching.apps;

import android.content.Context;

public interface App {
    public boolean isInstalled(final Context context);

    public String getName();

    int getId();
}
