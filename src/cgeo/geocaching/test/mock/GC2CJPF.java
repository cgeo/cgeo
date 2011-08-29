package cgeo.geocaching.test.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import cgeo.geocaching.ICache;

public class GC2CJPF implements ICache {
	
	@Override
	public Float getDifficulty() {
		return 2.5f;
	}

	@Override
	public String getGeocode() {
		return "GC2CJPF";
	}

	@Override
	public String getLatitute() {
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
	public String getSize() {
		return "small";
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
	public String getData() {
		try {
			InputStream is = this.getClass().getResourceAsStream("/cgeo/geocaching/test/mock/"+getGeocode()+".txt");
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}

			buffer.flush();
			return buffer.toString();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
		
	}
}
		