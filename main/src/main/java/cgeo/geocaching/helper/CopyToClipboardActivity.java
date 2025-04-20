package cgeo.geocaching.helper;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.ClipboardUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper activity to copy simple content to clipboard
 */
public class CopyToClipboardActivity extends AppCompatActivity {


    @Nullable
    public static Intent createClipboardIntent(final Context context, final CharSequence text, final Uri uri) {
        if (context == null || (text == null && uri == null)) {
            return null;
        }
        final Intent clipboardIntent = new Intent(context, CopyToClipboardActivity.class);
        clipboardIntent.putExtra(Intent.EXTRA_TEXT, text);
        clipboardIntent.putExtra(Intent.EXTRA_STREAM, uri);
        return clipboardIntent;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle = getIntent().getExtras();
        if (bundle != null && !StringUtils.isBlank(bundle.getString(Intent.EXTRA_TEXT))) {
            ClipboardUtils.copyToClipboard(bundle.getString(Intent.EXTRA_TEXT));
            ViewUtils.showToast(this, R.string.clipboard_copy_ok);
        } else if (bundle != null && bundle.getParcelable(Intent.EXTRA_STREAM) != null) {
            ClipboardUtils.copyToClipboard(bundle.getString(Intent.EXTRA_STREAM));
            ViewUtils.showToast(this, R.string.clipboard_copy_ok);
        } else {
            ViewUtils.showToast(this, R.string.clipboard_copy_failed);
        }

        finish();
    }
}
