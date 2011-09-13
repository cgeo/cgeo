package cgeo.geocaching.test.mock;

import cgeo.geocaching.enumerations.CacheSize;


public class GC2CJPF extends MockedCache {

	@Override
	public Float getDifficulty() {
		return 2.5f;
	}

	@Override
	public String getGeocode() {
		return "GC2CJPF";
	}

	@Override
	public String getLatitude() {
		return "N 52° 25.504";
	}

	@Override
	public String getLongitude() {
		return "E 009° 39.852";
	}

	@Override
	public String getOwner() {
		return "Tom03";
	}
	@Override
	public String getOwnerReal() {
		return getOwner();
	}

	@Override
	public CacheSize getSize() {
		return CacheSize.SMALL;
	}

	@Override
	public Float getTerrain() {
		return 2.0f;
	}

	@Override
	public String getType() {
		return "multi";
	}

	@Override
	public boolean isArchived() {
		return false;
	}

	@Override
	public boolean isDisabled() {
		return false;
	}

	@Override
	public boolean isMembersOnly() {
		return false;
	}

	@Override
	public boolean isOwn() {
		return false;
	}


	@Override
	public String getHint() {
		return "Das Final (unter Steinen) ist mit GC gekennzeichnet.";
	}

	@Override
	public String getDescription() {
		return "Kleiner Multi über 7 Stationen";
	}

	@Override
	public String getShortDescription() {
		return "Von Nachwuchs-Cachern für Nachwuchs-Cacher.";
	}

	@Override
	public String getName() {
		return "Kinderwald KiC";
	}

}
