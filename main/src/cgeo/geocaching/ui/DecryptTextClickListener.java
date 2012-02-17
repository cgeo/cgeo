package cgeo.geocaching.ui;

import cgeo.geocaching.utils.CryptUtils;

import android.text.Spannable;
import android.view.View;
import android.widget.TextView;

public class DecryptTextClickListener implements View.OnClickListener {

    public void onClick(View view) {
        if (view == null) {
            return;
        }

        try {
            final TextView logView = (TextView) view;
            CharSequence text = logView.getText();
            if (text instanceof Spannable) {
                Spannable span = (Spannable) text;
                logView.setText(CryptUtils.rot13(span));
            }
            else {
                String string = (String) text;
                logView.setText(CryptUtils.rot13(string));
            }
        } catch (Exception e) {
            // nothing
        }
    }
}
