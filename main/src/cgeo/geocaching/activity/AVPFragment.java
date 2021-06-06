package cgeo.geocaching.activity;

import cgeo.geocaching.utils.Log;
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
    private final Integer mutextContentIsUpToDate = 0;

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
        synchronized (mutextContentIsUpToDate) {
            contentIsUpToDate = false;
            setContent();
            contentIsUpToDate = true;
        }
        return binding.getRoot();
    }

    public void notifyDataSetChanged() {
        synchronized (mutextContentIsUpToDate) {
            contentIsUpToDate = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        synchronized (mutextContentIsUpToDate) {
            Log.e("onResume: update=" + (!contentIsUpToDate) + " " + getClass().getName());
            if (!contentIsUpToDate) {
                setContent();
                contentIsUpToDate = true;
            }
        }
    }


    // Fragment lifecycle methods - for testing purposes

    /*

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("onCreate " + getClass().toString());
    }

    // onCreateView(), see above

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.e("onViewCreated " + getClass().toString());
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e("onStart " + getClass().toString());
    }

    // onResume(), see above

    @Override
    public void onPause() {
        Log.e("onPause " + getClass().toString());
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e("onStop " + getClass().toString());
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.e("onDestroyView " + getClass().toString());
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.e("onDestroy " + getClass().toString());
        super.onDestroy();
    }

    */
}
