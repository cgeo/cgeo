package cgeo.contacts;

import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.ProcessUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

/** This class "connects" cgeo with the cgeo-contacts addon */
//TODO: class can be deleted when minSDK=23, see #13777
public class ContactsAddon {

    //Connector constants
    private static final String CONTACTS_INTENT = "cgeo.contacts.FIND";
    private static final String CONTACTS_URI_SCHEME = "find";
    private static final String CONTACTS_URI_HOST = "cgeo.org";
    private static final String CONTACTS_PARAM_NAME = "name"; // user name

    private ContactsAddon() {
        // utility class
    }

    public static void openContactCard(@NonNull final Context context, @NonNull final String userName) {
        final Parameters params = new Parameters(
                CONTACTS_PARAM_NAME, userName
        );

        context.startActivity(new Intent(CONTACTS_INTENT,
                Uri.parse(CONTACTS_URI_SCHEME + "://" + CONTACTS_URI_HOST + "?" + params)));
    }

    public static boolean isAvailable() {
        return ProcessUtils.isIntentAvailable(CONTACTS_INTENT, Uri.parse(CONTACTS_URI_SCHEME + "://" + CONTACTS_URI_HOST));
    }

}
