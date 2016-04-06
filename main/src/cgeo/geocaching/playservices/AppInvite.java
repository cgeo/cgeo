package cgeo.geocaching.playservices;

import android.app.Activity;
import android.content.Intent;

import com.google.android.gms.appinvite.AppInviteInvitation;

import org.eclipse.jdt.annotation.NonNull;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.utils.ProcessUtils;

public class AppInvite {

    private AppInvite() {
        // prevents calls from subclass throw new UnsupportedOperationException();
    }

    public static final boolean isAvailable() {
        return ProcessUtils.isIntentAvailable("com.google.android.gms.appinvite.ACTION_APP_INVITE");
    }

    public static void send(@NonNull final Activity activity) {
        final Intent intent = new AppInviteInvitation.IntentBuilder(activity.getString(R.string.invitation_title))
                .setMessage(activity.getString(R.string.invitation_message))
                .build();
        activity.startActivityForResult(intent, Intents.APP_INVITE_REQUEST_CODE);
    }


}
