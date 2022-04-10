package cgeo.geocaching.service;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.os.Build;

import androidx.core.provider.FontRequest;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;

public class EmojiDownloadService {

    private final EmojiCompat.InitCallback initCallback = new EmojiCompat.InitCallback() {
        @Override
        public void onInitialized() {
            super.onInitialized();
            Log.d("EmojiService succeeded in loading fonts");

        }

        @Override
        public void onFailed(final Throwable throwable) {
            super.onFailed(throwable);
            Log.d("EmojiService failed to load fonts", throwable);
        }
    };

    private final Context context;

    public EmojiDownloadService(final Context context) {
        this.context = context;
    }

    public void init() {
        final FontRequest fontRequest = new FontRequest(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            "Noto Color Emoji Compat",
            R.array.com_google_android_gms_fonts_certs);
        final FontRequestEmojiCompatConfig fontRequestConfig = new FontRequestEmojiCompatConfig(context, fontRequest);
        fontRequestConfig.registerInitCallback(initCallback);
        // On recent Androids we assume to have the latest emojis
        fontRequestConfig.setReplaceAll(Build.VERSION.SDK_INT < Build.VERSION_CODES.O);
        EmojiCompat.init(fontRequestConfig);
    }
}
