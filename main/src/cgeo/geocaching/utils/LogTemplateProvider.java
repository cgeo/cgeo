package cgeo.geocaching.utils;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.Formatter;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

/**
 * Provides all the available templates for logging.
 *
 */
public final class LogTemplateProvider {

    private LogTemplateProvider() {
        // utility class
    }

    /**
     * Context aware data container for log templates.
     * <p>
     * Some log templates need additional information. To provide that information, it can be encapsulated in this log
     * context.
     * </p>
     *
     */
    public static class LogContext {
        private Geocache cache;
        private Trackable trackable;
        private boolean offline = false;
        private LogEntry logEntry;

        public LogContext(final Geocache cache, LogEntry logEntry) {
            this(cache, logEntry, false);
        }

        public LogContext(final Trackable trackable, final LogEntry logEntry) {
            this.trackable = trackable;
            this.logEntry = logEntry;
        }

        public LogContext(final Geocache cache, LogEntry logEntry, final boolean offline) {
            this.cache = cache;
            this.offline = offline;
            this.logEntry = logEntry;
        }

        public final Geocache getCache() {
            return cache;
        }

        public final Trackable getTrackable() {
            return trackable;
        }

        public final boolean isOffline() {
            return offline;
        }

        public final LogEntry getLogEntry() {
            return logEntry;
        }
    }

    public abstract static class LogTemplate {
        private final String template;
        private final int resourceId;

        protected LogTemplate(final String template, final int resourceId) {
            this.template = template;
            this.resourceId = resourceId;
        }

        public abstract String getValue(LogContext context);

        public final int getResourceId() {
            return resourceId;
        }

        public final int getItemId() {
            return template.hashCode();
        }

        public final String getTemplateString() {
            return template;
        }

        protected final String apply(final String input, final LogContext context) {
            final String bracketedTemplate = "[" + template + "]";

            // check containment first to not unconditionally call the getValue(...) method
            if (input.contains(bracketedTemplate)) {
                return StringUtils.replace(input, bracketedTemplate, getValue(context));
            }
            return input;
        }
    }

    public static ArrayList<LogTemplate> getTemplates() {
        ArrayList<LogTemplate> templates = new ArrayList<LogTemplateProvider.LogTemplate>();
        templates.add(new LogTemplate("DATE", R.string.init_signature_template_date) {

            @Override
            public String getValue(final LogContext context) {
                return Formatter.formatFullDate(System.currentTimeMillis());
            }
        });
        templates.add(new LogTemplate("TIME", R.string.init_signature_template_time) {

            @Override
            public String getValue(final LogContext context) {
                return Formatter.formatTime(System.currentTimeMillis());
            }
        });
        templates.add(new LogTemplate("DATETIME", R.string.init_signature_template_datetime) {

            @Override
            public String getValue(final LogContext context) {
                final long currentTime = System.currentTimeMillis();
                return Formatter.formatFullDate(currentTime) + " " + Formatter.formatTime(currentTime);
            }
        });
        templates.add(new LogTemplate("USER", R.string.init_signature_template_user) {

            @Override
            public String getValue(final LogContext context) {
                return Settings.getUsername();
            }
        });
        templates.add(new LogTemplate("NUMBER", R.string.init_signature_template_number) {

            @Override
            public String getValue(final LogContext context) {
                final Geocache cache = context.getCache();
                if (cache == null) {
                    return StringUtils.EMPTY;
                }

                int current = 0;
                final IConnector connector = ConnectorFactory.getConnector(cache);
                if (connector instanceof ILogin) {
                    current = ((ILogin) connector).getCachesFound();
                }

                // try updating the login information, if the counter is zero
                if (current == 0) {
                    if (context.isOffline()) {
                        return StringUtils.EMPTY;
                    }
                    if (connector instanceof ILogin) {
                        ((ILogin) connector).login(null, null);
                        current = ((ILogin) connector).getCachesFound();
                    }
                }

                if (current >= 0) {
                    return String.valueOf(current + 1);
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("OWNER", R.string.init_signature_template_owner) {

            @Override
            public String getValue(final LogContext context) {
                Trackable trackable = context.getTrackable();
                if (trackable != null) {
                    return trackable.getOwner();
                }
                Geocache cache = context.getCache();
                if (cache != null) {
                    return cache.getOwnerDisplayName();
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("NAME", R.string.init_signature_template_name) {
            @Override
            public String getValue(LogContext context) {
                Trackable trackable = context.getTrackable();
                if (trackable != null) {
                    return trackable.getName();
                }
                Geocache cache = context.getCache();
                if (cache != null) {
                    return cache.getName();
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("URL", R.string.init_signature_template_url) {

            @Override
            public String getValue(LogContext context) {
                Trackable trackable = context.getTrackable();
                if (trackable != null) {
                    return trackable.getBrowserUrl();
                }
                Geocache cache = context.getCache();
                if (cache != null) {
                    return cache.getBrowserUrl();
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("LOG", R.string.init_signature_template_log) {
            @Override
            public String getValue(LogContext context) {
                LogEntry logEntry = context.getLogEntry();
                if (logEntry != null) {
                    return logEntry.getDisplayText();
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("SIGNATURE", R.string.init_signature) {
            @Override
            public String getValue(LogContext context) {
                return StringUtils.defaultString(Settings.getSignature());
            }
        });
        return templates;
    }

    public static LogTemplate getTemplate(final int itemId) {
        for (LogTemplate template : getTemplates()) {
            if (template.getItemId() == itemId) {
                return template;
            }
        }
        return null;
    }

    public static String applyTemplates(final String signature, final LogContext context) {
        if (signature == null) {
            return StringUtils.EMPTY;
        }
        String result = signature;
        for (LogTemplate template : getTemplates()) {
            result = template.apply(result, context);
        }
        return result;
    }
}
