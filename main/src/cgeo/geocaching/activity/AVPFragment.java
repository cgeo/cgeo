package cgeo.geocaching.activity;

import cgeo.geocaching.utils.ShareUtils;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import java.lang.ref.WeakReference;

public abstract class AVPFragment<ViewBindingClass extends ViewBinding> extends Fragment {
    protected WeakReference<Activity> activityWeakReference = null;
    protected ViewBindingClass binding;
    protected ViewGroup container;
    private boolean contentIsUpToDate = false;

    public void setActivity(final Activity activity) {
        this.activityWeakReference = new WeakReference<>(activity);
    }

    public void setClickListener(final View view, final String url) {
        final Activity activity = activityWeakReference.get();
        if (activity != null) {
            view.setOnClickListener(v -> ShareUtils.openUrl(activity, url));
        }
    }

    public abstract ViewBindingClass createView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);
    public abstract void setContent();

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        binding = createView(inflater, container, savedInstanceState);
        this.container = container;
        contentIsUpToDate = false;
        setContent();
        contentIsUpToDate = true;
        return binding.getRoot();
    }

    public void notifyDataSetChanged() {
        contentIsUpToDate = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!contentIsUpToDate) {
            setContent();
            contentIsUpToDate = true;
        }
    }
}
