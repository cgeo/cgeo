package cgeo.geocaching.details;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.ResourcesUtils;

import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.commons.lang3.StringUtils;

class CacheDetailsWebViewClient extends WebViewClient {

    @Override
    public void onPageFinished(final WebView view, final String url) {
        // remove everything except user content of the cache
        view.loadUrl("javascript:" + StringUtils.replace(ResourcesUtils.getRawResourceString(R.raw.cache_details_webview), "\r\n", ""));
    }

}
