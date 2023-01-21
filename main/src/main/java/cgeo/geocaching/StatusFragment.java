package cgeo.geocaching;

import cgeo.geocaching.databinding.StatusBinding;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class StatusFragment extends Fragment {

    private final CompositeDisposable statusSubscription = new CompositeDisposable();
    private StatusBinding binding;

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = StatusBinding.inflate(getLayoutInflater(), container, false);
        final ViewGroup statusGroup = binding.getRoot();
        statusSubscription.add(AndroidRxUtils.bindFragment(this, StatusUpdater.LATEST_STATUS)
                .subscribe(status -> {
                    if (status == Status.NO_STATUS) {
                        statusGroup.setVisibility(View.GONE);
                        return;
                    }

                    final Resources res = getResources();
                    final String packageName = getActivity().getPackageName();

                    if (status.icon != null) {
                        final int iconId = res.getIdentifier(status.icon, "drawable", packageName);
                        if (iconId != 0) {
                            binding.statusIcon.setImageResource(iconId);
                            binding.statusIcon.setVisibility(View.VISIBLE);
                        } else {
                            Log.w("StatusHandler: could not find icon corresponding to @drawable/" + status.icon);
                            binding.statusIcon.setVisibility(View.GONE);
                        }
                    } else {
                        binding.statusIcon.setVisibility(View.GONE);
                    }

                    String message = status.message;
                    if (status.messageId != null) {
                        final int messageId = res.getIdentifier(status.messageId, "string", packageName);
                        if (messageId != 0) {
                            message = res.getString(messageId);
                        }
                    }

                    binding.statusMessage.setText(message);
                    statusGroup.setVisibility(View.VISIBLE);

                    if (status.url != null) {
                        statusGroup.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(status.url)))); // don't use ShareUtils here. E.g. we want to redirect directly to PlayStore even if "use WebView" is set
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
    }

}
