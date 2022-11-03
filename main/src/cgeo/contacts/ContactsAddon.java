package cgeo.contacts;

import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.ProcessUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

public class ContactsAddon {

    private ContactsAddon() {
        // utility class
    }

    public static void openContactCard(@NonNull final Context context, @NonNull final String userName) {
        final Parameters params = new Parameters(
                IContacts.PARAM_NAME, userName
        );

        context.startActivity(new Intent(IContacts.INTENT,
                Uri.parse(IContacts.URI_SCHEME + "://" + IContacts.URI_HOST + "?" + params)));
    }

    public static boolean isAvailable() {
        return ProcessUtils.isIntentAvailable(IContacts.INTENT, Uri.parse(IContacts.URI_SCHEME + "://" + IContacts.URI_HOST));
    }

}
