package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.geopoint.Geopoint;

import android.content.Context;

public class CoordsGeocacheListLoader extends AbstractSearchLoader {
    private Geopoint coords;

    public CoordsGeocacheListLoader(Context context, Geopoint coords) {
        super(context);
        this.coords = coords;
    }

    @Override
    public SearchResult runSearch() {
        SearchResult search = GCParser.searchByCoords(coords, Settings.getCacheType(), Settings.isShowCaptcha(), this);

        for (ISearchByCenter centerConn : ConnectorFactory.getSearchByCenterConnectors()) {
            if (centerConn.isActivated()) {
                SearchResult temp = centerConn.searchByCenter(coords);
                if (temp != null) {
                    search.addGeocodes(temp.getGeocodes());
                }
            }
        }

        return search;
    }

}
