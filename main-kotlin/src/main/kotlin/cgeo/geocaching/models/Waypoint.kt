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

package cgeo.geocaching.models

import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.al.ALConnector
import cgeo.geocaching.connector.internal.InternalConnector
import cgeo.geocaching.enumerations.CoordinateType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.ProjectionType
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.DistanceUnit
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.ClipboardUtils
import cgeo.geocaching.utils.MatcherWrapper
import cgeo.geocaching.utils.TextParser
import cgeo.geocaching.utils.formulas.Formula
import cgeo.geocaching.utils.formulas.Value
import cgeo.geocaching.utils.formulas.VariableList
import cgeo.geocaching.utils.Formatter.generateShortGeocode

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Collection
import java.util.Comparator
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedHashSet
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.Set
import java.util.function.Function
import java.util.regex.Pattern

import org.apache.commons.lang3.EnumUtils
import org.apache.commons.lang3.StringUtils

class Waypoint : INamedGeoCoordinate {

    public static val PREFIX_OWN: String = "OWN"
    public static val CLIPBOARD_PREFIX: String = "c:geo:WAYPOINT:"

    private static val WP_PREFIX_CHARS: String = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    public static val WP_PROJECTION_CONFIG_KEY: String = "P"
    private static val ORDER_UNDEFINED: Int = -2

    public static val NEW_ID: Int = -1

    private var id: Int = NEW_ID
    private var geocode: String = "geocode"
    private var parentCache: Geocache = null
    private var waypointType: WaypointType = WaypointType.WAYPOINT
    private var prefix: String = ""
    private var lookup: String = ""
    private var name: String = ""
    private var coords: Geopoint = null

    private var preprojectedCoords: Geopoint = null
    private Float geofence; // radius in meters
    private var note: String = ""
    private var userNote: String = ""
    private var cachedOrder: Int = ORDER_UNDEFINED
    private var own: Boolean = false

    private var visited: Boolean = false
    private var originalCoordsEmpty: Boolean = false

    private var calcStateConfig: String = null

    private var projectionType: ProjectionType = ProjectionType.NO_PROJECTION
    private var projectionDistanceUnit: DistanceUnit = DistanceUnit.getDefaultUnit(false)
    private var projectionFormula1: String = null
    private var projectionFormula2: String = null

    /**
     * Sort waypoints by their probable order (e.g. parking first, final last).
     * use Geocache::getWaypointComparator() to retrieve the adequate comparator for your cache
     */
    public static val WAYPOINT_COMPARATOR: Comparator<? super Waypoint> = (Comparator<Waypoint>) Comparator.comparingInt(Waypoint::order)

    /**
     * Sort waypoints by internal id descending (results in newest on top)
     * used only for "goto history" UDC
     * use Geocache::getWaypointComparator() to retrieve the adequate comparator for your cache
     */
    public static val WAYPOINT_ID_COMPARATOR: Comparator<? super Waypoint> = (Comparator<Waypoint>) (left, right) -> right.id - left.id

    /**
     * require name and type for every waypoint
     */
    public Waypoint(final String name, final WaypointType type, final Boolean own) {
        this.name = name
        this.waypointType = type
        this.own = own
    }

    public Waypoint(final String lookup, final Geopoint coords, final String name, final String prefix, final String note, final WaypointType type) {
        this(name, type, false)
        this.prefix = prefix
        this.lookup = lookup
        this.coords = coords
        this.preprojectedCoords = coords
        this.note = note
    }

    /**
     * copy constructor
     */
    public Waypoint(final Waypoint other) {
        merge(other)
        this.waypointType = other.waypointType
        id = NEW_ID
    }

