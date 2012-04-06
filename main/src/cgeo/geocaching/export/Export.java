package cgeo.geocaching.export;

import cgeo.geocaching.cgCache;

import android.app.Activity;

import java.util.List;

public interface Export {
    public void export(List<cgCache> caches, Activity activity);

    public String getName();
}
