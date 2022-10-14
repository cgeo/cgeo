package cgeo.geocaching.log;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.FoundNumCounter;
import cgeo.geocaching.utils.Formatter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Provides all the available templates for logging.
 */
public final class LogTemplateProvider {

    private static final String TEMPLATE_LOCATION_ACCURACY_FORMAT = "%s (±%s)";

    private LogTemplateProvider() {
        // utility class
    }

    /**
     * Context aware data container for log templates.
     * <p>
     * Some log templates need additional information. To provide that information, it can be encapsulated in this log
     * context.
     * </p>
     */
    public static class LogContext {
        private Geocache cache;
        private Trackable trackable;
        private boolean offline = false;
        private final LogEntry logEntry;

        public LogContext(final Geocache cache, final LogEntry logEntry) {
            this(cache, logEntry, false);
        }

        public LogContext(final Trackable trackable, final LogEntry logEntry) {
            this.trackable = trackable;
            this.logEntry = logEntry;
        }

        public LogContext(final Geocache cache, final LogEntry logEntry, final boolean offline) {
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
        @StringRes
        private int resourceId = 0;
        private String name = "";

        protected LogTemplate(final String template, @StringRes final int resourceId) {
            this.template = template;
            this.resourceId = resourceId;
        }

        protected LogTemplate(final String template, final String name) {
            this.template = template;
            this.name = name;
        }

        public abstract String getValue(LogContext context);

        @StringRes
        public final int getResourceId() {
            return resourceId;
        }

        public final int getItemId() {
            return template.hashCode();
        }

        public final String getName() {
            return name;
        }

        public final String getTemplateString() {
            return template;
        }

        @NonNull
        private String apply(@NonNull final String input, final LogContext context) {
            final String bracketedTemplate = "[" + template + "]";

            // check containment first to not unconditionally call the getValue(...) method
            if (input.contains(bracketedTemplate)) {
                return StringUtils.replace(input, bracketedTemplate, getValue(context));
            }
            return input;
        }
    }

