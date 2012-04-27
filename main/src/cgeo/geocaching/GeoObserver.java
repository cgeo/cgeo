package cgeo.geocaching;

import cgeo.geocaching.utils.IObserver;

public abstract class GeoObserver implements IObserver<IGeoData> {

    abstract protected void updateLocation(final IGeoData geo);

    @Override
    public void update(final IGeoData geo) {
        updateLocation(geo);
    }
}