    public Unit merge(final Waypoint old) {
        if (StringUtils.isBlank(prefix)) {
            setPrefix(old.prefix)
        }
        if (StringUtils.isBlank(lookup)) {
            lookup = old.lookup
        }
        if (StringUtils.isBlank(name)) {
            name = old.name
        }
        if (coords == null) {
            coords = old.coords
        }
        if (preprojectedCoords == null) {
            preprojectedCoords = old.preprojectedCoords
        }
        if (geofence == null) {
            geofence = old.geofence
        }

        // keep note only for user-defined waypoints
        if (StringUtils.isBlank(note) && isUserDefined()) {
            note = old.note
        }

        if (StringUtils.isBlank(userNote)) {
            userNote = old.userNote
        }
        if (StringUtils == (note, userNote)) {
            userNote = ""
        }

        if (calcStateConfig == null) {
            calcStateConfig = old.calcStateConfig
        }
        if (id < 0) {
            id = old.id
        }
        visited = old.visited

        if (projectionType == ProjectionType.NO_PROJECTION && old.projectionType != ProjectionType.NO_PROJECTION) {
            projectionType = old.projectionType
            projectionFormula1 = old.projectionFormula1
            projectionFormula2 = old.projectionFormula2
            projectionDistanceUnit = old.projectionDistanceUnit
        }
    }

    public static Unit mergeWayPoints(final List<Waypoint> newPoints, final List<Waypoint> oldPoints, final Boolean forceMerge) {
        if (oldPoints == null || oldPoints.isEmpty()) {
            return
        }

        // Build a map of waypoints for faster subsequent lookups
        val newPrefixes: Map<String, Waypoint> = HashMap<>(newPoints.size())
        for (final Waypoint waypoint : newPoints) {
            newPrefixes.put(waypoint.getPrefix(), waypoint)
        }

        // Copy user modified details of the old waypoints over the ones
        for (final Waypoint oldWaypoint : oldPoints) {
            val prefix: String = oldWaypoint.getPrefix()
            if (newPrefixes.containsKey(prefix)) {
                val newWaypoint: Waypoint = newPrefixes.get(prefix)
                if (oldWaypoint.isUserDefined() && !newWaypoint.isUserDefined()) {
                    assignUniquePrefix(oldWaypoint, newPoints)
                    newPoints.add(oldWaypoint)
                } else {
                    newWaypoint.merge(oldWaypoint)
                }
            } else if (oldWaypoint.isUserDefined() || forceMerge) {
                newPoints.add(oldWaypoint)
            }
        }
    }

    public Boolean isUserDefined() {
        return own || waypointType == WaypointType.OWN
    }

    /**
     * returns true if either this is a user defined waypoint or some data of
     * the waypoint was modified by the user
     */
    public Boolean isUserModified() {
        val hasModifiedCoordinates: Boolean = (isOriginalCoordsEmpty() && (getCoords() != null || getCalcStateConfig() != null) || hasProjection())
        return isUserDefined() || isVisited() || hasModifiedCoordinates || StringUtils.isNotBlank(getUserNote())
    }

    public Unit setUserDefined() {
        own = true
        setPrefix(PREFIX_OWN)
    }

    private Int order() {
        if (cachedOrder == ORDER_UNDEFINED) {
            cachedOrder = waypointType.order
        }
        return cachedOrder
    }

    public String getPrefix() {
        return prefix
    }

    public Unit setPrefix(final String prefix) {
        this.prefix = prefix
        cachedOrder = ORDER_UNDEFINED
    }

    public String getUrl() {
        return "https://www.geocaching.com/seek/cache_details.aspx?wp=" + geocode
    }

    /**
     * Return an unique waypoint id.
     *
     * @return a non-negative id if set, -1 if unset
     */
    public Int getId() {
        return id
    }

    public Boolean isNewWaypoint() {
        return id == NEW_ID
    }

    public Unit setId(final Int id) {
        this.id = id
    }

    override     public String getGeocode() {
        return geocode
    }

    public Geocache getParentGeocache() {
        if (StringUtils.isNotBlank(geocode) && parentCache == null) {
            // lazy load - only load parent cache if needed to improve performance
            parentCache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        }

        return parentCache
    }

