package cgeo.geocaching.connector;

import cgeo.geocaching.cgCache;

/**
 * connector for OpenCaching.com
 *
 */
public class OXConnector extends AbstractConnector implements IConnector {

	@Override
	public boolean canHandle(String geocode) {
		return geocode != null && geocode.toUpperCase().startsWith("OX");
	}

	@Override
	public String getCacheUrl(cgCache cache) {
		return "http://www.opencaching.com/#!geocache/" + cache.geocode;
	}

}
