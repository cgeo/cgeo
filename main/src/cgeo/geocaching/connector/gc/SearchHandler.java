package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SearchHandler extends Handler {
    private Activity activity = null;
    private Resources res = null;
    private AbstractSearchThread recaptchaThread = null;
    private ImageView imageView = null;
    private Bitmap img = null;

    private Handler imgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                if (img != null && imageView != null) {
                    imageView.setImageBitmap(img);
                }
            } catch (Exception e) {
                // nothing
            }
        }
    };

    public SearchHandler(Activity activityIn, Resources resIn, AbstractSearchThread recaptchaThreadIn) {
        activity = activityIn;
        res = resIn;
        recaptchaThread = recaptchaThreadIn;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg.what == 1) {
                final AlertDialog.Builder dlg = new AlertDialog.Builder(activity);
                final LayoutInflater inflater = activity.getLayoutInflater();
                final View view = inflater.inflate(R.layout.recaptcha_dialog, null);

                imageView = (ImageView) view.findViewById(R.id.image);

                (new getCaptcha(new URL("http://www.google.com/recaptcha/api/image?c=" + recaptchaThread.getChallenge()))).start();

                dlg.setTitle(res.getString(R.string.caches_recaptcha_title));
                dlg.setView(view);
                dlg.setNeutralButton(res.getString(R.string.caches_recaptcha_continue), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        final String text = ((EditText) view.findViewById(R.id.text)).getText().toString();

                        recaptchaThread.setText(text);

                        dialog.cancel();
                    }
                });

                dlg.create().show();
            }
        } catch (Exception e) {
            // nothing
        }
    }

    private class getCaptcha extends Thread {
        private URL uri = null;

        public getCaptcha(URL uriIn) {
            uri = uriIn;
        }

        @Override
        public void run() {
            try {
                HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
                connection.setDoInput(true);
                connection.connect();

                InputStream is = connection.getInputStream();

                img = BitmapFactory.decodeStream(is);

                is.close();

                imgHandler.sendEmptyMessage(0);
            } catch (IOException e) {
                Log.e("Failed to download reCAPTCHA image");
            }
        }
    }
}