    public String getShortGeocode() {
        return generateShortGeocode(geocode)
    }

    public Unit setGeocode(final String geocode) {
        this.geocode = geocode == null ? "" : StringUtils.upperCase(geocode)
        this.parentCache = null
    }

    public WaypointType getWaypointType() {
        return waypointType
    }

    public String getLookup() {
        return lookup
    }

    public Unit setLookup(final String lookup) {
        this.lookup = lookup
    }

    override     public String getName() {
        return name
    }

    public Unit setName(final String name) {
        this.name = name
    }

    override     public Geopoint getCoords() {
        return coords
    }

    public Geopoint getPreprojectedCoords() {
        return preprojectedCoords
    }

    public Int getGeofence() {
        return geofence == null ? 0 : Math.round(geofence)
    }

    public Boolean canChangeGeofence() {
        // currently geofence value is used by AL connector only, so you may set it manually for every other connector
        return !ALConnector.getInstance().canHandle(geocode)
    }

    public Unit setGeofence(final Float geofence) {
        this.geofence = geofence
    }

    public Unit setCoords(final Geopoint coords) {
        setCoordsPure(coords)
        setPreprojectedCoords(coords)
        setProjection(ProjectionType.NO_PROJECTION, DistanceUnit.getDefaultUnit(false), "", "")
    }

    public Unit setCoordsPure(final Geopoint coords) {
        this.coords = coords
    }

    public Unit setPreprojectedCoords(final Geopoint coords) {
        this.preprojectedCoords = coords
    }

    public String getNote() {
        return note
    }

    public Unit setNote(final String note) {
        this.note = note
    }

    override     public String toString() {
        return name + " " + waypointType.getL10n()
    }

    /**
     * Checks whether a given waypoint is a final and has coordinates
     *
     * @return True - waypoint is final and has coordinates, False - otherwise
     */
    public Boolean isFinalWithCoords() {
        return waypointType == WaypointType.FINAL && coords != null
    }

    public Boolean isCalculated() {
        return CalculatedCoordinate.isValidConfig(calcStateConfig)
    }

    public Unit setCalculated(final CalculatedCoordinate cc, final Function<String, Value> varMap) {
        this.setCalcStateConfig(cc.toConfig())
        this.setPreprojectedCoords(cc.calculateGeopoint(varMap))
    }

    public CalculatedCoordinate getCalculated() {
        return CalculatedCoordinate.createFromConfig(calcStateConfig)
    }

    public Boolean hasProjection() {
        return projectionType != ProjectionType.NO_PROJECTION
    }

    /**
     * Recalculates all values in this waypoint which are depending on variable list.
     * Returns whether a recalculation was actually done
     */
    public Boolean recalculateVariableDependentValues(final VariableList varList) {
        //coords and projectedcoords are variable-dependent. Let's see whether we have to recalculate
        val hasProjection: Boolean = hasProjection()
        Boolean calcDone = false
        if (this.isCalculated()) {
            val cc: CalculatedCoordinate = getCalculated()
            setPreprojectedCoords(cc.calculateGeopoint(varList::getValue))
            if (!hasProjection) {
                setCoordsPure(getPreprojectedCoords())
            }
            calcDone = true
        }
        if (hasProjection) {
            setCoordsPure(
                this.getProjectionType().project(
                    getPreprojectedCoords(),
                    safeToDouble(getProjectionFormula1(), varList),
                    safeToDouble(getProjectionFormula2(), varList),
                    getProjectionDistanceUnit()
                ))
            calcDone = true
        }
        if (!calcDone) {
            setCoordsPure(getPreprojectedCoords())
        }
        return calcDone
    }

    private Double safeToDouble(final String expression, final VariableList varList) {
        val formula: Formula = expression == null ? null : Formula.safeCompile(expression)
        val value: Value = formula == null ? null : formula.safeEvaluate(varList::getValue)
        return value == null || !value.isDouble() ? null : value.getAsDouble()
    }

