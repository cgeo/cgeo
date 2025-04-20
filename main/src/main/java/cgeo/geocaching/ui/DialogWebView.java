package cgeo.geocaching.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** A WebView usable in (Alert)Dialogs */
public class DialogWebView extends WebView {
    public DialogWebView(@NonNull final Context context) {
        super(context);
    }

    public DialogWebView(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    public DialogWebView(@NonNull final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DialogWebView(@NonNull final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /** This override is important so soft keyboard is shown on text input fields in WebView in Dialogs */
    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }
}
