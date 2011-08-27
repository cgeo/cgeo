package cgeo.geocaching.connector;

import cgeo.geocaching.cgCache;

public final class ConnectorFactory {
	private static final GCConnector GC_CONNECTOR = new GCConnector();
	private static final IConnector[] connectors = new IConnector[] {GC_CONNECTOR, new OCConnector(), new OXConnector()};

	public static IConnector[] getConnectors() {
		return connectors;
	}

	public static boolean canHandle(final String geocode) {
		for (IConnector connector : connectors) {
			if (connector.canHandle(geocode)) {
				return true;
			}
		}
		return false;
	}

	public static IConnector getConnector(cgCache cache) {
		for (IConnector connector : connectors) {
			if (connector.canHandle(cache.geocode)) {
				return connector;
			}
		}
		// in case of errors, assume GC as default
		return GC_CONNECTOR;
	}
}
