package cgeo.geocaching.files;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Route;

import android.sax.Element;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

public abstract class GPXMultiParserAbstractTracksRoutes extends GPXMultiParserBase {

    // temporary variables
    protected ArrayList<Geopoint> temp;
    protected ArrayList<Float> tempElevation;
    protected Element points;
    protected Element point;
    protected String namespace;
    protected Route result;

    protected void resetTempData() {
        temp = new ArrayList<>();
        tempElevation = new ArrayList<>();
    }

    protected void setNameAndLatLonParsers() {
/*
        points.getChild(namespace, "name").setEndTextElementListener(result::setName);

        point.setStartElementListener(attrs -> {
            if (attrs.getIndex("lat") > -1 && attrs.getIndex("lon") > -1) {
                final String latitude = attrs.getValue("lat");
                final String longitude = attrs.getValue("lon");
                if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
                    temp.add(new Geopoint(Double.parseDouble(latitude), Double.parseDouble(longitude)));
                }
            }
        });
        point.getChild(namespace, "ele").setEndTextElementListener(el -> tempElevation.add(Float.parseFloat(el)));
*/
    }

    @Override
    public void onParsingDone(@NonNull final Collection<Object> result) {
        result.add(this.result);
    }

}
