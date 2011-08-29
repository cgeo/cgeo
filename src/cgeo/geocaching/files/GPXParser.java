package cgeo.geocaching.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.os.Handler;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgLog;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgTrackable;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.connector.ConnectorFactory;

public abstract class GPXParser extends FileParser {

	private static final SimpleDateFormat formatSimple = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // 2010-04-20T07:00:00Z
	private static final SimpleDateFormat formatTimezone = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000'Z"); // 2010-04-20T01:01:03.000-04:00

	private static final Pattern patternGeocode = Pattern.compile("([A-Z]{2}[0-9A-Z]+)", Pattern.CASE_INSENSITIVE);
	private static final String[] nsGCList = new String[] {
		"http://www.groundspeak.com/cache/1/1", // PQ 1.1
		"http://www.groundspeak.com/cache/1/0/1", // PQ 1.0.1
		"http://www.groundspeak.com/cache/1/0", // PQ 1.0
	};

	private cgeoapplication app = null;
	private int listId = 1;
	private cgSearch search = null;
	protected String namespace = null;
	private String version;
	private Handler handler = null;

	private cgCache cache = new cgCache();
	private cgTrackable trackable = new cgTrackable();
	private cgLog log = new cgLog();

	private String type = null;
	private String sym = null;
	private String name = null;
	private String cmt = null;
	private String desc = null;

	public GPXParser(cgeoapplication appIn, int listIdIn, cgSearch searchIn, String namespaceIn, String versionIn) {
		app = appIn;
		listId = listIdIn;
		search = searchIn;
		namespace = namespaceIn;
		version = versionIn;
	}

	private static Date parseDate(String input) throws ParseException {
		input = input.trim();
		if (input.length() >= 3 && input.charAt(input.length() - 3) == ':') {
			String removeColon = input.substring(0, input.length() - 3) + input.substring(input.length() - 2);
			return formatTimezone.parse(removeColon);
		}
		return formatSimple.parse(input);
	}

