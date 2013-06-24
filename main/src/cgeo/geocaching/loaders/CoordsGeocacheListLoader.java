package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.OldSettings;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.geopoint.Geopoint;

import android.content.Context;

public class CoordsGeocacheListLoader extends AbstractSearchLoader {
    private final Geopoint coords;

    public CoordsGeocacheListLoader(Context context, Geopoint coords) {
        super(context);
        this.coords = coords;
    }

    @Override
    public SearchResult runSearch() {

        SearchResult search = new SearchResult();
        if (OldSettings.isGCConnectorActive()) {
            search = GCParser.searchByCoords(coords, OldSettings.getCacheType(), OldSettings.isShowCaptcha(), this);
        }

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