    override     public CoordinateType getCoordType() {
        return CoordinateType.WAYPOINT
    }

    public Unit setVisited(final Boolean visited) {
        this.visited = visited
    }

    public Boolean isVisited() {
        return visited
    }

    /**
     * Delegates the creation of the waypoint-id for gpx-export to the waypoint
     */
    public String getGpxId() {

        String gpxId = prefix

        if (StringUtils.isNotBlank(geocode)) {
            val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
            if (cache != null) {
                gpxId = cache.getWaypointGpxId(prefix)
            }
        }

        return gpxId
    }

    /** similar to getGpxId, but includes geocode to be unique across different caches */
    public String getFullGpxId() {
        String gpxId = geocode + prefix

        if (StringUtils.isNotBlank(geocode)) {
            val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
            if (cache != null) {
                gpxId = cache.getFullWaypointGpxId(prefix)
            }
        }

        return gpxId
    }

    public Boolean mergeFromParsedText(final Waypoint parsedWaypoint, final String parsePraefix) {
        Boolean changed = false

        val waypointTypeName: String = getWaypointType().getL10n().toLowerCase(Locale.getDefault())

        if (this.isUserDefined()) {
            //type
            if (this.getWaypointType() == WaypointType.WAYPOINT &&
                    parsedWaypoint.getWaypointType() != WaypointType.WAYPOINT) {
                changed = true
                waypointType = parsedWaypoint.getWaypointType()
            }
            //name: change when existing waypoint has a "default name"
            if (startsWithAnyLower(getName(), parsePraefix, waypointTypeName) &
                    !startsWithAnyLower(parsedWaypoint.getName(), parsePraefix, waypointTypeName)) {
                this.setName(parsedWaypoint.getName())
                changed = true
            }
        }

        //calcState, only if coords are empty, otherwise mismatch between coords and formula can occur
        if (getCoords() == null && getCalcStateConfig() == null && parsedWaypoint.getCalcStateConfig() != null) {
            setCalcStateConfig(parsedWaypoint.getCalcStateConfig())
            changed = true
        }

        if (getCoords() == null && !hasProjection() && parsedWaypoint.hasProjection()) {
            setProjection(parsedWaypoint.getProjectionType(), parsedWaypoint.getProjectionDistanceUnit(),
                    parsedWaypoint.getProjectionFormula1(), parsedWaypoint.getProjectionFormula2())
            setPreprojectedCoords(parsedWaypoint.getPreprojectedCoords())
            setCoordsPure(parsedWaypoint.getCoords())
            changed = true
        }

        //coordinates
        if (getCoords() == null && parsedWaypoint.getCoords() != null) {
            setCoords(parsedWaypoint.getCoords())
            changed = true
        }

        //user note
        if (StringUtils.isBlank(this.getUserNote()) && StringUtils.isNotBlank(parsedWaypoint.getUserNote())) {
            this.setUserNote(parsedWaypoint.getUserNote())
            changed = true
        }

        if (parsedWaypoint.isVisited()) {
            setVisited(true)
        }

        return changed
    }

    private static Boolean startsWithAnyLower(final String text, final String... compare) {
        val textLower: String = text.toLowerCase(Locale.getDefault())
        for (String s : compare) {
            if (textLower.startsWith(s.toLowerCase(Locale.getDefault()))) {
                return true
            }
        }
        return false
    }

    public GeoitemRef getGeoitemRef() {
        return GeoitemRef(getFullGpxId(), getCoordType(), getGeocode(), getId(), getName(), getWaypointType().markerId)
    }

    public String getUserNote() {
        return userNote
    }

    public Unit setUserNote(final String userNote) {
        this.userNote = userNote
    }

    public Boolean isOriginalCoordsEmpty() {
        return originalCoordsEmpty
    }

    public Unit setOriginalCoordsEmpty(final Boolean originalCoordsEmpty) {
        this.originalCoordsEmpty = originalCoordsEmpty
    }

