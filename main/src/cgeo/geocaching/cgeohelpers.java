package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

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

    public void installManual() {
        final Locale loc = Locale.getDefault();
        final String language = loc.getLanguage();

        if ("de".equalsIgnoreCase(language)) {
            installFromMarket("gnu.android.app.cgeomanual.de");
        }
        else {
            installFromMarket("gnu.android.app.cgeomanual.en");
        }
    }

    public void installLocus() {
        installFromMarket("menion.android.locus");
    }

    public void installGpsStatus() {
        installFromMarket("com.eclipsim.gpsstatus2");
    }

    public void installBluetoothGps() {
        installFromMarket("googoo.android.btgps");
    }
}
