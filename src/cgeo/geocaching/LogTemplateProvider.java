package cgeo.geocaching;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;


/**
 * provides all the available templates for logging
 *
 */
public class LogTemplateProvider {
	public static abstract class LogTemplate {
		private String template;
		private int resourceId;

		public LogTemplate(String template, int resourceId) {
			this.template = template;
			this.resourceId = resourceId;
		}

		abstract String getValue(cgBase base);

		public int getResourceId() {
			return resourceId;
		}

		public int getItemId() {
			return template.hashCode();
		}

		public String getTemplateString() {
			return template;
		}

		protected String apply(String input, cgBase base) {
			if (input.contains("[" + template + "]")) {
				return input.replaceAll("\\[" + template + "\\]", getValue(base));
			}
			return input;
		}
	}

	private static LogTemplate[] templates;

	public static LogTemplate[] getTemplates() {
		if (templates == null) {
			templates = new LogTemplate[] {
					new LogTemplate("DATE", R.string.init_signature_template_date) {

				@Override
				String getValue(final cgBase base) {
					return base.formatFullDate(System.currentTimeMillis());
				}
			},
			new LogTemplate("TIME", R.string.init_signature_template_time) {

				@Override
				String getValue(final cgBase base) {
					return base.formatTime(System.currentTimeMillis());
				}
			},
            new LogTemplate("DATETIME", R.string.init_signature_template_datetime) {

                @Override
                String getValue(final cgBase base) {
                    final long currentTime = System.currentTimeMillis();
                    return base.formatFullDate(currentTime) + " " + base.formatTime(currentTime);
                }
            },
			new LogTemplate("USER", R.string.init_signature_template_user) {

				@Override
				String getValue(final cgBase base) {
					return base.getUserName();
				}
			},
			new LogTemplate("NUMBER", R.string.init_signature_template_number) {

				@Override
				String getValue(final cgBase base) {
					String findCount = "";
					final HashMap<String, String> params = new HashMap<String, String>();
					final String page = base.request(false, "www.geocaching.com", "/email/", "GET", params, false, false, false).getData();
					int current = parseFindCount(page);

					if (current >= 0) {
						findCount = String.valueOf(current + 1);
					}
					return findCount;
				}
			}
			};
		}
		return templates;
	}

	public static LogTemplate getTemplate(int itemId) {
		for (LogTemplate template : getTemplates()) {
			if (template.getItemId() == itemId) {
				return template;
			}
		}
		return null;
	}

	public static String applyTemplates(String signature, cgBase base) {
		if (signature == null) {
			return "";
		}
		String result = signature;
		for (LogTemplate template : getTemplates()) {
			result = template.apply(result, base);
		}
		return result;
	}
	
	private static int parseFindCount(String page) {
		if (page == null || page.length() == 0) {
			return -1;
		}

		int findCount = -1;

		try {
			final Pattern findPattern = Pattern.compile("<strong><img.+?icon_smile.+?title=\"Caches Found\" /> ([,\\d]+)", Pattern.CASE_INSENSITIVE);
			final Matcher findMatcher = findPattern.matcher(page);
			if (findMatcher.find()) {
				if (findMatcher.groupCount() > 0) {
					String count = findMatcher.group(1);

					if (count != null) {
						if (count.length() == 0) {
							findCount = 0;
						} else {
							findCount = Integer.parseInt(count.replaceAll(",", ""));
						}
					}
				}
			}
		} catch (Exception e) {
			Log.w(cgSettings.tag, "cgBase.parseFindCount: " + e.toString());
		}

		return findCount;
	}

	

}
