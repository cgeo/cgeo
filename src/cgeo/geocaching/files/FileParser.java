package cgeo.geocaching.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import android.os.Handler;
import android.os.Message;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgSearch;

public abstract class FileParser {
	protected static StringBuilder readFile(File file)
			throws FileNotFoundException, IOException {
		StringBuilder buffer = new StringBuilder();
		BufferedReader input =  new BufferedReader(new FileReader(file));
		try {
		  String line = null;
		  while (( line = input.readLine()) != null){
		    buffer.append(line);
		  }
		}
		finally {
		  input.close();
		}
		return buffer;
	}

	static void showFinishedMessage(Handler handler, cgSearch search) {
		if (handler != null) {
			final Message msg = new Message();
			msg.obj = search.getCount();
			handler.sendMessage(msg);
		}
	}

	protected static void fixCache(cgCache cache) {
		cache.latitudeString = cgBase.formatLatitude(cache.latitude, true);
		cache.longitudeString = cgBase.formatLongitude(cache.longitude, true);
		if (cache.inventory != null) {
			cache.inventoryItems = cache.inventory.size();
		} else {
			cache.inventoryItems = 0;
		}
		cache.updated = new Date().getTime();
		cache.detailedUpdate = new Date().getTime();
	}



}
