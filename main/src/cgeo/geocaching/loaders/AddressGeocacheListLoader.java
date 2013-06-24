package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.OldSettings;
import cgeo.geocaching.connector.gc.GCParser;

import android.content.Context;

public class AddressGeocacheListLoader extends AbstractSearchLoader {

    private final String address;

    public AddressGeocacheListLoader(Context context, String address) {
        super(context);
        this.address = address;
    }

    @Override
    public SearchResult runSearch() {
        return GCParser.searchByAddress(address, OldSettings.getCacheType(), OldSettings.isShowCaptcha(), this);
    }

}
