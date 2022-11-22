package cgeo.geocaching.models;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.LocalizationUtils;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An instance of this class represents the state of an OLD calculated waypoint
 */
public class LegacyCalculatedCoordinateMigrator {

//    Kept here for reference: legacy code used to read legacy calculated coordinate json data
//    format = Settings.CoordInputFormatEnum.fromInt(json.optInt("format", Settings.CoordInputFormatEnum.DEFAULT_INT_VALUE));
//    plainLat = json.optString("plainLat");
//    plainLon = json.optString("plainLon");
//    latHemisphere = (char) json.optInt("latHemisphere", ERROR_CHAR);
//    lonHemisphere = (char) json.optInt("lonHemisphere", ERROR_CHAR);
//    buttons       = createJSONAbleList(json.optJSONArray("buttons"),       new ButtonDataFactory());
//    equations     = createJSONAbleList(json.optJSONArray("equations"),     new VariableDataFactory());
//    freeVariables = createJSONAbleList(json.optJSONArray("freeVariables"), new VariableDataFactory());
//    variableBank = new ArrayList<>(); // "variableBank" intentionally not loaded.

    private final Map<String, String> initialVars;
    private final WaypointMigrationData waypointMigrationData;
    private final Map<String, String> newCacheVariables = new HashMap<>();

    private enum MigrationCalculatedCoordinateType {
        PLAIN(CalculatedCoordinateType.PLAIN, null, null),
        DEG(CalculatedCoordinateType.DEGREE, "**.----*****°", "***.----*****°"),
        MIN(CalculatedCoordinateType.DEGREE_MINUTE, "**°**.----***'", "***°**.----***'"),
        SEC(CalculatedCoordinateType.DEGREE_MINUTE_SEC, "**°**'**.--***\"", "***°**'**.--***\"");

        public final CalculatedCoordinateType type;
        public String latPattern;
        public String lonPattern;

        MigrationCalculatedCoordinateType(final CalculatedCoordinateType type, final String latPattern, final String lonPattern) {
            this.type = type;
            this.latPattern = latPattern;
            this.lonPattern = lonPattern;
        }

        public static MigrationCalculatedCoordinateType fromMigration(final int migValue) {
            return MigrationCalculatedCoordinateType.values()[migValue];
        }
    }

    private enum ButtonType { INPUT, AUTO, BLANK, CUSTOM }

    public static class WaypointMigrationData {

        private int id;
        private String name;

        private CalculatedCoordinateType type;
        private String latPattern;
        private String lonPattern;
        private final Map<String, String> variables = new HashMap<>();

        private String migrationNotes;

        /**
         * used for test cases
         */
        public static WaypointMigrationData create(final CalculatedCoordinateType type, final String latPattern, final String lonPattern, final Map<String, String> vars) {
            final WaypointMigrationData ccm = new WaypointMigrationData();
            ccm.type = type;
            ccm.latPattern = convertPattern(latPattern);
            ccm.lonPattern = convertPattern(lonPattern);
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                ccm.variables.put(entry.getKey(), convertPattern(entry.getValue()));
            }
            ccm.createMigNotes();

            return ccm;
        }

        public static WaypointMigrationData createFromJson(final int id, final String name, final String jsonString) {
            if (jsonString == null) {
                return null;
            }

            final WaypointMigrationData ccm = new WaypointMigrationData();
            ccm.id = id;
            ccm.name = name;

            try {
                final JSONObject json = new JSONObject(jsonString);
                final MigrationCalculatedCoordinateType ccmType = MigrationCalculatedCoordinateType.fromMigration(json.optInt("format", 2));
                ccm.type = ccmType.type;
                if (ccm.type == CalculatedCoordinateType.PLAIN) {
                    ccm.latPattern = convertPattern(json.optString("plainLat"));
                    ccm.lonPattern = convertPattern(json.optString("plainLon"));
                } else {
                    ccm.latPattern = convertPattern("" + (char) json.optInt("latHemisphere") + parseButtonDataFromJson(json.optJSONArray("buttons"), 0, ccmType.latPattern));
                    ccm.lonPattern = convertPattern("" + (char) json.optInt("lonHemisphere") + parseButtonDataFromJson(json.optJSONArray("buttons"), 11, ccmType.lonPattern));
                }

                addVariablesFromJson(ccm.variables, json.optJSONArray("equations"));
                addVariablesFromJson(ccm.variables, json.optJSONArray("freeVariables"));

                ccm.createMigNotes();

            } catch (JSONException je) {
                return null;
            }

            return ccm;
        }

        private static String convertPattern(final String degreePattern) {
            if (degreePattern == null) {
                return "";
            }
            return degreePattern.trim().replace('[', '(').replace(']', ')');
        }

