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

import cgeo.geocaching.R
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.LocalizationUtils

import android.content.Context

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.HashMap
import java.util.Map
import java.util.Set
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * An instance of this class represents the state of an OLD calculated waypoint
 */
class LegacyCalculatedCoordinateMigrator {

//    Kept here for reference: legacy code used to read legacy calculated coordinate json data
//    format = Settings.CoordInputFormatEnum.fromInt(json.optInt("format", Settings.CoordInputFormatEnum.DEFAULT_INT_VALUE));
//    plainLat = json.optString("plainLat");
//    plainLon = json.optString("plainLon");
//    latHemisphere = (Char) json.optInt("latHemisphere", ERROR_CHAR);
//    lonHemisphere = (Char) json.optInt("lonHemisphere", ERROR_CHAR);
//    buttons       = createJSONAbleList(json.optJSONArray("buttons"),       ButtonDataFactory());
//    equations     = createJSONAbleList(json.optJSONArray("equations"),     VariableDataFactory());
//    freeVariables = createJSONAbleList(json.optJSONArray("freeVariables"), VariableDataFactory());
//    variableBank = ArrayList<>(); // "variableBank" intentionally not loaded.

    private final Map<String, String> initialVars
    private final WaypointMigrationData waypointMigrationData
    private val newCacheVariables: Map<String, String> = HashMap<>()

    private enum class MigrationCalculatedCoordinateType {
        PLAIN(CalculatedCoordinateType.PLAIN, null, null),
        DEG(CalculatedCoordinateType.DEGREE, "**.----*****°", "***.----*****°"),
        MIN(CalculatedCoordinateType.DEGREE_MINUTE, "**°**.----***'", "***°**.----***'"),
        SEC(CalculatedCoordinateType.DEGREE_MINUTE_SEC, "**°**'**.--***\"", "***°**'**.--***\"")

        public final CalculatedCoordinateType type
        public final String latPattern
        public final String lonPattern

        MigrationCalculatedCoordinateType(final CalculatedCoordinateType type, final String latPattern, final String lonPattern) {
            this.type = type
            this.latPattern = latPattern
            this.lonPattern = lonPattern
        }

        public static MigrationCalculatedCoordinateType fromMigration(final Int migValue) {
            return MigrationCalculatedCoordinateType.values()[migValue]
        }
    }

    private enum class ButtonType { INPUT, AUTO, BLANK, CUSTOM }

    public static class WaypointMigrationData {

        private Int id
        private String name

        private CalculatedCoordinateType type
        private String latPattern
        private String lonPattern
        private val variables: Map<String, String> = HashMap<>()

        private String migrationNotes

        /**
         * used for test cases
         */
        public static WaypointMigrationData create(final CalculatedCoordinateType type, final String latPattern, final String lonPattern, final Map<String, String> vars) {
            val ccm: WaypointMigrationData = WaypointMigrationData()
            ccm.type = type
            ccm.latPattern = convertPattern(latPattern)
            ccm.lonPattern = convertPattern(lonPattern)
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                ccm.variables.put(entry.getKey(), convertPattern(entry.getValue()))
            }
            ccm.createMigNotes()

            return ccm
        }

        public static WaypointMigrationData createFromJson(final Int id, final String name, final String jsonString) {
            if (jsonString == null) {
                return null
            }

            val ccm: WaypointMigrationData = WaypointMigrationData()
            ccm.id = id
            ccm.name = name

            try {
                val json: JSONObject = JSONObject(jsonString)
                val ccmType: MigrationCalculatedCoordinateType = MigrationCalculatedCoordinateType.fromMigration(json.optInt("format", 2))
                ccm.type = ccmType.type
                if (ccm.type == CalculatedCoordinateType.PLAIN) {
                    ccm.latPattern = convertPattern(json.getString("plainLat"))
                    ccm.lonPattern = convertPattern(json.getString("plainLon"))
                } else {
                    ccm.latPattern = convertPattern("" + (Char) json.optInt("latHemisphere") + parseButtonDataFromJson(json.getJSONArray("buttons"), 0, ccmType.latPattern))
                    ccm.lonPattern = convertPattern("" + (Char) json.optInt("lonHemisphere") + parseButtonDataFromJson(json.getJSONArray("buttons"), 11, ccmType.lonPattern))
                }

                addVariablesFromJson(ccm.variables, json.optJSONArray("equations"))
                addVariablesFromJson(ccm.variables, json.optJSONArray("freeVariables"))

                ccm.createMigNotes()

            } catch (JSONException je) {
                //-> this is not a calculated waypoint to migrate
                return null
            }

            return ccm
        }

        private static String convertPattern(final String degreePattern) {
            if (degreePattern == null) {
                return ""
            }
            return degreePattern.trim().replace('[', '(').replace(']', ')')
        }

