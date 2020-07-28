package cgeo.geocaching;

import cgeo.geocaching.network.StatusUpdater;
import cgeo.geocaching.network.StatusUpdater.Status;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class StatusFragment extends Fragment {

    @BindView(R.id.status_icon)
    protected ImageView statusIcon;
    @BindView(R.id.status_message)
    protected TextView statusMessage;

    private final CompositeDisposable statusSubscription = new CompositeDisposable();
    private Unbinder unbinder;

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final ViewGroup statusGroup = (ViewGroup) inflater.inflate(R.layout.status, container, false);
        unbinder = ButterKnife.bind(this, statusGroup);
        statusSubscription.add(AndroidRxUtils.bindFragment(this, StatusUpdater.LATEST_STATUS)
                .subscribe(status -> {
                    if (status == Status.NO_STATUS) {
                        statusGroup.setVisibility(View.INVISIBLE);
                        return;
                    }

                    final Resources res = getResources();
                    final String packageName = getActivity().getPackageName();

                    if (status.icon != null) {
                        final int iconId = res.getIdentifier(status.icon, "drawable", packageName);
                        if (iconId != 0) {
                            statusIcon.setImageResource(iconId);
                            statusIcon.setVisibility(View.VISIBLE);
                        } else {
                            Log.w("StatusHandler: could not find icon corresponding to @drawable/" + status.icon);
                            statusIcon.setVisibility(View.GONE);
                        }
                    } else {
                        statusIcon.setVisibility(View.GONE);
                    }

                    String message = status.message;
                    if (status.messageId != null) {
                        final int messageId = res.getIdentifier(status.messageId, "string", packageName);
                        if (messageId != 0) {
                            message = res.getString(messageId);
                        }
                    }

                    statusMessage.setText(message);
                    statusGroup.setVisibility(View.VISIBLE);

                    if (status.url != null) {
                        statusGroup.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(status.url))));
                    } else {
                        statusGroup.setClickable(false);
                    }
                }));
        return statusGroup;
    }

    @Override
    public void onDestroyView() {
        statusSubscription.clear();
        super.onDestroyView();
        unbinder.unbind();
    }

}