	public long parse(final InputStream stream, Handler handlerIn) {
		handler = handlerIn;

		final RootElement root = new RootElement(namespace, "gpx");
		final Element waypoint = root.getChild(namespace, "wpt");

		// waypoint - attributes
		waypoint.setStartElementListener(new StartElementListener() {

			@Override
			public void start(Attributes attrs) {
				try {
					if (attrs.getIndex("lat") > -1) {
						cache.latitude = new Double(attrs.getValue("lat"));
					}
					if (attrs.getIndex("lon") > -1) {
						cache.longitude = new Double(attrs.getValue("lon"));
					}
				} catch (Exception e) {
					Log.w(cgSettings.tag, "Failed to parse waypoint's latitude and/or longitude.");
				}
			}
		});

		// waypoint
		waypoint.setEndElementListener(new EndElementListener() {

			@Override
			public void end() {
				if (cache.geocode == null || cache.geocode.length() == 0) {
					// try to find geocode somewhere else
					findGeoCode(name);
					findGeoCode(desc);
					findGeoCode(cmt);
				}

				if (cache.geocode != null && cache.geocode.length() > 0
						&& cache.latitude != null && cache.longitude != null
						&& ((type == null && sym == null)
						|| (type != null && type.indexOf("geocache") > -1)
						|| (sym != null && sym.indexOf("geocache") > -1))) {
					fixCache(cache);
					cache.reason = listId;
					cache.detailed = true;

					app.addCacheToSearch(search, cache);
				}

				showFinishedMessage(handler, search);

				type = null;
				sym = null;
				name = null;
				desc = null;
				cmt = null;

				cache = null;
				cache = new cgCache();
			}
		});

		// waypoint.time
		waypoint.getChild(namespace, "time").setEndTextElementListener(new EndTextElementListener() {

			@Override
			public void end(String body) {
				try {
					cache.hidden = parseDate(body);
				} catch (Exception e) {
					Log.w(cgSettings.tag, "Failed to parse cache date: " + e.toString());
				}
			}
		});

		// waypoint.name
		waypoint.getChild(namespace, "name").setEndTextElementListener(new EndTextElementListener() {

			@Override
			public void end(String body) {
				name = body;

				final String content = body.trim();
				cache.name = content;

				findGeoCode(cache.name);
				findGeoCode(cache.description);
			}
		});

		// waypoint.desc
		waypoint.getChild(namespace, "desc").setEndTextElementListener(new EndTextElementListener() {

			@Override
			public void end(String body) {
				desc = body;

				cache.shortdesc = validate(body);
			}
		});

		// waypoint.cmt
		waypoint.getChild(namespace, "cmt").setEndTextElementListener(new EndTextElementListener() {

			@Override
			public void end(String body) {
				cmt = body;

				cache.description = validate(body);
			}
		});

		// waypoint.type
		waypoint.getChild(namespace, "type").setEndTextElementListener(new EndTextElementListener() {

			@Override
			public void end(String body) {
				final String[] content = body.split("\\|");
				if (content.length > 0) {
					type = content[0].toLowerCase().trim();
				}
			}
		});

		// waypoint.sym
		waypoint.getChild(namespace, "sym").setEndTextElementListener(new EndTextElementListener() {

			@Override
			public void end(String body) {
				body = body.toLowerCase();
				sym = body;
				if (body.indexOf("geocache") != -1 && body.indexOf("found") != -1) {
					cache.found = true;
				}
			}
		});

		// for GPX 1.0, cache info comes from waypoint node (so called private children,
		// for GPX 1.1 from extensions node
		final Element cacheParent = getCacheParent(waypoint);

		for (String nsGC : nsGCList) {
			// waypoints.cache
			final Element gcCache = cacheParent.getChild(nsGC, "cache");

			gcCache.setStartElementListener(new StartElementListener() {

				@Override
				public void start(Attributes attrs) {
					try {
						if (attrs.getIndex("id") > -1) {
							cache.cacheid = attrs.getValue("id");
						}
						if (attrs.getIndex("archived") > -1) {
							cache.archived = attrs.getValue("archived").equalsIgnoreCase("true");
						}
						if (attrs.getIndex("available") > -1) {
							cache.disabled = !attrs.getValue("available").equalsIgnoreCase("true");
						}
					} catch (Exception e) {
						Log.w(cgSettings.tag, "Failed to parse cache attributes.");
					}
				}
			});

			// waypoint.cache.name
			gcCache.getChild(nsGC, "name").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String cacheName) {
					cache.name = validate(cacheName);
				}
			});

			// waypoint.cache.owner
			gcCache.getChild(nsGC, "owner").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String cacheOwner) {
					cache.owner = validate(cacheOwner);
				}
			});

			// waypoint.cache.type
			gcCache.getChild(nsGC, "type").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String body) {
					setType(validate(body.toLowerCase()));
				}
			});

			// waypoint.cache.container
			gcCache.getChild(nsGC, "container").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String body) {
					cache.size = validate(body.toLowerCase());
				}
			});

			// waypoint.cache.difficulty
			gcCache.getChild(nsGC, "difficulty").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String body) {
					try {
						cache.difficulty = new Float(body);
					} catch (Exception e) {
						Log.w(cgSettings.tag, "Failed to parse difficulty: " + e.toString());
					}
				}
			});

			// waypoint.cache.terrain
			gcCache.getChild(nsGC, "terrain").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String body) {
					try {
						cache.terrain = new Float(body);
					} catch (Exception e) {
						Log.w(cgSettings.tag, "Failed to parse terrain: " + e.toString());
					}
				}
			});

			// waypoint.cache.country
			gcCache.getChild(nsGC, "country").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String country) {
					if (cache.location == null || cache.location.length() == 0) {
						cache.location = validate(country);
					} else {
						cache.location = cache.location + ", " + country.trim();
					}
				}
			});

			// waypoint.cache.state
			gcCache.getChild(nsGC, "state").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String state) {
					if (cache.location == null || cache.location.length() == 0) {
						cache.location = validate(state);
					} else {
						cache.location = state.trim() + ", " + cache.location;
					}
				}
			});

			// waypoint.cache.encoded_hints
			gcCache.getChild(nsGC, "encoded_hints").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String encoded) {
					cache.hint = validate(encoded);
				}
			});

			gcCache.getChild(nsGC, "short_description").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String shortDesc) {
					cache.shortdesc = validate(shortDesc);
				}
			});

			gcCache.getChild(nsGC, "long_description").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String desc) {
					cache.description = validate(desc);
				}
			});

			// waypoint.cache.travelbugs
			final Element gcTBs = gcCache.getChild(nsGC, "travelbugs");

			// waypoint.cache.travelbugs.travelbug
			gcTBs.getChild(nsGC, "travelbug").setStartElementListener(new StartElementListener() {

				@Override
				public void start(Attributes attrs) {
					trackable = new cgTrackable();

					try {
						if (attrs.getIndex("ref") > -1) {
							trackable.geocode = attrs.getValue("ref").toUpperCase();
						}
					} catch (Exception e) {
						// nothing
					}
				}
			});

			// waypoint.cache.travelbug
			final Element gcTB = gcTBs.getChild(nsGC, "travelbug");

			gcTB.setEndElementListener(new EndElementListener() {

				@Override
				public void end() {
					if (trackable.geocode != null && trackable.geocode.length() > 0 && trackable.name != null && trackable.name.length() > 0) {
						if (cache.inventory == null) {
							cache.inventory = new ArrayList<cgTrackable>();
						}
						cache.inventory.add(trackable);
					}
				}
			});

			// waypoint.cache.travelbugs.travelbug.name
			gcTB.getChild(nsGC, "name").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String tbName) {
					trackable.name = validate(tbName);
				}
			});

			// waypoint.cache.logs
			final Element gcLogs = gcCache.getChild(nsGC, "logs");

			// waypoint.cache.log
			final Element gcLog = gcLogs.getChild(nsGC, "log");

			gcLog.setStartElementListener(new StartElementListener() {

				@Override
				public void start(Attributes attrs) {
					log = new cgLog();

					try {
						if (attrs.getIndex("id") > -1) {
							log.id = Integer.parseInt(attrs.getValue("id"));
						}
					} catch (Exception e) {
						// nothing
					}
				}
			});

			gcLog.setEndElementListener(new EndElementListener() {

				@Override
				public void end() {
					if (log.log != null && log.log.length() > 0) {
						if (cache.logs == null) {
							cache.logs = new ArrayList<cgLog>();
						}
						cache.logs.add(log);
					}
				}
			});

			// waypoint.cache.logs.log.date
			gcLog.getChild(nsGC, "date").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String body) {
					try {
						log.date = parseDate(body).getTime();
					} catch (Exception e) {
						Log.w(cgSettings.tag, "Failed to parse log date: " + e.toString());
					}
				}
			});

			// waypoint.cache.logs.log.type
			gcLog.getChild(nsGC, "type").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String body) {
					final String logType = validate(body).toLowerCase();
					if (cgBase.logTypes0.containsKey(logType)) {
						log.type = cgBase.logTypes0.get(logType);
					} else {
						log.type = cgBase.LOG_NOTE;
					}
				}
			});

			// waypoint.cache.logs.log.finder
			gcLog.getChild(nsGC, "finder").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String finderName) {
					log.author = validate(finderName);
				}
			});

			// waypoint.cache.logs.log.text
			gcLog.getChild(nsGC, "text").setEndTextElementListener(new EndTextElementListener() {

				@Override
				public void end(String logText) {
					log.log = validate(logText);
				}
			});
		}
		boolean parsed = false;
		try {
			Xml.parse(stream, Xml.Encoding.UTF_8, root.getContentHandler());
			parsed = true;
		} catch (IOException e) {
			Log.e(cgSettings.tag, "Cannot parse .gpx file as GPX " + version + ": could not read file!");
		} catch (SAXException e) {
			Log.e(cgSettings.tag, "Cannot parse .gpx file as GPX " + version + ": could not parse XML - " + e.toString());
		}
		return parsed ? search.getCurrentId() : 0l;
	}

	private long parse(final File file, final Handler handlerIn) {
		if (file == null) {
			return 0l;
		}

		FileInputStream fis = null;
		long result = 0l;
		try {
			fis = new FileInputStream(file);
			result = parse(fis, handlerIn);
		} catch (FileNotFoundException e) {
			Log.e(cgSettings.tag, "Cannot parse .gpx file " + file.getAbsolutePath() + " as GPX " + version + ": file not found!");
		}
		try {
			if (fis != null) {
				fis.close();
			}
		} catch (IOException e) {
			Log.e(cgSettings.tag, "Error after parsing .gpx file " + file.getAbsolutePath() + " as GPX " + version + ": could not close file!");
		}
		return result;
	}

	protected abstract Element getCacheParent(Element waypoint);

	protected static String validate(String input) {
		if ("nil".equalsIgnoreCase(input)) {
			return "";
		}
		return input.trim();
	}

	private void setType(String parsedString) {
		final String knownType = cgBase.cacheTypes.get(parsedString);
		if (knownType != null) {
			cache.type = knownType;
		}
		else {
			if (cache.type == null || cache.type.length() == 0) {
				cache.type = "mystery"; // default for not recognized types
			}
		}
	}

	private void findGeoCode(final String input) {
		if (input == null || (cache.geocode != null && cache.geocode.length() != 0)) {
			return;
		}
		Matcher matcherGeocode = patternGeocode.matcher(input);
		if (matcherGeocode.find()) {
			if (matcherGeocode.groupCount() > 0) {
				String geocode = matcherGeocode.group(1);
				if (ConnectorFactory.canHandle(geocode)) {
					cache.geocode = geocode;
				}
			}
		}
	}

	public static Long parseGPX(cgeoapplication app, File file, int listId, Handler handler) {
		cgSearch search = new cgSearch();
		long searchId = 0l;

		try {
			GPXParser parser = new GPX10Parser(app, listId, search);
			searchId = parser.parse(file, handler);
			if (searchId == 0l) {
				parser = new GPX11Parser(app, listId, search);
				searchId = parser.parse(file, handler);
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgBase.parseGPX: " + e.toString());
		}

		Log.i(cgSettings.tag, "Caches found in .gpx file: " + app.getCount(searchId));

		return search.getCurrentId();
	}
}