        private void createMigNotes() {
            final StringBuilder sb = new StringBuilder("[" + latPattern + " • " + lonPattern + " ");
            for (Map.Entry<String, String> v : variables.entrySet()) {
                sb.append("|").append(v.getKey()).append("=").append(v.getValue());
            }
            sb.append("]");

            migrationNotes = sb.toString();
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getMigrationNotes() {
            return migrationNotes;
        }

        public void replaceVars(final Map<String, String> replacements) {
            this.latPattern = replaceVars(this.latPattern, replacements);
            this.lonPattern = replaceVars(this.lonPattern, replacements);
            for (String v : variables.keySet()) {
                final String oldFormula = variables.get(v);
                variables.put(v, replaceVars(oldFormula, replacements));
            }

            for (Map.Entry<String, String> e : replacements.entrySet()) {
                final String formula = this.variables.get(e.getKey());
                if (formula != null) {
                    this.variables.remove(e.getKey());
                    this.variables.put(e.getValue(), formula);
                }
            }
        }

        private String replaceVars(final String formula, final Map<String, String> replacements) {
            final Matcher m = Pattern.compile("[a-zA-Z]").matcher(formula);
            final StringBuffer sb = new StringBuffer();
            while (m.find()) {
                final String oldVar = m.group();
                final String newVar;
                if (replacements.containsKey(oldVar)) {
                    final String v = replacements.get(oldVar);
                    newVar = v.length() == 1 ? v : "(\\$" + v + ")";
                } else {
                    newVar = oldVar;
                }
                m.appendReplacement(sb, newVar);
            }
            m.appendTail(sb);
            return sb.toString();
        }

        private static String parseButtonDataFromJson(final JSONArray ja, final int start, final String pattern) {
            if (ja == null) {
                return "";
            }
            final StringBuilder sb = new StringBuilder();
            int pos = start;
            for (char p : pattern.toCharArray()) {
                switch (p) {
                    case '*':
                        sb.append(getCharFromButton(ja, pos));
                        pos++;
                        break;
                    case '-':
                        pos++;
                        break;
                    default:
                        sb.append(p);
                        break;
                }
            }
            return sb.toString();
        }

        private static char getCharFromButton(final JSONArray ja, final int pos) {
            try {
                final JSONObject jo = ja.getJSONObject(pos);
                final ButtonType bt = ButtonType.values()[jo.getInt("type")];
                final char c;
                switch (bt) {
                    case INPUT:
                        c = ((char) jo.getInt("inputVal"));
                        break;
                    case AUTO:
                        c = ((char) jo.getInt("autoChar"));
                        break;
                    case CUSTOM:
                        c = ((char) jo.getInt("customChar"));
                        break;
                    case BLANK:
                    default:
                        c = '_';
                        break;
                }
                return c;
            } catch (JSONException je) {
                return ' ';
            }
        }

        private static void addVariablesFromJson(final Map<String, String> varMap, final JSONArray ja) {
            if (ja == null) {
                return;
            }
            for (int i = 0; i < ja.length(); i++) {
                try {
                    final JSONObject jo = ja.getJSONObject(i);
                    final String varName = "" + ((char) jo.getInt("name"));
                    final String formula = convertPattern(jo.getString("expression"));
                    varMap.put(varName, formula);
                } catch (JSONException je) {
                    //do nothing
                }
            }
        }

        public CalculatedCoordinateType getType() {
            return type;
        }

        public Map<String, String> getVariables() {
            return variables;
        }

        public String getLatPattern() {
            return latPattern;
        }

        public String getLonPattern() {
            return lonPattern;
        }

        @NonNull
        @Override
        public String toString() {
            return "Type:" + type + ",lat:" + latPattern + ",lon:" + lonPattern + ", vars:" + variables;
        }
    }

    public LegacyCalculatedCoordinateMigrator(final Geocache cache, final Waypoint wp) {
        this(cache.getVariables().toMap(), WaypointMigrationData.createFromJson(wp.getId(), wp.getName(), wp.getCalcStateConfig()));
    }

    public LegacyCalculatedCoordinateMigrator(final Map<String, String> initialVars, final WaypointMigrationData wmd) {
        this.initialVars = initialVars;
        waypointMigrationData = wmd;
        final Map<String, String> cacheVars = new HashMap<>();
        for (Map.Entry<String, String> iv : initialVars.entrySet()) {
            if (iv.getKey() != null && iv.getValue() != null) {
                cacheVars.put(iv.getKey(), iv.getValue());
            }
        }

        //find out what needs to be replaced and do it
        final Map<String, String> replacements = new HashMap<>();
        for (String v : wmd.getVariables().keySet()) {
            if (cacheVars.containsKey(v) && !cacheVars.get(v).equals(wmd.getVariables().get(v))) {
                final String newVar = createNewUniqueVar(v, cacheVars.keySet());
                replacements.put(v, newVar);
            }
        }
        wmd.replaceVars(replacements);

        //vars are now cache-global-unique ->  add to global var map
        cacheVars.putAll(wmd.getVariables());
        newCacheVariables.putAll(wmd.getVariables());

    }

