package cgeo.geocaching.details;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.ui.AbstractCachingPageViewCreator;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.lang.ref.WeakReference;

/**
 * View creator for the browser tab of the cache details. Lazily loads the cache website, when the tab becomes active.
 */
public class WebViewCreator extends AbstractCachingPageViewCreator<WebView> {

    private final WeakReference<CacheDetailActivity> activityRef;
    private boolean hasLoaded = false;

    public WebViewCreator(final CacheDetailActivity activity) {
        this.activityRef = new WeakReference<>(activity);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public WebView getDispatchedView(final ViewGroup parentView) {
        final CacheDetailActivity activity = activityRef.get();
        if (activity == null) {
            return null;
        }
        view = (WebView) activity.getLayoutInflater().inflate(R.layout.cachedetail_webview_page, parentView, false);
        final WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        allowUnsecureImages(settings);
        view.setWebViewClient(new CacheDetailsWebViewClient());
        view.setWebChromeClient(new CacheDetailsWebChromeClient());

        return view;
    }

    /**
     * Http images are not loaded in our https website, starting with Lollipop.
     */
    @SuppressLint("NewApi")
    private static void allowUnsecureImages(final WebSettings settings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    /**
     * Load given URL lazily, but only if this webview has not loaded anything yet.
     */
    public void load(final String url) {
        if (hasLoaded) {
            return;
        }
        final WebView webView = (WebView) getView(null);
        if (webView == null) {
            return;
        }
        webView.loadUrl(url);
        hasLoaded = true;
    }

}
