package cgeo.geocaching.ui;

import cgeo.geocaching.utils.CryptUtils;

import android.text.Spannable;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class Rot13TextView {

    @NonNull private final TextView targetView;
    private boolean isDecrypted;

    public Rot13TextView(@NonNull final TextView targetView, final boolean initiallyDecrypted) {
        this.targetView = targetView;
        this.isDecrypted = initiallyDecrypted;
        if (isDecrypted) {
            rotate(this.targetView);
        }
    }

    public void setText(final CharSequence text) {
        this.targetView.setText(text);
        if (isDecrypted) {
            rotate(this.targetView);
        }
    }

    public void setText(final CharSequence text, final TextView.BufferType bt) {
        this.targetView.setText(text, bt);
        if (isDecrypted) {
            rotate(this.targetView);
        }
    }

    public void rotate() {
        this.isDecrypted = !this.isDecrypted;
        rotate(this.targetView);
    }

    public static void rotate(final TextView targetView) {
        try {
            final CharSequence text = targetView.getText();
            if (text instanceof Spannable) {
                targetView.setText(CryptUtils.rot13((Spannable) text));
            } else {
                targetView.setText(CryptUtils.rot13((String) text));
            }
        } catch (final RuntimeException ignored) {
            // nothing
        }
    }

}
