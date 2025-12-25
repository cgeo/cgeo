// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.activity

import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.ShareUtils

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class TabbedViewPagerFragment<ViewBindingClass : ViewBinding()> : Fragment() {
    protected ViewBindingClass binding
    protected ViewGroup container
    private var contentIsUpToDate: Boolean = false
    private val mutextContentIsUpToDate: Integer = 0

    public TabbedViewPagerFragment() {
        Log.d("fragment for " + getClass())
    }

    public Unit setClickListener(final View view, final String url) {
        val activity: Activity = getActivity()
        if (activity != null) {
            view.setOnClickListener(v -> ShareUtils.openUrl(activity, url))
        }
    }

    override     public Unit onAttach(final Activity activity) {
        super.onAttach(activity)
        // notify TabbedViewPagerActivity to help it rebuild the fragment cache
        ((TabbedViewPagerActivity) activity).registerFragment(getPageId(), this)
    }

    public abstract ViewBindingClass createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)

    public abstract Unit setContent()

    public abstract Long getPageId()

    override     public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        binding = createView(inflater, container, savedInstanceState)
        binding.getRoot().setVisibility(View.GONE)
        this.container = container
        synchronized (mutextContentIsUpToDate) {
            contentIsUpToDate = false
            setContent()
            contentIsUpToDate = true
        }
        return binding.getRoot()
    }

    public Unit notifyDataSetChanged() {
        synchronized (mutextContentIsUpToDate) {
            contentIsUpToDate = false
            // do an update anyway to catch situations where currently active view gets updated (and thus no onResume gets called)
            setContent()
            contentIsUpToDate = true
        }
    }

    override     public Unit onResume() {
        super.onResume()
        synchronized (mutextContentIsUpToDate) {
            Log.d("onResume: update=" + (!contentIsUpToDate) + " " + getClass().getName())
            if (!contentIsUpToDate) {
                setContent()
                contentIsUpToDate = true
            }
        }
    }

    public Unit reinitializeTitle() {
        if (getActivity() is TabbedViewPagerActivity) {
            ((TabbedViewPagerActivity) getActivity()).reinitializeTitle(getPageId())
        }
    }


    // Fragment lifecycle methods - for testing purposes

    /*

    override     public Unit onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        Log.e("onCreate " + getClass().toString())
    }

    // onCreateView(), see above

    override     public Unit onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("onViewCreated " + getClass().toString())
    }

    override     public Unit onStart() {
        super.onStart()
        Log.e("onStart " + getClass().toString())
    }

    // onResume(), see above

    override     public Unit onPause() {
        Log.e("onPause " + getClass().toString())
        super.onPause()
    }

    override     public Unit onStop() {
        Log.e("onStop " + getClass().toString())
        super.onStop()
    }

    override     public Unit onDestroyView() {
        Log.e("onDestroyView " + getClass().toString())
        super.onDestroyView()
    }

    override     public Unit onDestroy() {
        Log.e("onDestroy " + getClass().toString())
        super.onDestroy()
    }

    */
}
