package cgeo.geocaching.activity;

import cgeo.geocaching.utils.ShareUtils;

import android.app.Activity;
import android.view.View;

import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;

public class AVPFragment extends Fragment {
    protected WeakReference<Activity> activityWeakReference = null;

    public void setActivity(final Activity activity) {
        this.activityWeakReference = new WeakReference<>(activity);
    }

    public void setClickListener(final View view, final String url) {
        final Activity activity = activityWeakReference.get();
        if (activity != null) {
            view.setOnClickListener(v -> ShareUtils.openUrl(activity, url));
        }
    }

}
