package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import java.util.Locale;

public class cgeohelpers extends AbstractActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init
        setTheme();
        setContentView(R.layout.helpers);
        setTitle(res.getString(R.string.helpers));
    }

    @Override
    public void onResume() {
        super.onResume();

        settings.load();
    }

    private void installFromMarket(String marketId) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:" + marketId)));
        } catch (Exception e) {
            // market not available in standard emulator
        }

        finish();
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void installManual(View view) {
        final Locale loc = Locale.getDefault();
        final String language = loc.getLanguage();

        if ("de".equalsIgnoreCase(language)) {
            installFromMarket("gnu.android.app.cgeomanual.de");
        }
        else {
            installFromMarket("gnu.android.app.cgeomanual.en");
        }
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void installLocus(View view) {
        installFromMarket("menion.android.locus");
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void installGpsStatus(View view) {
        installFromMarket("com.eclipsim.gpsstatus2");
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void installBluetoothGps(View view) {
        installFromMarket("googoo.android.btgps");
    }
}
