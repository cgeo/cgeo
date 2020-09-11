package cgeo.geocaching.ui;

import cgeo.geocaching.ui.dialog.ContextMenuDialog;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.text.Spannable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class DecryptTextClickListener implements View.OnClickListener, Action1<ContextMenuDialog.Item> {

    @NonNull private final TextView targetView;

    public DecryptTextClickListener(@NonNull final TextView targetView) {
        this.targetView = targetView;
    }

    @Override
    public final void onClick(final View view) {
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

    @Override
    public void call(final ContextMenuDialog.Item item) {
        onClick(null);
    }
}
