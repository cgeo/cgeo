package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import static cgeo.geocaching.utils.SettingsUtils.setPrefSummaryActiveStatus;

import android.os.Bundle;

public class PreferenceServicesFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings_title_services);

        // display connectors' activation status
        setPrefSummaryActiveStatus(this, R.string.pref_fakekey_gc_authorization, Settings.isGCConnectorActive());
        setPrefSummaryActiveStatus(this, R.string.pref_connectorALActive, Settings.isGCConnectorActive() && Settings.isALConnectorActive());
        setPrefSummaryActiveStatus(this, R.string.pref_fakekey_ocde_authorization, Settings.isOCConnectorActive(R.string.pref_connectorOCActive));
        setPrefSummaryActiveStatus(this, R.string.pref_fakekey_ocpl_authorization, Settings.isOCConnectorActive(R.string.pref_connectorOCPLActive));
        setPrefSummaryActiveStatus(this, R.string.pref_fakekey_ocnl_authorization, Settings.isOCConnectorActive(R.string.pref_connectorOCNLActive));
        setPrefSummaryActiveStatus(this, R.string.pref_fakekey_ocus_authorization, Settings.isOCConnectorActive(R.string.pref_connectorOCUSActive));
        setPrefSummaryActiveStatus(this, R.string.pref_fakekey_ocro_authorization, Settings.isOCConnectorActive(R.string.pref_connectorOCROActive));
        setPrefSummaryActiveStatus(this, R.string.pref_fakekey_ocuk_authorization, Settings.isOCConnectorActive(R.string.pref_connectorOCUKActive));
        setPrefSummaryActiveStatus(this, R.string.pref_fakekey_ec_authorization, Settings.isECConnectorActive());
        setPrefSummaryActiveStatus(this, R.string.pref_fakekey_su_authorization, Settings.isSUConnectorActive());
        setPrefSummaryActiveStatus(this, R.string.pref_fakekey_geokrety_authorization, Settings.isGeokretyConnectorActive());
        setPrefSummaryActiveStatus(this, R.string.pref_fakekey_sendtocgeo_info, Settings.isRegisteredForSend2cgeo());
        setPrefSummaryActiveStatus(this, R.string.pref_fakekey_twitter_authorization, Settings.isUseTwitter());
    }
}