    public String getMigrationInformationMarkup() {
        final String migNotes = "`" + waypointMigrationData.getMigrationNotes() + "`";
        final String newCoordinate = "`" + waypointMigrationData.getLatPattern() + " • " + waypointMigrationData.getLonPattern() + "`";
        final StringBuilder newVariables = new StringBuilder();
        for (Map.Entry<String, String> ve : newCacheVariables.entrySet()) {
            newVariables.append("\n- `").append(ve.getKey()).append(" = ").append(ve.getValue()).append("`");
        }

        final String migrateButtonName = "**" + LocalizationUtils.getString(R.string.calccoord_migrate_migrate) + "**";
        final String cancelButtonName = "**" + LocalizationUtils.getString(R.string.calccoord_migrate_cancel) + "**";
        final String dismissButtonName = "**" + LocalizationUtils.getString(R.string.calccoord_migrate_dismiss) + "**";

        return LocalizationUtils.getString(R.string.calccoord_migrate_infotext_markdown, migNotes, newCoordinate, newVariables.toString(),
                migrateButtonName, dismissButtonName, cancelButtonName);
    }

    @NonNull
    @Override
    public String toString() {
        return "wmd:" + waypointMigrationData + ", ivs:" + initialVars.toString() + ", ncvs:" + newCacheVariables;
    }

    public WaypointMigrationData getMigrationData() {
        return waypointMigrationData;
    }

    public Map<String, String> getNewCacheVariables() {
        return newCacheVariables;
    }

    public static boolean needsMigration(final Waypoint w) {
        return WaypointMigrationData.createFromJson(w.getId(), w.getName(), w.getCalcStateConfig()) != null;
    }

    public static void performMigration(final Context ctx, final Geocache cache, final Waypoint w, final Runnable actionAfterMigration) {
        if (!needsMigration(w)) {
            actionAfterMigration.run();
            return;
        }
        final LegacyCalculatedCoordinateMigrator mig = new LegacyCalculatedCoordinateMigrator(cache, w);
        SimpleDialog.ofContext(ctx).setTitle(TextParam.id(R.string.calccoord_migrate_title))
                .setMessage(TextParam.text(mig.getMigrationInformationMarkup()).setMarkdown(true))
                .setPositiveButton(TextParam.id(R.string.calccoord_migrate_migrate))
                .setNegativeButton(TextParam.id(R.string.calccoord_migrate_cancel))
                .setNeutralButton(TextParam.id(R.string.calccoord_migrate_dismiss))
                .show((v, i) -> {
                    w.setUserNote(w.getUserNote() + "\n" + LocalizationUtils.getString(R.string.calccoord_migrate_migrate_usernote_praefix) +
                            ":" + mig.getMigrationData().getMigrationNotes());
                    for (Map.Entry<String, String> newVar : mig.getNewCacheVariables().entrySet()) {
                        cache.getVariables().addVariable(newVar.getKey(), newVar.getValue());
                    }
                    cache.getVariables().saveState();
                    final CalculatedCoordinate cc = new CalculatedCoordinate();
                    cc.setType(mig.getMigrationData().getType());
                    cc.setLatitudePattern(mig.getMigrationData().getLatPattern());
                    cc.setLongitudePattern(mig.getMigrationData().getLonPattern());
                    w.setCalcStateConfig(cc.toConfig());
                    cache.addOrChangeWaypoint(w, true);
                    actionAfterMigration.run();
                }, (v, i) -> actionAfterMigration.run(), (v, i) -> {
                    //dismiss calculated coordinate data
                    w.setUserNote(w.getUserNote() + "\n" + LocalizationUtils.getString(R.string.calccoord_migrate_dismiss_usernote_praefix) +
                            ":" + mig.getMigrationData().getMigrationNotes());
                    w.setCalcStateConfig(null);
                    cache.addOrChangeWaypoint(w, true);
                    actionAfterMigration.run();
                });
    }

    private static String createNewUniqueVar(final String oldVar, final Set<String> existingVars) {
        //for now, lets append numbers
        int idx = 2;
        while (existingVars.contains(oldVar + idx)) {
            idx++;
        }
        return oldVar + idx;
    }

}
