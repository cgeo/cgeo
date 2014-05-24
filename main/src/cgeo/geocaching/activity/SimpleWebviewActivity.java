package cgeo.geocaching.activity;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import cgeo.geocaching.R;

public class SimpleWebviewActivity extends AbstractActionBarActivity {

    private WebView webview;

    class SimplelWebviewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            webview.loadUrl(url);
            return true;
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.internal_browser);

        webview = (WebView) findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setWebViewClient(new SimplelWebviewClient());
        webview.loadUrl(String.valueOf(getIntent().getData()));
    }
}