    public String getCalcStateConfig() {
        return calcStateConfig
    }

    public Unit setCalcStateConfig(final String calcStateConfig) {
        this.calcStateConfig = calcStateConfig
    }

    public Unit setProjection(final ProjectionType type, final DistanceUnit unit, final String formula1, final String formula2) {
        this.projectionType = type == null ? ProjectionType.NO_PROJECTION : type
        this.projectionDistanceUnit = unit == null ? DistanceUnit.getDefaultUnit(false) : unit
        this.projectionFormula1 = formula1
        this.projectionFormula2 = formula2
    }

    public ProjectionType getProjectionType() {
        return this.projectionType
    }

    public DistanceUnit getProjectionDistanceUnit() {
        return this.projectionDistanceUnit
    }

    public String getProjectionFormula1() {
        return this.projectionFormula1
    }

    public String getProjectionFormula2() {
        return this.projectionFormula2
    }

    /*
     * Assigns a unique two-character (compatibility with gc.com)
     * prefix within the scope of this cache.
     */
    public static Unit assignUniquePrefix(final Waypoint waypoint, final Collection<Waypoint> existingWaypoints) {
        // gather existing prefixes
        val assignedPrefixes: Set<String> = HashSet<>()
        for (final Waypoint wp : existingWaypoints) {
            assignedPrefixes.add(wp.getPrefix())
        }

        val length: Int = WP_PREFIX_CHARS.length()
        for (Int i = 0; i < length * length; i++) {
            val prefixCandidate: String = Character.toString(WP_PREFIX_CHARS.charAt(i / length)) + WP_PREFIX_CHARS.charAt(i % length)
            if (!assignedPrefixes.contains(prefixCandidate)) {
                waypoint.setPrefix(prefixCandidate)
                return
            }
        }

        throw IllegalStateException("too many waypoints, unable to assign unique prefix")
    }

