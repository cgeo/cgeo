package cgeo.geocaching;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.os.Handler;
import android.os.Message;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.text.Html;
import android.util.Log;
import android.util.Xml;

public class cgGPXParser {

	private cgeoapplication app = null;
	private int listId = 1;
	private cgSearch search = null;
	private Handler handler = null;
	private cgCache cache = new cgCache();
	private cgTrackable trackable = new cgTrackable();
	private cgLog log = new cgLog();
	private boolean htmlShort = true;
	private boolean htmlLong = true;
	private String type = null;
	private String sym = null;
	private String ns = null;
	private ArrayList<String> nsGCList = new ArrayList<String>();
	private final Pattern patternGeocode = Pattern.compile("(GC[0-9A-Z]+)", Pattern.CASE_INSENSITIVE);
	private String name = null;
	private String cmt = null;
	private String desc = null;

	public cgGPXParser(cgeoapplication appIn, int listIdIn, cgSearch searchIn) {
		app = appIn;
		listId = listIdIn;
		search = searchIn;

		nsGCList.add("http://www.groundspeak.com/cache/1/1"); // PQ 1.1
		nsGCList.add("http://www.groundspeak.com/cache/1/0/1"); // PQ 1.0.1
		nsGCList.add("http://www.groundspeak.com/cache/1/0"); // PQ 1.0
	}

