package cgeo.geocaching.connector.gc;

import butterknife.ButterKnife;

import cgeo.geocaching.R;
import cgeo.geocaching.loaders.RecaptchaReceiver;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import org.apache.commons.io.IOUtils;

import rx.Observable;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;
import rx.functions.Func0;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.InputStream;

public class RecaptchaHandler extends Handler {
    final public static int SHOW_CAPTCHA = 1;

    final private Activity activity;
    final private RecaptchaReceiver recaptchaReceiver;

    public RecaptchaHandler(final Activity activity, final RecaptchaReceiver recaptchaReceiver) {
        this.activity = activity;
        this.recaptchaReceiver = recaptchaReceiver;
    }

    private void loadChallenge(final ImageView imageView, final View reloadButton) {
        final Observable<Bitmap> captcha = Observable.defer(new Func0<Observable<? extends Bitmap>>() {
            @Override
            public Observable<? extends Bitmap> call() {
                final String url = "http://www.google.com/recaptcha/api/image?c=" + recaptchaReceiver.getChallenge();
                final InputStream is = Network.getResponseStream(Network.getRequest(url));
                if (is != null) {
                    try {
                        final Bitmap img = BitmapFactory.decodeStream(is);
                        return Observable.from(img);
                    } catch (final Exception e) {
                        Log.e("RecaptchaHandler.getCaptcha", e);
                        return Observable.error(e);
                    } finally {
                        IOUtils.closeQuietly(is);
                    }
                }
                return Observable.empty();
            }
        });
        AndroidObservable.bindActivity(activity, captcha).subscribeOn(RxUtils.networkScheduler).subscribe(new Action1<Bitmap>() {
            @Override
            public void call(final Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(final Throwable throwable) {
                // Do nothing
            }
        });
        reloadButton.setEnabled(true);
    }

    @SuppressLint("InflateParams")
    @Override
    public void handleMessage(final Message msg) {
        if (msg.what == SHOW_CAPTCHA) {
            final AlertDialog.Builder dlg = new AlertDialog.Builder(activity);
            final View view = activity.getLayoutInflater().inflate(R.layout.recaptcha_dialog, null, false);

            final ImageView imageView = ButterKnife.findById(view, R.id.image);

            final ImageButton reloadButton = ButterKnife.findById(view, R.id.button_recaptcha_refresh);
            reloadButton.setEnabled(false);
            reloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    recaptchaReceiver.fetchChallenge();
                    loadChallenge(imageView, reloadButton);
                }
            });

            loadChallenge(imageView, reloadButton);

            dlg.setTitle(activity.getString(R.string.caches_recaptcha_title));
            dlg.setView(view);
            dlg.setNeutralButton(activity.getString(R.string.caches_recaptcha_continue), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int id) {
                    final EditText editText = ButterKnife.findById(view, R.id.text);
                    final String text = editText.getText().toString();
                    recaptchaReceiver.setText(text);
                    dialog.cancel();
                }
            });

            dlg.create().show();
        }
    }

}