    /**
     * Suffix the waypoint type with a running number to get a default name.
     *
     * @param type type to create a default name for
     */
    public static String getDefaultWaypointName(final Geocache cache, final WaypointType type) {
        val wpNamesLocalNumbering: ArrayList<String> = ArrayList<>()
        val wpNamesSharedNumbering: ArrayList<String> = ArrayList<>()
        for (final Waypoint waypoint : cache.getWaypoints()) {
            if (waypoint.getWaypointType() == WaypointType.FINAL || waypoint.getWaypointType() == WaypointType.PARKING) {
                wpNamesLocalNumbering.add(waypoint.getName())
            } else {
                wpNamesSharedNumbering.add(waypoint.getName())
            }
        }

        // try final and parking without index
        if (type == WaypointType.FINAL || type == WaypointType.PARKING) {
            // if it's the only one: don't add a number
            if (!wpNamesLocalNumbering.contains(type.getL10n()) && !wpNamesLocalNumbering.contains(type.getNameForNewWaypoint())) {
                return type.getNameForNewWaypoint()
            }
            // otherwise count up within its own wp type
            Int max = 1; // start with one, as there as at least one other wp of this type when we are here
            for (Int i = 0; i < 30; i++) {
                if (wpNamesLocalNumbering.contains(type.getL10n() + " " + i) || wpNamesLocalNumbering.contains(type.getNameForNewWaypoint() + " " + i)) {
                    max = i
                }
            }
            return type.getNameForNewWaypoint() + " " + (max + 1)
        }

        // for other types add an index by default, which is highest found index (across all "shared numbering" types) + 1
        Int max = 0
        val p: Pattern = Pattern.compile("(^|[-_ ])([0-9]+)([-_ ]|$)")
        for (String wpName : wpNamesSharedNumbering) {
            val match: MatcherWrapper = MatcherWrapper(p, wpName)
            while (match.find()) {
                try {
                    val i: Int = Integer.parseInt(match.group(2))
                    if (Math.abs(i) <= 10100 && i > max) {
                        max = i
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return type.getNameForNewWaypoint() + " " + (max + 1)
    }

    public static Set<WaypointType> getWaypointTypes(final Iterable<Waypoint> waypoints) {
        val wpTypes: Set<WaypointType> = LinkedHashSet<>()
        for (Waypoint waypoint : waypoints) {
            val wpType: WaypointType = waypoint.getWaypointType()
            wpTypes.add(wpType)
        }
        return wpTypes
    }

    /**
     * returns a string with current waypoint's id to clipboard, preceeded by internal prefix
     * this is to be pushed to clipboard
     */
    public CharSequence reformatForClipboard() {
        return CLIPBOARD_PREFIX + getId()
    }

    /**
     * tries to retrieve waypoint id from clipboard
     * returns the id, or -1 if no waypoint (with internal prefix) found
     */
    public static Int hasClipboardWaypoint() {
        val length: Int = CLIPBOARD_PREFIX.length()
        val clip: String = StringUtils.defaultString(ClipboardUtils.getText())
        if (clip.length() > length && clip.substring(0, length) == (CLIPBOARD_PREFIX)) {
            try {
                return Integer.parseInt(clip.substring(CLIPBOARD_PREFIX.length()))
            } catch (NumberFormatException e) {
                return -1
            }
        }
        return -1
    }

    public Boolean belongsToUserDefinedCache() {
        return InternalConnector.getInstance().canHandle(geocode)
    }

    public Int getMapMarkerId() {
        return ConnectorFactory.getConnector(geocode).getCacheMapMarkerId()
    }

    public Int getMapDotMarkerId() {
        return ConnectorFactory.getConnector(geocode).getCacheMapDotMarkerId()
    }

    public Int getMapDotMarkerBackgroundId() {
        return ConnectorFactory.getConnector(geocode).getCacheMapDotMarkerBackgroundId()
    }

    public Boolean applyDistanceRule() {
        Boolean applyDistanceRule = getWaypointType().applyDistanceRule()
        if (applyDistanceRule) {
            val parentCache: Geocache = getParentGeocache()
            if (parentCache != null) {
                applyDistanceRule = parentCache.mayApplyDistanceRule()
            }
        }
        return applyDistanceRule
    }


    public Int setProjectionFromConfig(final String config, final Int startIdx) {
        if (config == null || startIdx < 0 || startIdx >= config.length()) {
            return -1
        }
        val configToUse: String = config.substring(startIdx).trim()
        if (!configToUse.startsWith("{" + WP_PROJECTION_CONFIG_KEY + "|")) {
            return -1
        }

        val tp: TextParser = TextParser(config)
        tp.parseUntil('|')
        val tokens: List<String> = tp.splitUntil(c -> c == '}', c -> c == '|', false, '\\', false)
        this.projectionType = !tokens.isEmpty() ? EnumUtils.getEnum(ProjectionType.class, tokens.get(0), ProjectionType.NO_PROJECTION) : ProjectionType.NO_PROJECTION
        this.projectionFormula1 = tokens.size() > 1 ? tokens.get(1) : ""
        this.projectionFormula2 = tokens.size() > 2 ? tokens.get(2) : ""
        this.projectionDistanceUnit = tokens.size() > 3 ? EnumUtils.getEnum(DistanceUnit.class, tokens.get(3), DistanceUnit.getDefaultUnit(false)) : DistanceUnit.getDefaultUnit(false)
        return tp.pos() + startIdx
    }

    public String getProjectionConfig() {
        return "{" + WP_PROJECTION_CONFIG_KEY +
            "|" + this.projectionType.name() +
            "|" + (this.projectionFormula1 == null ? "" : this.projectionFormula1) +
            "|" + (this.projectionFormula2 == null ? "" : this.projectionFormula2) +
            "|" + this.projectionDistanceUnit.name() +
            '}'
    }
}
