package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.loaders.RecaptchaReceiver;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.utils.Log;

import org.apache.commons.io.IOUtils;

import rx.Observable;
import rx.android.observables.AndroidObservable;
import rx.schedulers.Schedulers;
import rx.functions.Action1;
import rx.functions.Func0;

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
        getCaptcha().subscribe(new Action1<Bitmap>() {
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

    @Override
    public void handleMessage(Message msg) {
        if (msg.what == SHOW_CAPTCHA) {
            final AlertDialog.Builder dlg = new AlertDialog.Builder(activity);
            final View view = activity.getLayoutInflater().inflate(R.layout.recaptcha_dialog, null);

            final ImageView imageView = (ImageView) view.findViewById(R.id.image);

            final ImageButton reloadButton = (ImageButton) view.findViewById(R.id.button_recaptcha_refresh);
            reloadButton.setEnabled(false);
            reloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    recaptchaReceiver.fetchChallenge();
                    loadChallenge(imageView, reloadButton);
                }
            });

            loadChallenge(imageView, reloadButton);

            dlg.setTitle(activity.getString(R.string.caches_recaptcha_title));
            dlg.setView(view);
            dlg.setNeutralButton(activity.getString(R.string.caches_recaptcha_continue), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    final String text = ((EditText) view.findViewById(R.id.text)).getText().toString();
                    recaptchaReceiver.setText(text);
                    dialog.cancel();
                }
            });

            dlg.create().show();
        }
    }

    private Observable<Bitmap> getCaptcha() {
        return AndroidObservable.bindActivity(activity,
                Observable.defer(new Func0<Observable<? extends Bitmap>>() {
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
                }).subscribeOn(Schedulers.io()));
    }

}
