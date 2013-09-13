package cgeo.geocaching;

import cgeo.geocaching.network.StatusUpdater.Status;
import cgeo.geocaching.utils.IObserver;
import cgeo.geocaching.utils.Log;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class StatusFragment extends Fragment {

    private ViewGroup status;
    private ImageView statusIcon;
    private TextView statusMessage;

    final private StatusHandler statusHandler = new StatusHandler();

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        status = (ViewGroup) inflater.inflate(R.layout.status, container, false);
        statusIcon = (ImageView) status.findViewById(R.id.status_icon);
        statusMessage = (TextView) status.findViewById(R.id.status_message);
        return status;
    }

    @Override
    public void onResume() {
        super.onResume();
        CgeoApplication.getInstance().getStatusUpdater().addObserver(statusHandler);
    }

    @Override
    public void onPause() {
        CgeoApplication.getInstance().getStatusUpdater().deleteObserver(statusHandler);
        super.onPause();
    }

    private class StatusHandler extends Handler implements IObserver<Status> {

        @Override
        public void update(final Status data) {
            obtainMessage(0, data).sendToTarget();
        }

        @Override
        public void handleMessage(final Message msg) {
            final Status data = (Status) msg.obj;
            updateDisplay(data != null && data.message != null ? data : Status.defaultStatus());
        }

        private void updateDisplay(final Status data) {

            if (data == null) {
                status.setVisibility(View.INVISIBLE);
                return;
            }

            final Resources res = getResources();
            final String packageName = getActivity().getPackageName();

            if (data.icon != null) {
                final int iconId = res.getIdentifier(data.icon, "drawable", packageName);
                if (iconId != 0) {
                    statusIcon.setImageResource(iconId);
                    statusIcon.setVisibility(View.VISIBLE);
                } else {
                    Log.w("StatusHandler: could not find icon corresponding to @drawable/" + data.icon);
                    statusIcon.setVisibility(View.GONE);
                }
            } else {
                statusIcon.setVisibility(View.GONE);
            }

            String message = data.message;
            if (data.messageId != null) {
                final int messageId = res.getIdentifier(data.messageId, "string", packageName);
                if (messageId != 0) {
                    message = res.getString(messageId);
                }
            }

            statusMessage.setText(message);
            status.setVisibility(View.VISIBLE);

            if (data.url != null) {
                status.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(data.url)));
                    }
                });
            } else {
                status.setClickable(false);
            }
        }

    }
}
