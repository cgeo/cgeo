// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.log

import cgeo.geocaching.R
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.capability.ILogin
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.location.Units
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.extension.FoundNumCounter
import cgeo.geocaching.utils.Formatter

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat

import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils

/**
 * Provides all the available templates for logging.
 */
class LogTemplateProvider {

    private static val TEMPLATE_LOCATION_ACCURACY_FORMAT: String = "%s (±%s)"

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
        private Geocache cache
        private Trackable trackable
        private var offline: Boolean = false
        private final LogEntry logEntry

        public LogContext(final Geocache cache, final LogEntry logEntry) {
            this(cache, logEntry, false)
        }

        public LogContext(final Trackable trackable, final LogEntry logEntry) {
            this.trackable = trackable
            this.logEntry = logEntry
        }

        public LogContext(final Geocache cache, final LogEntry logEntry, final Boolean offline) {
            this.cache = cache
            this.offline = offline
            this.logEntry = logEntry
        }

        public final Geocache getCache() {
            return cache
        }

        public final Trackable getTrackable() {
            return trackable
        }

        public final Boolean isOffline() {
            return offline
        }

        public final LogEntry getLogEntry() {
            return logEntry
        }
    }

    public abstract static class LogTemplate {
        private final String template
        @StringRes
        private var resourceId: Int = 0
        private var name: String = ""

        protected LogTemplate(final String template, @StringRes final Int resourceId) {
            this.template = template
            this.resourceId = resourceId
        }

        protected LogTemplate(final String template, final String name) {
            this.template = template
            this.name = name
        }

        public abstract String getValue(LogContext context)

        @StringRes
        public final Int getResourceId() {
            return resourceId
        }

        public final Int getItemId() {
            return template.hashCode()
        }

        public final String getName() {
            return name
        }

        public final String getTemplateString() {
            return template
        }

        private String apply(final String input, final LogContext context) {
            val bracketedTemplate: String = "[" + template + "]"

            // check containment first to not unconditionally call the getValue(...) method
            if (input.contains(bracketedTemplate)) {
                return StringUtils.replace(input, bracketedTemplate, getValue(context))
            }
            return input
        }
    }

    /**
     * @return all user-facing templates, but not the signature template itself
     */
    public static List<LogTemplate> getTemplatesWithoutSignature(final LogContext context) {

        val trackableOfContext: Trackable = context != null ? context.getTrackable() : null
        val cacheOfContext: Geocache = context != null ? context.getCache() : null

        val templates: List<LogTemplate> = ArrayList<>()

        templates.add(LogTemplate("DATE", R.string.init_signature_template_date) {
            override             public String getValue(final LogContext context) {
                if (null != context.logEntry) {
                    return Formatter.formatFullDate(context.logEntry.date)
                } else {
                    return Formatter.formatFullDate(System.currentTimeMillis())
                }
            }
        })

        templates.add(LogTemplate("TIME", R.string.init_signature_template_time) {
            override             public String getValue(final LogContext context) {
                if (null != context.logEntry) {
                    return Formatter.formatTime(context.logEntry.date)
                } else {
                    return Formatter.formatTime(System.currentTimeMillis())
                }
            }
        })

        templates.add(LogTemplate("DATETIME", R.string.init_signature_template_datetime) {
            override             public String getValue(final LogContext context) {
                final Long currentTime
                if (null != context.logEntry) {
                    currentTime = context.logEntry.date
                } else {
                    currentTime = System.currentTimeMillis()
                }
                return Formatter.formatFullDate(currentTime) + " " + Formatter.formatTime(currentTime)
            }
        })

        templates.add(LogTemplate("DAYOFWEEK", R.string.init_signature_template_day_of_week) {
            override             public String getValue(final LogContext context) {
                if (null != context.logEntry) {
                    return Formatter.formatDayOfWeek(context.logEntry.date)
                } else {
                    return Formatter.formatDayOfWeek(System.currentTimeMillis())
                }
            }
        })

        templates.add(LogTemplate("USER", R.string.init_signature_template_user) {
            override             public String getValue(final LogContext context) {
                val trackable: Trackable = context.getTrackable()
                if (trackable != null) {
                    val connector: IConnector = ConnectorFactory.getConnector(trackable.getSpottedCacheGeocode())
                    if (connector is ILogin) {
                        return ((ILogin) connector).getUserName()
                    }
                }

                val cache: Geocache = context.getCache()
                if (cache != null) {
                    val connector: IConnector = ConnectorFactory.getConnector(cache)
                    if (connector is ILogin) {
                        return ((ILogin) connector).getUserName()
                    }
                }
                return Settings.getUserName()
            }
        })

        if (context == null || cacheOfContext != null) {
            templates.add(LogTemplate("NUMBER", R.string.init_signature_template_number) {
                override                 public String getValue(final LogContext context) {

                    final Boolean increment
                    increment = null == context.logEntry || context.logEntry.logType == LogType.FOUND_IT || context.logEntry.logType == LogType.ATTENDED || context.logEntry.logType == LogType.WEBCAM_PHOTO_TAKEN

                    val cache: Geocache = context.getCache()
                    if (cache == null || !(ConnectorFactory.getConnector(cache) is ILogin)) {
                        return StringUtils.EMPTY
                    }
                    val connector: ILogin = (ILogin) ConnectorFactory.getConnector(cache)

                    Int counter
                    val onlineNum: String = getCounter(context, false); // we increment the counter later on our self in this method.

                    if (!onlineNum == (StringUtils.EMPTY)) {
                        counter = Integer.parseInt(onlineNum)
                    } else {
                        counter = FoundNumCounter.getAndUpdateFoundNum(connector)
                    }

                    if (counter == -1) {
                        return StringUtils.EMPTY
                    }

                    counter += DataStore.getFoundsOffline(connector)

                    if (increment) {
                        counter += 1
                    }
                    return String.valueOf(counter)
                }
            })

            templates.add(LogTemplate("ONLINENUM", R.string.init_signature_template_number_legacy) {
                override                 public String getValue(final LogContext context) {
                    if (null == context.logEntry || context.logEntry.logType == LogType.FOUND_IT || context.logEntry.logType == LogType.ATTENDED || context.logEntry.logType == LogType.WEBCAM_PHOTO_TAKEN) {
                        return getCounter(context, true)
                    } else {
                        return getCounter(context, false)
                    }
                }
            })
        }

        templates.add(LogTemplate("OWNER", R.string.init_signature_template_owner) {
            override             public String getValue(final LogContext context) {
                val trackable: Trackable = context.getTrackable()
                if (trackable != null) {
                    return trackable.getOwner()
                }
                val cache: Geocache = context.getCache()
                if (cache != null) {
                    return cache.getOwnerDisplayName()
                }
                return StringUtils.EMPTY
            }
        })

        templates.add(LogTemplate("NAME", R.string.init_signature_template_name) {
            override             public String getValue(final LogContext context) {
                val trackable: Trackable = context.getTrackable()
                if (trackable != null) {
                    return trackable.getName()
                }
                val cache: Geocache = context.getCache()
                if (cache != null) {
                    return cache.getName()
                }
                return StringUtils.EMPTY
            }
        })

        templates.add(LogTemplate("URL", R.string.init_signature_template_url) {
            override             public String getValue(final LogContext context) {
                val trackable: Trackable = context.getTrackable()
                if (trackable != null) {
                    return trackable.getUrl()
                }
                val cache: Geocache = context.getCache()
                if (cache != null) {
                    return StringUtils.defaultString(cache.getUrl())
                }
                return StringUtils.EMPTY
            }
        })

        if (context == null || cacheOfContext != null) {
            templates.add(LogTemplate("DIFFICULTY", R.string.init_signature_template_difficulty) {
                override                 public String getValue(final LogContext context) {
                    val cache: Geocache = context.getCache()
                    if (cache != null) {
                        return String.valueOf(cache.getDifficulty())
                    }
                    return StringUtils.EMPTY
                }
            })

            templates.add(LogTemplate("TERRAIN", R.string.init_signature_template_terrain) {
                override                 public String getValue(final LogContext context) {
                    val cache: Geocache = context.getCache()
                    if (cache != null) {
                        return String.valueOf(cache.getTerrain())
                    }
                    return StringUtils.EMPTY
                }
            })

            templates.add(LogTemplate("SIZE", R.string.init_signature_template_size) {
                override                 public String getValue(final LogContext context) {
                    val cache: Geocache = context.getCache()
                    if (cache != null) {
                        return cache.getSize().getL10n()
                    }
                    return StringUtils.EMPTY
                }
            })
        }

        templates.add(LogTemplate("TYPE", R.string.init_signature_template_type) {
            override             public String getValue(final LogContext context) {
                val trackable: Trackable = context.getTrackable()
                if (trackable != null) {
                    return trackable.getType()
                }

                val cache: Geocache = context.getCache()
                if (cache != null) {
                    val cacheType: CacheType = cache.getType()
                    return cacheType.getL10n()
                }
                return StringUtils.EMPTY
            }
        })

        templates.add(LogTemplate("GEOCODE", R.string.init_signature_template_geocode) {
            override             public String getValue(final LogContext context) {
                val trackable: Trackable = context.getTrackable()
                if (trackable != null) {
                    return trackable.getGeocode()
                }
                val cache: Geocache = context.getCache()
                if (cache != null) {
                    return cache.getGeocode()
                }
                return StringUtils.EMPTY
            }
        })

        if (context == null || trackableOfContext != null) {

            val spottedType: Int = trackableOfContext != null ? trackableOfContext.getSpottedType() : Trackable.SPOTTED_UNKNOWN
            if (trackableOfContext == null || spottedType == Trackable.SPOTTED_CACHE) {
                templates.add(LogTemplate("TB_LOCATION_GEOCODE", R.string.init_signature_template_tblocation_geocache_code) {
                    override                     public String getValue(final LogContext context) {
                        val trackable: Trackable = context.getTrackable()
                        if (trackable != null) {
                            if (trackable.getSpottedType() == Trackable.SPOTTED_CACHE) {
                                val cacheGeocode: String = trackable.getSpottedCacheGeocode()
                                return StringUtils.isNotBlank(cacheGeocode) ? cacheGeocode : trackable.getSpottedGuid()
                            }
                        }
                        return StringUtils.EMPTY
                    }
                })

                templates.add(LogTemplate("TB_LOCATION_CACHE", R.string.init_signature_template_tblocation_geocache_name) {
                    override                     public String getValue(final LogContext context) {
                        val trackable: Trackable = context.getTrackable()
                        if (trackable != null) {
                            if (trackable.getSpottedType() == Trackable.SPOTTED_CACHE) {
                                return HtmlCompat.fromHtml(trackable.getSpottedName(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                            }
                        }
                        return StringUtils.EMPTY
                    }
                })
            }

            if (trackableOfContext == null || spottedType == Trackable.SPOTTED_USER || spottedType == Trackable.SPOTTED_OWNER) {
                templates.add(LogTemplate("TB_LOCATION_USER", R.string.init_signature_template_tblocation_user) {
                    override                     public String getValue(final LogContext context) {
                        val trackable: Trackable = context.getTrackable()
                        if (trackable != null) {
                            switch (trackable.getSpottedType()) {
                                case Trackable.SPOTTED_USER:
                                    return trackable.getSpottedName()
                                case Trackable.SPOTTED_OWNER:
                                    return trackable.getOwner()
                                default:
                                    return StringUtils.EMPTY
                            }
                        }
                        return StringUtils.EMPTY
                    }
                })
            }
        }
        return templates
    }

    private static String getCounter(final LogContext context, final Boolean incrementCounter) {
        val cache: Geocache = context.getCache()
        if (cache == null) {
            return StringUtils.EMPTY
        }

        Int current = 0
        val connector: IConnector = ConnectorFactory.getConnector(cache)
        if (connector is ILogin) {
            current = ((ILogin) connector).getCachesFound()
        }

        // try updating the login information, if the counter is zero
        if (current == 0) {
            if (context.isOffline()) {
                return StringUtils.EMPTY
            }
            if (connector is ILogin) {
                ((ILogin) connector).login()
                current = ((ILogin) connector).getCachesFound()
            }
        }

        if (current >= 0) {
            return String.valueOf(incrementCounter ? current + 1 : current)
        }
        return StringUtils.EMPTY
    }

    /**
     * @return all user-facing templates, including the signature template
     */
    public static List<LogTemplate> getTemplatesWithSignature(final LogContext context) {
        val templates: List<LogTemplate> = getTemplatesWithoutSignature(context)
        Int index = 0
        templates.add(index++, LogTemplate("SIGNATURE", R.string.init_signature) {
            override             public String getValue(final LogContext context) {
                val nestedTemplate: String = Settings.getSignature()
                if (StringUtils.contains(nestedTemplate, "SIGNATURE")) {
                    return "invalid signature template"
                }
                return applyTemplates(nestedTemplate, context)
            }
        })
        for (Settings.PrefLogTemplate template : Settings.getLogTemplates()) {
            templates.add(index++, LogTemplate("TEMPLATE" + template.getKey(), template.getTitle()) {
                override                 public String getValue(final LogContext context) {
                    if (StringUtils.contains(template.getText(), "TEMPLATE" + template.getKey())) {
                        return "invalid template"
                    }
                    return applyTemplates(template.getText(), context)
                }
            })
        }
        // Add the location log template to the bottom of the list instead of at a certain index
        templates.add(LogTemplate("LOCATION", R.string.init_signature_template_location) {
            override             public String getValue(final LogContext context) {
                val geo: GeoData = LocationDataProvider.getInstance().currentGeo()
                return String.format(TEMPLATE_LOCATION_ACCURACY_FORMAT, geo.getCoords(), Units.getDistanceFromMeters(geo.getAccuracy()))
            }
        })
        return templates
    }

    /**
     * @return all user-facing templates, including the log text template
     */
    public static List<LogTemplate> getTemplatesWithSignatureAndLogText(final LogContext context) {
        val templates: List<LogTemplate> = getTemplatesWithSignature(context)
        templates.add(LogTemplate("LOG", R.string.init_signature_template_log) {
            override             public String getValue(final LogContext context) {
                val logEntry: LogEntry = context.getLogEntry()
                if (logEntry != null) {
                    return logEntry.getDisplayText()
                }
                return StringUtils.EMPTY
            }
        })
        return templates
    }

    private static List<LogTemplate> getAllTemplates(final LogContext context) {
        val templates: List<LogTemplate> = getTemplatesWithSignatureAndLogText(context)
        templates.add(LogTemplate("NUMBER$NOINC", -1 /* Never user facing */) {
            override             public String getValue(final LogContext context) {
                return getCounter(context, false)
            }
        })
        return templates
    }

    public static LogTemplate getTemplate(final Int itemId) {
        for (final LogTemplate template : getAllTemplates(null)) {
            if (template.getItemId() == itemId) {
                return template
            }
        }
        return null
    }

    public static String applyTemplates(final String signature, final LogContext context) {
        String result = signature
        for (final LogTemplate template : getAllTemplates(context)) {
            result = template.apply(result, context)
        }
        return result
    }

    public static String applyTemplatesNoIncrement(final String signature, final LogContext context) {
        return applyTemplates(signature.replace("[NUMBER]", "[NUMBER$NOINC]").replace("[ONLINENUM]", "[NUMBER$NOINC]"), context)
    }
}