    /**
     * @return all user-facing templates, but not the signature template itself
     */
    @NonNull
    public static List<LogTemplate> getTemplatesWithoutSignature() {
        final List<LogTemplate> templates = new ArrayList<>();
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
        templates.add(new LogTemplate("DAYOFWEEK", R.string.init_signature_template_day_of_week) {

            @Override
            public String getValue(final LogContext context) {
                return Formatter.formatDayOfWeek(System.currentTimeMillis());
            }
        });
        templates.add(new LogTemplate("USER", R.string.init_signature_template_user) {

            @Override
            public String getValue(final LogContext context) {
                final Geocache cache = context.getCache();
                if (cache != null) {
                    final IConnector connector = ConnectorFactory.getConnector(cache);
                    if (connector instanceof ILogin) {
                        return ((ILogin) connector).getUserName();
                    }
                }
                return Settings.getUserName();
            }
        });
        templates.add(new LogTemplate("NUMBER", R.string.init_signature_template_number) {

            @Override
            public String getValue(final LogContext context) {

                final boolean increment;
                increment = null == context.logEntry || context.logEntry.logType == LogType.FOUND_IT || context.logEntry.logType == LogType.ATTENDED || context.logEntry.logType == LogType.WEBCAM_PHOTO_TAKEN;

                final Geocache cache = context.getCache();
                if (cache == null || !(ConnectorFactory.getConnector(cache) instanceof ILogin)) {
                    return StringUtils.EMPTY;
                }
                final ILogin connector = (ILogin) ConnectorFactory.getConnector(cache);

                int counter;
                final String onlineNum = getCounter(context, false); // we increment the counter later on our self in this method.

                if (!onlineNum.equals(StringUtils.EMPTY)) {
                    counter = Integer.parseInt(onlineNum);
                } else {
                    counter = FoundNumCounter.getAndUpdateFoundNum(connector);
                }

                if (counter == -1) {
                    return StringUtils.EMPTY;
                }

                counter += DataStore.getFoundsOffline(connector);

                if (increment) {
                    counter += 1;
                }
                return String.valueOf(counter);
            }
        });
        templates.add(new LogTemplate("ONLINENUM", R.string.init_signature_template_number_legacy) {

            @Override
            public String getValue(final LogContext context) {

                if (null == context.logEntry || context.logEntry.logType == LogType.FOUND_IT || context.logEntry.logType == LogType.ATTENDED || context.logEntry.logType == LogType.WEBCAM_PHOTO_TAKEN) {
                    return getCounter(context, true);
                } else {
                    return getCounter(context, false);
                }
            }
        });
        templates.add(new LogTemplate("OWNER", R.string.init_signature_template_owner) {

            @Override
            public String getValue(final LogContext context) {
                final Trackable trackable = context.getTrackable();
                if (trackable != null) {
                    return trackable.getOwner();
                }
                final Geocache cache = context.getCache();
                if (cache != null) {
                    return cache.getOwnerDisplayName();
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("NAME", R.string.init_signature_template_name) {
            @Override
            public String getValue(final LogContext context) {
                final Trackable trackable = context.getTrackable();
                if (trackable != null) {
                    return trackable.getName();
                }
                final Geocache cache = context.getCache();
                if (cache != null) {
                    return cache.getName();
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("DIFFICULTY", R.string.init_signature_template_difficulty) {
            @Override
            public String getValue(final LogContext context) {
                final Geocache cache = context.getCache();
                if (cache != null) {
                    return String.valueOf(cache.getDifficulty());
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("TERRAIN", R.string.init_signature_template_terrain) {
            @Override
            public String getValue(final LogContext context) {
                final Geocache cache = context.getCache();
                if (cache != null) {
                    return String.valueOf(cache.getTerrain());
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("SIZE", R.string.init_signature_template_size) {
            @Override
            public String getValue(final LogContext context) {
                final Geocache cache = context.getCache();
                if (cache != null) {
                    return cache.getSize().getL10n();
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("URL", R.string.init_signature_template_url) {

            @Override
            public String getValue(final LogContext context) {
                final Trackable trackable = context.getTrackable();
                if (trackable != null) {
                    return trackable.getUrl();
                }
                final Geocache cache = context.getCache();
                if (cache != null) {
                    return StringUtils.defaultString(cache.getUrl());
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("TYPE", R.string.init_signature_template_type) {
            @Override
            public String getValue(final LogContext context) {
                final Geocache cache = context.getCache();
                if (cache != null) {
                    final CacheType cacheType = cache.getType();
                    return cacheType.getL10n();
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("GEOCODE", R.string.init_signature_template_geocode) {
            @Override
            public String getValue(final LogContext context) {
                final Geocache cache = context.getCache();
                if (cache != null) {
                    return cache.getGeocode();
                }
                return StringUtils.EMPTY;
            }
        });
        return templates;
    }

    @NonNull
    private static String getCounter(final LogContext context, final boolean incrementCounter) {
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
                ((ILogin) connector).login();
                current = ((ILogin) connector).getCachesFound();
            }
        }

        if (current >= 0) {
            return String.valueOf(incrementCounter ? current + 1 : current);
        }
        return StringUtils.EMPTY;
    }

    /**
     * @return all user-facing templates, including the signature template
     */
    @NonNull
    public static List<LogTemplate> getTemplatesWithSignature() {
        final List<LogTemplate> templates = getTemplatesWithoutSignature();
        int index = 0;
        templates.add(index++, new LogTemplate("SIGNATURE", R.string.init_signature) {
            @Override
            public String getValue(final LogContext context) {
                final String nestedTemplate = Settings.getSignature();
                if (StringUtils.contains(nestedTemplate, "SIGNATURE")) {
                    return "invalid signature template";
                }
                return applyTemplates(nestedTemplate, context);
            }
        });
        for (Settings.PrefLogTemplate template : Settings.getLogTemplates()) {
            templates.add(index++, new LogTemplate("TEMPLATE" + template.getKey(), template.getTitle()) {
                @Override
                public String getValue(final LogContext context) {
                    if (StringUtils.contains(template.getText(), "TEMPLATE" + template.getKey())) {
                        return "invalid template";
                    }
                    return applyTemplates(template.getText(), context);
                }
            });
        }
        // Add the location log template to the bottom of the list instead of at a certain index
        templates.add(new LogTemplate("LOCATION", R.string.init_signature_template_location) {
            @Override
            public String getValue(final LogContext context) {
                final GeoData geo = Sensors.getInstance().currentGeo();
                return String.format(TEMPLATE_LOCATION_ACCURACY_FORMAT, geo.getCoords(), Units.getDistanceFromMeters(geo.getAccuracy()));
            }
        });
        return templates;
    }

    /**
     * @return all user-facing templates, including the log text template
     */
    @NonNull
    public static List<LogTemplate> getTemplatesWithSignatureAndLogText() {
        final List<LogTemplate> templates = getTemplatesWithSignature();
        templates.add(new LogTemplate("LOG", R.string.init_signature_template_log) {
            @Override
            public String getValue(final LogContext context) {
                final LogEntry logEntry = context.getLogEntry();
                if (logEntry != null) {
                    return logEntry.getDisplayText();
                }
                return StringUtils.EMPTY;
            }
        });
        return templates;
    }

    @NonNull
    private static List<LogTemplate> getAllTemplates() {
        final List<LogTemplate> templates = getTemplatesWithSignatureAndLogText();
        templates.add(new LogTemplate("NUMBER$NOINC", -1 /* Never user facing */) {
            @Override
            public String getValue(final LogContext context) {
                return getCounter(context, false);
            }
        });
        return templates;
    }

    @Nullable
    public static LogTemplate getTemplate(final int itemId) {
        for (final LogTemplate template : getAllTemplates()) {
            if (template.getItemId() == itemId) {
                return template;
            }
        }
        return null;
    }

    public static String applyTemplates(@NonNull final String signature, final LogContext context) {
        String result = signature;
        for (final LogTemplate template : getAllTemplates()) {
            result = template.apply(result, context);
        }
        return result;
    }

    public static String applyTemplatesNoIncrement(@NonNull final String signature, final LogContext context) {
        return applyTemplates(signature.replace("[NUMBER]", "[NUMBER$NOINC]").replace("[ONLINENUM]", "[NUMBER$NOINC]"), context);
    }
}