	public long parse(File file, int version, Handler handlerIn) {
		handler = handlerIn;
		if (file == null) {
			return 0l;
		}

		if (version == 11) {
			ns = "http://www.topografix.com/GPX/1/1"; // GPX 1.1
		} else {
			ns = "http://www.topografix.com/GPX/1/0"; // GPX 1.0
		}
		final RootElement root = new RootElement(ns, "gpx");
		final Element waypoint = root.getChild(ns, "wpt");

		// waypoint - attributes
		waypoint.setStartElementListener(new StartElementListener() {

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

			public void end() {
				if (cache.geocode == null || cache.geocode.length() == 0) {
					// try to find geocode somewhere else
					String geocode = null;
					Matcher matcherGeocode = null;

					if (name != null && geocode == null) {
						matcherGeocode = patternGeocode.matcher(name);
						while (matcherGeocode.find()) {
							if (matcherGeocode.groupCount() > 0) {
								geocode = matcherGeocode.group(1);
							}
						}
					}

					if (desc != null && geocode == null) {
						matcherGeocode = patternGeocode.matcher(desc);
						while (matcherGeocode.find()) {
							if (matcherGeocode.groupCount() > 0) {
								geocode = matcherGeocode.group(1);
							}
						}
					}

					if (cmt != null && geocode == null) {
						matcherGeocode = patternGeocode.matcher(cmt);
						while (matcherGeocode.find()) {
							if (matcherGeocode.groupCount() > 0) {
								geocode = matcherGeocode.group(1);
							}
						}
					}

					if (geocode != null && geocode.length() > 0) {
						cache.geocode = geocode;
					}

					geocode = null;
					matcherGeocode = null;
				}

				if (cache.geocode != null && cache.geocode.length() > 0
						&& cache.latitude != null && cache.longitude != null
						&& ((type == null && sym == null)
						|| (type != null && type.indexOf("geocache") > -1)
						|| (sym != null && sym.indexOf("geocache") > -1))) {
					cache.latitudeString = cgBase.formatCoordinate(cache.latitude, "lat", true);
					cache.longitudeString = cgBase.formatCoordinate(cache.longitude, "lon", true);
					if (cache.inventory != null) {
						cache.inventoryItems = cache.inventory.size();
					} else {
						cache.inventoryItems = 0;
					}
					cache.reason = listId;
					cache.updated = new Date().getTime();
					cache.detailedUpdate = new Date().getTime();
					cache.detailed = true;

					app.addCacheToSearch(search, cache);
				}

				if (handler != null) {
					final Message msg = new Message();
					msg.obj = search.getCount();
					handler.sendMessage(msg);
				}

				htmlShort = true;
				htmlLong = true;
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
		waypoint.getChild(ns, "time").setEndTextElementListener(new EndTextElementListener() {

			public void end(String body) {
				try {
					cache.hidden = cgBase.dateGPXIn.parse(body.trim());
				} catch (Exception e) {
					Log.w(cgSettings.tag, "Failed to parse cache date: " + e.toString());
				}
			}
		});

		// waypoint.name
		waypoint.getChild(ns, "name").setEndTextElementListener(new EndTextElementListener() {

			public void end(String body) {
				name = body;

				final String content = Html.fromHtml(body).toString().trim();
				cache.name = content;
				if (cache.name.length() > 2 && cache.name.substring(0, 2).equalsIgnoreCase("GC")) {
					cache.geocode = cache.name.toUpperCase();
				}
			}
		});

		// waypoint.desc
		waypoint.getChild(ns, "desc").setEndTextElementListener(new EndTextElementListener() {

			public void end(String body) {
				desc = body;

				final String content = Html.fromHtml(body).toString().trim();
				cache.shortdesc = content;
			}
		});

		// waypoint.cmt
		waypoint.getChild(ns, "cmt").setEndTextElementListener(new EndTextElementListener() {

			public void end(String body) {
				cmt = body;

				final String content = Html.fromHtml(body).toString().trim();
				cache.description = content;
			}
		});

		// waypoint.type
		waypoint.getChild(ns, "type").setEndTextElementListener(new EndTextElementListener() {

			public void end(String body) {
				final String[] content = body.split("\\|");
				if (content.length > 0) {
					type = content[0].toLowerCase().trim();
				}
			}
		});

		// waypoint.sym
		waypoint.getChild(ns, "sym").setEndTextElementListener(new EndTextElementListener() {

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
		final Element cacheParent = version == 11 ? waypoint.getChild(ns, "extensions") : waypoint;

		for (String nsGC : nsGCList) {
			// waypoints.cache
			final Element gcCache = cacheParent.getChild(nsGC, "cache");

			gcCache.setStartElementListener(new StartElementListener() {

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

				public void end(String body) {
					final String content = Html.fromHtml(body).toString().trim();
					cache.name = validate(content);
				}
			});

			// waypoint.cache.owner
			gcCache.getChild(nsGC, "owner").setEndTextElementListener(new EndTextElementListener() {

				public void end(String body) {
					final String content = Html.fromHtml(body).toString().trim();
					cache.owner = validate(content);
				}
			});

			// waypoint.cache.type
			gcCache.getChild(nsGC, "type").setEndTextElementListener(new EndTextElementListener() {

				public void end(String body) {
					String parsedString = validate(body.toLowerCase());
					setType(parsedString);
				}
			});

			// waypoint.cache.container
			gcCache.getChild(nsGC, "container").setEndTextElementListener(new EndTextElementListener() {

				public void end(String body) {
					final String content = body.toLowerCase();
					cache.size = validate(content);
				}
			});

			// waypoint.cache.difficulty
			gcCache.getChild(nsGC, "difficulty").setEndTextElementListener(new EndTextElementListener() {

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

				public void end(String body) {
					if (cache.location == null || cache.location.length() == 0) {
						cache.location = validate(body.trim());
					} else {
						cache.location = cache.location + ", " + body.trim();
					}
				}
			});

			// waypoint.cache.state
			gcCache.getChild(nsGC, "state").setEndTextElementListener(new EndTextElementListener() {

				public void end(String body) {
					if (cache.location == null || cache.location.length() == 0) {
						cache.location = validate(body.trim());
					} else {
						cache.location = body.trim() + ", " + cache.location;
					}
				}
			});

			// waypoint.cache.encoded_hints
			gcCache.getChild(nsGC, "encoded_hints").setEndTextElementListener(new EndTextElementListener() {

				public void end(String body) {
					cache.hint = validate(body.trim());
				}
			});

			// waypoint.cache.short_description
			gcCache.getChild(nsGC, "short_description").setStartElementListener(new StartElementListener() {

				public void start(Attributes attrs) {
					try {
						if (attrs.getIndex("html") > -1) {
							final String at = attrs.getValue("html");
							if (at.equalsIgnoreCase("false")) {
								htmlShort = false;
							}
						}
					} catch (Exception e) {
						// nothing
					}
				}
			});

			gcCache.getChild(nsGC, "short_description").setEndTextElementListener(new EndTextElementListener() {

				public void end(String body) {
					if (!htmlShort) {
						cache.shortdesc = Html.fromHtml(body).toString();
					} else {
						cache.shortdesc = body;
					}
				}
			});

			// waypoint.cache.long_description
			gcCache.getChild(nsGC, "long_description").setStartElementListener(new StartElementListener() {

				public void start(Attributes attrs) {
					try {
						if (attrs.getIndex("html") > -1) {
							if (attrs.getValue("html").equalsIgnoreCase("false")) {
								htmlLong = false;
							}
						}
					} catch (Exception e) {
						// nothing
					}
				}
			});

			gcCache.getChild(nsGC, "long_description").setEndTextElementListener(new EndTextElementListener() {

				public void end(String body) {
					if (htmlLong == false) {
						cache.description = Html.fromHtml(body).toString().trim();
					} else {
						cache.description = body;
					}
				}
			});

			// waypoint.cache.travelbugs
			final Element gcTBs = gcCache.getChild(nsGC, "travelbugs");

			// waypoint.cache.travelbugs.travelbug
			gcTBs.getChild(nsGC, "travelbug").setStartElementListener(new StartElementListener() {

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

				public void end() {
					if (trackable.geocode != null && trackable.geocode.length() > 0 && trackable.name != null && trackable.name.length() > 0) {
						if (cache.inventory == null)
							cache.inventory = new ArrayList<cgTrackable>();
						cache.inventory.add(trackable);
					}
				}
			});

			// waypoint.cache.travelbugs.travelbug.name
			gcTB.getChild(nsGC, "name").setEndTextElementListener(new EndTextElementListener() {

				public void end(String body) {
					String content = Html.fromHtml(body).toString();
					trackable.name = content;
				}
			});

			// waypoint.cache.logs
			final Element gcLogs = gcCache.getChild(nsGC, "logs");

			// waypoint.cache.log
			final Element gcLog = gcLogs.getChild(nsGC, "log");

			gcLog.setStartElementListener(new StartElementListener() {

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

				public void end() {
					if (log.log != null && log.log.length() > 0) {
						if (cache.logs == null)
							cache.logs = new ArrayList<cgLog>();
						cache.logs.add(log);
					}
				}
			});

			// waypoint.cache.logs.log.date
			gcLog.getChild(nsGC, "date").setEndTextElementListener(new EndTextElementListener() {

				public void end(String body) {
					try {
						log.date = cgBase.dateGPXIn.parse(body.trim()).getTime();
					} catch (Exception e) {
						Log.w(cgSettings.tag, "Failed to parse log date: " + e.toString());
					}
				}
			});

			// waypoint.cache.logs.log.type
			gcLog.getChild(nsGC, "type").setEndTextElementListener(new EndTextElementListener() {

				public void end(String body) {
					final String content = body.trim().toLowerCase();
					if (cgBase.logTypes0.containsKey(content)) {
						log.type = cgBase.logTypes0.get(content);
					} else {
						log.type = 4;
					}
				}
			});

			// waypoint.cache.logs.log.finder
			gcLog.getChild(nsGC, "finder").setEndTextElementListener(new EndTextElementListener() {

				public void end(String body) {
					String content = Html.fromHtml(body).toString();
					log.author = content;
				}
			});

			// waypoint.cache.logs.log.finder
			gcLog.getChild(nsGC, "text").setEndTextElementListener(new EndTextElementListener() {

				public void end(String body) {
					String content = Html.fromHtml(body).toString();
					log.log = content;
				}
			});
		}
		FileInputStream fis = null;
		boolean parsed = false;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			Log.e(cgSettings.tag, "Cannot parse .gpx file " + file.getAbsolutePath() + " as GPX " + version + ": file not found!");
		}
		try {
			Xml.parse(fis, Xml.Encoding.UTF_8, root.getContentHandler());
			parsed = true;
		} catch (IOException e) {
			Log.e(cgSettings.tag, "Cannot parse .gpx file " + file.getAbsolutePath() + " as GPX " + version + ": could not read file!");
		} catch (SAXException e) {
			Log.e(cgSettings.tag, "Cannot parse .gpx file " + file.getAbsolutePath() + " as GPX " + version + ": could not parse XML - " + e.toString());
		}
		try {
			fis.close();
		} catch (IOException e) {
			Log.e(cgSettings.tag, "Error after parsing .gpx file " + file.getAbsolutePath() + " as GPX " + version + ": could not close file!");
		}
		return parsed ? search.getCurrentId() : 0l;
	}

	protected String validate(String input) {
		if ("nil".equalsIgnoreCase(input)) {
			return "";
		}
		return input;
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
}
