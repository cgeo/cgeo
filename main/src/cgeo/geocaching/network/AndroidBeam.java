package cgeo.geocaching.network;

import cgeo.geocaching.CgeoApplication;

import org.apache.commons.io.Charsets;
import org.eclipse.jdt.annotation.Nullable;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.Build;

/**
 * utility class managing all NFC related tasks
 */
public class AndroidBeam {

    private AndroidBeam() {
        // utility class
    }

    /**
     * returns the URI transmitted via Android Beam, or the URI contained in the data of the intent
     */
    @Nullable
    public static Uri getUri(final Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            final NdefMessage msg = (NdefMessage) intent.getExtras().getParcelableArray(NfcAdapter.EXTRA_NDEF_MESSAGES)[0];
            return Uri.parse("http://" + new String(msg.getRecords()[0].getPayload(), Charsets.UTF_8));
        }
        return intent.getData();
    }

    // Do not support older devices than Android 4.0
    // Although there even are 2.3 devices  (Nexus S)
    // these are so few that we don't want to deal with the older (non Android Beam) API

    public interface ActivitySharingInterface {
        /** Return an URL that represent the current activity for sharing or null for no sharing. */
        @Nullable
        public String getAndroidBeamUri();
    }

    public static void enable(final Activity activity, final ActivitySharingInterface sharingInterface) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            initializeICSAndroidBeam(activity, createMessageCallback(sharingInterface));
        }
    }

    public static void disable(final Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            initializeICSAndroidBeam(activity, null);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static void initializeICSAndroidBeam(final Activity activity, final CreateNdefMessageCallback messageCallback) {
        final NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter == null) {
            return;
        }
        nfcAdapter.setNdefPushMessageCallback(messageCallback, activity);

    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static CreateNdefMessageCallback createMessageCallback(final ActivitySharingInterface sharingInterface) {
        return new NfcAdapter.CreateNdefMessageCallback() {
            @Override
            public NdefMessage createNdefMessage(final NfcEvent event) {
                final String uri = sharingInterface.getAndroidBeamUri();
                if (uri == null) {
                    return null;
                }
                final NdefRecord[] records = {
                        NdefRecord.createUri(uri),
                        NdefRecord.createApplicationRecord(CgeoApplication.getInstance().getPackageName())
                };
                return new NdefMessage(records);
            }
        };
    }

}
