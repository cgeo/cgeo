package cgeo.geocaching;

import java.util.HashMap;


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
			return input.replaceAll("\\[" + template + "\\]", getValue(base));
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
					final String page = base.request(false, "www.geocaching.com", "/my/", "GET", params, false, false, false).getData();
					int current = cgBase.parseFindCount(page);

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

}
