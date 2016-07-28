package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.loaders.RecaptchaReceiver;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Message;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.InputStream;

import butterknife.ButterKnife;
import org.apache.commons.io.IOUtils;
import rx.Observable;
import rx.android.app.AppObservable;
import rx.functions.Action1;
import rx.functions.Func0;

public class RecaptchaHandler extends WeakReferenceHandler<Activity> {
    public static final int SHOW_CAPTCHA = 1;

    private final RecaptchaReceiver recaptchaReceiver;

    public RecaptchaHandler(final Activity activity, final RecaptchaReceiver recaptchaReceiver) {
        super(activity);
        this.recaptchaReceiver = recaptchaReceiver;
    }

    private void loadChallenge(final ImageView imageView, final View reloadButton, final boolean needsFetch) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        final Observable<Bitmap> captcha = Observable.defer(new Func0<Observable<Bitmap>>() {
            @Override
            public Observable<Bitmap> call() {
                if (needsFetch) {
                    recaptchaReceiver.fetchChallenge();
                }
                final String url = "http://www.google.com/recaptcha/api/image?c=" + recaptchaReceiver.getChallenge();
                final InputStream is = Network.getResponseStream(Network.getRequest(url));
                if (is != null) {
                    try {
                        final Bitmap img = BitmapFactory.decodeStream(is);
                        return Observable.just(img);
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
        AppObservable.bindActivity(activity, captcha).subscribeOn(AndroidRxUtils.networkScheduler).subscribe(new Action1<Bitmap>() {
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
            final Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            final AlertDialog.Builder dlg = new AlertDialog.Builder(activity);
            final View view = activity.getLayoutInflater().inflate(R.layout.recaptcha_dialog, null, false);

            final ImageView imageView = ButterKnife.findById(view, R.id.image);

            final ImageButton reloadButton = ButterKnife.findById(view, R.id.button_recaptcha_refresh);
            reloadButton.setEnabled(false);
            reloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    loadChallenge(imageView, reloadButton, true);
                }
            });

            loadChallenge(imageView, reloadButton, false);

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