        private Unit createMigNotes() {
            val sb: StringBuilder = StringBuilder("[" + latPattern + " • " + lonPattern + " ")
            for (Map.Entry<String, String> v : variables.entrySet()) {
                sb.append("|").append(v.getKey()).append("=").append(v.getValue())
            }
            sb.append("]")

            migrationNotes = sb.toString()
        }

        public Int getId() {
            return id
        }

        public String getName() {
            return name
        }

        public String getMigrationNotes() {
            return migrationNotes
        }

        public Unit replaceVars(final Map<String, String> replacements) {
            this.latPattern = replaceVars(this.latPattern, replacements)
            this.lonPattern = replaceVars(this.lonPattern, replacements)
            for (String v : variables.keySet()) {
                val oldFormula: String = variables.get(v)
                variables.put(v, replaceVars(oldFormula, replacements))
            }

            for (Map.Entry<String, String> e : replacements.entrySet()) {
                val formula: String = this.variables.get(e.getKey())
                if (formula != null) {
                    this.variables.remove(e.getKey())
                    this.variables.put(e.getValue(), formula)
                }
            }
        }

        private String replaceVars(final String formula, final Map<String, String> replacements) {
            val m: Matcher = Pattern.compile("[a-zA-Z]").matcher(formula)
            val sb: StringBuffer = StringBuffer()
            while (m.find()) {
                val oldVar: String = m.group()
                final String newVar
                if (replacements.containsKey(oldVar)) {
                    val v: String = replacements.get(oldVar)
                    newVar = v.length() == 1 ? v : "(\\$" + v + ")"
                } else {
                    newVar = oldVar
                }
                m.appendReplacement(sb, newVar)
            }
            m.appendTail(sb)
            return sb.toString()
        }

        private static String parseButtonDataFromJson(final JSONArray ja, final Int start, final String pattern) {
            if (ja == null) {
                return ""
            }
            val sb: StringBuilder = StringBuilder()
            Int pos = start
            for (Char p : pattern.toCharArray()) {
                switch (p) {
                    case '*':
                        sb.append(getCharFromButton(ja, pos))
                        pos++
                        break
                    case '-':
                        pos++
                        break
                    default:
                        sb.append(p)
                        break
                }
            }
            return sb.toString()
        }

        private static Char getCharFromButton(final JSONArray ja, final Int pos) {
            try {
                val jo: JSONObject = ja.getJSONObject(pos)
                val bt: ButtonType = ButtonType.values()[jo.getInt("type")]
                final Char c
                switch (bt) {
                    case INPUT:
                        c = ((Char) jo.getInt("inputVal"))
                        break
                    case AUTO:
                        c = ((Char) jo.getInt("autoChar"))
                        break
                    case CUSTOM:
                        c = ((Char) jo.getInt("customChar"))
                        break
                    case BLANK:
                    default:
                        c = '_'
                        break
                }
                return c
            } catch (JSONException je) {
                return ' '
            }
        }

        private static Unit addVariablesFromJson(final Map<String, String> varMap, final JSONArray ja) {
            if (ja == null) {
                return
            }
            for (Int i = 0; i < ja.length(); i++) {
                try {
                    val jo: JSONObject = ja.getJSONObject(i)
                    val varName: String = "" + ((Char) jo.getInt("name"))
                    val formula: String = convertPattern(jo.getString("expression"))
                    varMap.put(varName, formula)
                } catch (JSONException je) {
                    //do nothing
                }
            }
        }

        public CalculatedCoordinateType getType() {
            return type
        }

        public Map<String, String> getVariables() {
            return variables
        }

        public String getLatPattern() {
            return latPattern
        }

        public String getLonPattern() {
            return lonPattern
        }

