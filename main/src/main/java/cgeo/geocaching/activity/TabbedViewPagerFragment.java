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

public abstract class TabbedViewPagerFragment<ViewBindingClass extends ViewBinding> extends Fragment {
    protected ViewBindingClass binding;
    protected ViewGroup container;
    private boolean contentIsUpToDate = false;
    private final Integer mutextContentIsUpToDate = 0;

    public TabbedViewPagerFragment() {
        Log.d("new fragment for " + getClass());
    }

    public void setClickListener(final View view, final String url) {
        final Activity activity = getActivity();
        if (activity != null) {
            view.setOnClickListener(v -> ShareUtils.openUrl(activity, url));
        }
    }

    @Override
    public void onAttach(@NonNull final Activity activity) {
        super.onAttach(activity);
        // notify TabbedViewPagerActivity to help it rebuild the fragment cache
        ((TabbedViewPagerActivity) activity).registerFragment(getPageId(), this);
    }

    public abstract ViewBindingClass createView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

    public abstract void setContent();

    public abstract long getPageId();

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        binding = createView(inflater, container, savedInstanceState);
        binding.getRoot().setVisibility(View.GONE);
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
            // do an update anyway to catch situations where currently active view gets updated (and thus no onResume gets called)
            setContent();
            contentIsUpToDate = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        synchronized (mutextContentIsUpToDate) {
            Log.d("onResume: update=" + (!contentIsUpToDate) + " " + getClass().getName());
            if (!contentIsUpToDate) {
                setContent();
                contentIsUpToDate = true;
            }
        }
    }

    public void reinitializeTitle() {
        if (getActivity() instanceof TabbedViewPagerActivity) {
            ((TabbedViewPagerActivity) getActivity()).reinitializeTitle(getPageId());
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
