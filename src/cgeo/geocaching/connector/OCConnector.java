package cgeo.geocaching.connector;

import cgeo.geocaching.cgCache;

public class OCConnector extends AbstractConnector implements IConnector {
	@Override
	public boolean canHandle(String geocode) {
		return geocode != null && geocode.toUpperCase().startsWith("OC");
	}

	@Override
	public String getCacheUrl(cgCache cache) {
		return "http://www.opencaching.de/viewcache.php?wp=" + cache.geocode;
	}
}
