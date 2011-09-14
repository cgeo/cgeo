package cgeo.geocaching;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class cgSearch {
    private UUID id;
    private List<String> geocodes = new ArrayList<String>();

    public String error = null;
    public String url = "";
    public String[] viewstates = null;
    public int totalCnt = 0;

    public cgSearch() {
        id = UUID.randomUUID();
    }

    public UUID getCurrentId() {
        return id;
    }

    public List<String> getGeocodes() {
        return geocodes;
    }

    public int getCount() {
        return geocodes.size();
    }

    public void addGeocode(String geocode) {
        if (geocodes == null) {
            geocodes = new ArrayList<String>();
        }

        geocodes.add(geocode);
    }
}
