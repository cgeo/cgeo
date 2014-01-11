package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.settings.Settings;

import android.content.Context;

public class AddressGeocacheListLoader extends AbstractSearchLoader {

    private final String address;

    public AddressGeocacheListLoader(Context context, String address) {
        super(context);
        this.address = address;
    }

    @Override
    public SearchResult runSearch() {
        return GCParser.searchByAddress(address, Settings.getCacheType(), Settings.isShowCaptcha(), this);
    }

}