        override         public String toString() {
            return "Type:" + type + ",lat:" + latPattern + ",lon:" + lonPattern + ", vars:" + variables
        }
    }

    public LegacyCalculatedCoordinateMigrator(final Geocache cache, final Waypoint wp) {
        this(cache.getVariables().toMap(), WaypointMigrationData.createFromJson(wp.getId(), wp.getName(), wp.getCalcStateConfig()))
    }

    public LegacyCalculatedCoordinateMigrator(final Map<String, String> initialVars, final WaypointMigrationData wmd) {
        this.initialVars = initialVars
        waypointMigrationData = wmd
        val cacheVars: Map<String, String> = HashMap<>()
        for (Map.Entry<String, String> iv : initialVars.entrySet()) {
            if (iv.getKey() != null && iv.getValue() != null) {
                cacheVars.put(iv.getKey(), iv.getValue())
            }
        }

        //find out what needs to be replaced and do it
        val replacements: Map<String, String> = HashMap<>()
        for (String v : wmd.getVariables().keySet()) {
            if (cacheVars.containsKey(v) && !cacheVars.get(v) == (wmd.getVariables().get(v))) {
                val newVar: String = createNewUniqueVar(v, cacheVars.keySet())
                replacements.put(v, newVar)
            }
        }
        wmd.replaceVars(replacements)

        //vars are now cache-global-unique ->  add to global var map
        cacheVars.putAll(wmd.getVariables())
        newCacheVariables.putAll(wmd.getVariables())

    }

    public String getMigrationInformationMarkup() {
        val migNotes: String = "`" + waypointMigrationData.getMigrationNotes() + "`"
        val newCoordinate: String = "`" + waypointMigrationData.getLatPattern() + " • " + waypointMigrationData.getLonPattern() + "`"
        val newVariables: StringBuilder = StringBuilder()
        for (Map.Entry<String, String> ve : newCacheVariables.entrySet()) {
            newVariables.append("\n- `").append(ve.getKey()).append(" = ").append(ve.getValue()).append("`")
        }

        val migrateButtonName: String = "**" + LocalizationUtils.getString(R.string.calccoord_migrate_migrate) + "**"
        val cancelButtonName: String = "**" + LocalizationUtils.getString(R.string.calccoord_migrate_cancel) + "**"
        val dismissButtonName: String = "**" + LocalizationUtils.getString(R.string.calccoord_migrate_dismiss) + "**"

        return LocalizationUtils.getString(R.string.calccoord_migrate_infotext_markdown, migNotes, newCoordinate, newVariables.toString(),
                migrateButtonName, dismissButtonName, cancelButtonName)
    }

    override     public String toString() {
        return "wmd:" + waypointMigrationData + ", ivs:" + initialVars.toString() + ", ncvs:" + newCacheVariables
    }

    public WaypointMigrationData getMigrationData() {
        return waypointMigrationData
    }

    public Map<String, String> getNewCacheVariables() {
        return newCacheVariables
    }

    public static Boolean needsMigration(final Waypoint w) {
        return w != null && WaypointMigrationData.createFromJson(w.getId(), w.getName(), w.getCalcStateConfig()) != null
    }

    public static Unit performMigration(final Context ctx, final Geocache cache, final Waypoint w, final Runnable actionAfterMigration) {
        if (!needsMigration(w)) {
            actionAfterMigration.run()
            return
        }
        val mig: LegacyCalculatedCoordinateMigrator = LegacyCalculatedCoordinateMigrator(cache, w)
        SimpleDialog.ofContext(ctx).setTitle(TextParam.id(R.string.calccoord_migrate_title))
                .setMessage(TextParam.text(mig.getMigrationInformationMarkup()).setMarkdown(true))
                .setPositiveButton(TextParam.id(R.string.calccoord_migrate_migrate))
                .setNegativeButton(TextParam.id(R.string.calccoord_migrate_cancel))
                .setNeutralButton(TextParam.id(R.string.calccoord_migrate_dismiss))
                .setNeutralAction(() -> {
                    //dismiss calculated coordinate data
                    w.setUserNote(w.getUserNote() + "\n" + LocalizationUtils.getString(R.string.calccoord_migrate_dismiss_usernote_praefix) +
                            ":" + mig.getMigrationData().getMigrationNotes())
                    w.setCalcStateConfig(null)
                    cache.addOrChangeWaypoint(w, true)
                    actionAfterMigration.run()
                })
                .confirm(() -> {
                    w.setUserNote(w.getUserNote() + "\n" + LocalizationUtils.getString(R.string.calccoord_migrate_migrate_usernote_praefix) +
                            ":" + mig.getMigrationData().getMigrationNotes())
                    for (Map.Entry<String, String> newVar : mig.getNewCacheVariables().entrySet()) {
                        cache.getVariables().addVariable(newVar.getKey(), newVar.getValue())
                    }
                    cache.getVariables().saveState()
                    val cc: CalculatedCoordinate = CalculatedCoordinate()
                    cc.setType(mig.getMigrationData().getType())
                    cc.setLatitudePattern(mig.getMigrationData().getLatPattern())
                    cc.setLongitudePattern(mig.getMigrationData().getLonPattern())
                    w.setCalcStateConfig(cc.toConfig())
                    cache.addOrChangeWaypoint(w, true)
                    actionAfterMigration.run()
                }, actionAfterMigration)
    }

    private static String createNewUniqueVar(final String oldVar, final Set<String> existingVars) {
        //for now, lets append numbers
        Int idx = 2
        while (existingVars.contains(oldVar + idx)) {
            idx++
        }
        return oldVar + idx
    }

}
