package cgeo.geocaching.activity;

import butterknife.ButterKnife;

import cgeo.geocaching.R;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class SimpleWebviewActivity extends AbstractActionBarActivity {

    private WebView webview;

    class SimpleWebviewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
            webview.loadUrl(url);
            return true;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.internal_browser);

        webview = ButterKnife.findById(this, R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setWebViewClient(new SimpleWebviewClient());
        webview.loadUrl(String.valueOf(getIntent().getData()));
    }
}
