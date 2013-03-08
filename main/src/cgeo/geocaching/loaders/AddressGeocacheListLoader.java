package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.connector.gc.GCParser;

import android.content.Context;

public class AddressGeocacheListLoader extends AbstractSearchLoader {

    private String address;

    public AddressGeocacheListLoader(Context context, String address) {
        super(context);
        this.address = address;
    }

    @Override
    public SearchResult runSearch() {
        return GCParser.searchByAddress(address, Settings.getCacheType(), Settings.isShowCaptcha(), this);
    }

}
