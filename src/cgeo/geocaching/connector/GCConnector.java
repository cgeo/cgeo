package cgeo.geocaching.connector;

import org.apache.commons.lang3.StringUtils;

import cgeo.geocaching.cgCache;

public class GCConnector extends AbstractConnector implements IConnector {

	@Override
	public boolean canHandle(String geocode) {
		return StringUtils.isNotBlank(geocode) && geocode.toUpperCase().startsWith("GC");
	}

	@Override
	public boolean supportsRefreshCache(cgCache cache) {
		return true;
	}

	@Override
	public String getCacheUrl(cgCache cache) {
		return "http://www.geocaching.com/seek/cache_details.aspx?wp=" + cache.geocode;
	}

	@Override
	public boolean supportsWatchList() {
		return true;
	}

	@Override
	public boolean supportsLogging() {
		return true;
	}
}
