package cgeo.geocaching.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class LegacyCalculatedCoordinateMigratorTest {

//    private static final String JSON_SAMPLE_1 = "{\"format\":2,\"plainLat\":\"N 53° 33.065'\",\"plainLon\":\"E 009° 59.621'\",\"latHemisphere\":78,\"lonHemisphere\":69,\"buttons\":[{\"type\":0,\"inputVal\":53,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":45,\"customChar\":0},{\"type\":1,\"inputVal\":51,\"autoChar\":65,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":54,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":66,\"customChar\":0},{\"type\":1,\"inputVal\":57,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":67,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":67,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":67,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":67,\"customChar\":0},{\"type\":0,\"inputVal\":54,\"autoChar\":67,\"customChar\":0},{\"type\":2,\"inputVal\":50,\"autoChar\":67,\"customChar\":0},{\"type\":0,\"inputVal\":49,\"autoChar\":67,\"customChar\":0}],\"equations\":[{\"name\":65,\"expression\":\"a+b\"},{\"name\":66,\"expression\":\"5\"}],\"freeVariables\":[{\"name\":97,\"expression\":\"3\"},{\"name\":98,\"expression\":\"4\"}]}";
//    private static final String JSON_SAMPLE_2 = "{\"format\":0,\"plainLat\":\"N 53° 33.06A'\",\"plainLon\":\"E 009° 59.B21'\",\"latHemisphere\":78,\"lonHemisphere\":69,\"buttons\":[{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0}],\"equations\":[{\"name\":65,\"expression\":\"a+b\"},{\"name\":66,\"expression\":\"5\"}],\"freeVariables\":[{\"name\":97,\"expression\":\"3\"},{\"name\":98,\"expression\":\"4\"}]}";
//    private static final String JSON_SAMPLE_3 = "{\"format\":2,\"plainLat\":\"N 53° 32.555'\",\"plainLon\":\"E 009° 59.333'\",\"latHemisphere\":78,\"lonHemisphere\":69,\"buttons\":[{\"type\":0,\"inputVal\":53,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":50,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":1,\"inputVal\":53,\"autoChar\":65,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":66,\"customChar\":0}],\"equations\":[{\"name\":65,\"expression\":\"5\"}],\"freeVariables\":[]}";

    private static final String JSON_DEGREE_EXAMPLE_1 = "{\"format\":1,\"plainLat\":\"N 53° 33.065'\",\"plainLon\":\"E 009° 59.621'\",\"latHemisphere\":83,\"lonHemisphere\":87,\"buttons\":[{\"type\":0,\"inputVal\":53,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":1,\"inputVal\":53,\"autoChar\":65,\"customChar\":0},{\"type\":2,\"inputVal\":53,\"autoChar\":66,\"customChar\":0},{\"type\":3,\"inputVal\":49,\"autoChar\":66,\"customChar\":70},{\"type\":0,\"inputVal\":48,\"autoChar\":71,\"customChar\":0},{\"type\":0,\"inputVal\":56,\"autoChar\":71,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":71,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":71,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":71,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":71,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":71,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":71,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":71,\"customChar\":0},{\"type\":1,\"inputVal\":57,\"autoChar\":71,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":72,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":72,\"customChar\":0},{\"type\":2,\"inputVal\":54,\"autoChar\":72,\"customChar\":0},{\"type\":0,\"inputVal\":56,\"autoChar\":72,\"customChar\":0}],\"equations\":[{\"name\":65,\"expression\":\"3\"},{\"name\":70,\"expression\":\"7\"},{\"name\":71,\"expression\":\"a+6\"}],\"freeVariables\":[{\"name\":97,\"expression\":\"6\"}]}";
    private static final String JSON_DEGMIN_EXAMPLE_1 = "{\"format\":2,\"plainLat\":\"N 53° 33.065'\",\"plainLon\":\"E 009° 59.621'\",\"latHemisphere\":78,\"lonHemisphere\":69,\"buttons\":[{\"type\":0,\"inputVal\":53,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":45,\"customChar\":0},{\"type\":1,\"inputVal\":51,\"autoChar\":65,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":66,\"customChar\":0},{\"type\":2,\"inputVal\":54,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":66,\"customChar\":0},{\"type\":3,\"inputVal\":57,\"autoChar\":66,\"customChar\":82},{\"type\":0,\"inputVal\":0,\"autoChar\":83,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":83,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":83,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":83,\"customChar\":0},{\"type\":0,\"inputVal\":54,\"autoChar\":83,\"customChar\":0},{\"type\":0,\"inputVal\":50,\"autoChar\":83,\"customChar\":0},{\"type\":0,\"inputVal\":49,\"autoChar\":83,\"customChar\":0}],\"equations\":[{\"name\":65,\"expression\":\"a+b\"},{\"name\":82,\"expression\":\"4\"}],\"freeVariables\":[{\"name\":97,\"expression\":\"3\"},{\"name\":98,\"expression\":\"4\"}]}";
    private static final String JSON_DEGMINSEC_EXAMPLE_1 = "{\"format\":3,\"plainLat\":\"N 53° 33.065'\",\"plainLon\":\"E 009° 59.621'\",\"latHemisphere\":78,\"lonHemisphere\":87,\"buttons\":[{\"type\":0,\"inputVal\":53,\"autoChar\":45,\"customChar\":0},{\"type\":3,\"inputVal\":51,\"autoChar\":45,\"customChar\":87},{\"type\":0,\"inputVal\":51,\"autoChar\":88,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":88,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":88,\"customChar\":0},{\"type\":2,\"inputVal\":51,\"autoChar\":88,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":88,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":88,\"customChar\":0},{\"type\":2,\"inputVal\":56,\"autoChar\":88,\"customChar\":0},{\"type\":3,\"inputVal\":57,\"autoChar\":88,\"customChar\":68},{\"type\":0,\"inputVal\":57,\"autoChar\":69,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":69,\"customChar\":0},{\"type\":2,\"inputVal\":48,\"autoChar\":69,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":69,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":69,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":69,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":69,\"customChar\":0},{\"type\":3,\"inputVal\":55,\"autoChar\":69,\"customChar\":65},{\"type\":0,\"inputVal\":0,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":50,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":66,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":66,\"customChar\":0}],\"equations\":[{\"name\":65,\"expression\":\"1\"},{\"name\":68,\"expression\":\"3\"},{\"name\":87,\"expression\":\"2\"}],\"freeVariables\":[]}";
    private static final String JSON_PLAIN_EXAMPLE_1 = "{\"format\":0,\"plainLat\":\"N 53° 33.AAA\",\"plainLon\":\"E 009° 59.BCD'\",\"latHemisphere\":78,\"lonHemisphere\":69,\"buttons\":[{\"type\":0,\"inputVal\":53,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":56,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":48,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":57,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":51,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":55,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":0,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":50,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":45,\"customChar\":0},{\"type\":0,\"inputVal\":53,\"autoChar\":45,\"customChar\":0}],\"equations\":[{\"name\":65,\"expression\":\"1\"},{\"name\":66,\"expression\":\"2\"},{\"name\":67,\"expression\":\"3\"},{\"name\":68,\"expression\":\"4\"}],\"freeVariables\":[]}";

    private static final String[][] EXPECTED_DATA = new String[][]{
            new String[]{JSON_DEGREE_EXAMPLE_1, CalculatedCoordinateType.DEGREE.toString(), "S53.A_F08°", "W009.G93_8°"},
            new String[]{JSON_DEGMIN_EXAMPLE_1, CalculatedCoordinateType.DEGREE_MINUTE.toString(), "N53°A3.0_5'", "E009°5R.621'"},
            new String[]{JSON_DEGMINSEC_EXAMPLE_1, CalculatedCoordinateType.DEGREE_MINUTE_SEC.toString(), "N5W°33'0_._D9\"", "W0_9°59'3A.259\""},
            new String[]{JSON_PLAIN_EXAMPLE_1, CalculatedCoordinateType.PLAIN.toString(), "N 53° 33.AAA", "E 009° 59.BCD'"}
    };

    @Test
    public void migrationExamplesBase() {
        for (String[] testData : EXPECTED_DATA) {
            final LegacyCalculatedCoordinateMigrator.WaypointMigrationData ccm = LegacyCalculatedCoordinateMigrator.WaypointMigrationData.createFromJson(-1, "", testData[0]);
            assertThat(ccm.getType().toString()).isEqualTo(testData[1]);
            assertThat(ccm.getLatPattern()).isEqualTo(testData[2]);
            assertThat(ccm.getLonPattern()).isEqualTo(testData[3]);
        }
    }

    @Test
    public void simpleMigration() {
        final Map<String, String> initialVars = createMap("A", "2");

        final Map<String, String> varMap = createMap("A", "B+2", "C", "A-2");

        final LegacyCalculatedCoordinateMigrator.WaypointMigrationData wmd =
                LegacyCalculatedCoordinateMigrator.WaypointMigrationData.create(CalculatedCoordinateType.DEGREE_MINUTE, "ABC", "DEF", varMap);

        final LegacyCalculatedCoordinateMigrator ccm = new LegacyCalculatedCoordinateMigrator(initialVars, wmd);
        assertThat(ccm.getMigrationData().getLatPattern()).isEqualTo("($A2)BC");
        assertThat(ccm.getMigrationData().getLonPattern()).isEqualTo("DEF");
        assertThat(ccm.getNewCacheVariables().get("A2")).isEqualTo("B+2");
        assertThat(ccm.getNewCacheVariables().get("C")).isEqualTo("($A2)-2");
    }

    @Test
    public void migrateWithSquareParenthesis() {
        final Map<String, String> varMap = createMap("A", "1", "B", "[2+3]*5", "C", "3", "D", "3", "E", "2", "F", "1");

        final LegacyCalculatedCoordinateMigrator.WaypointMigrationData wmd =
                LegacyCalculatedCoordinateMigrator.WaypointMigrationData.create(CalculatedCoordinateType.DEGREE_MINUTE,
                        "N48° 45.[A+B](C+D)[E+F]", "E 009° 05.000", varMap);

        final LegacyCalculatedCoordinateMigrator ccm = new LegacyCalculatedCoordinateMigrator(Collections.emptyMap(), wmd);
        assertThat(ccm.getMigrationData().getLatPattern()).isEqualTo("N48° 45.(A+B)(C+D)(E+F)");
        assertThat(ccm.getMigrationData().getLonPattern()).isEqualTo("E 009° 05.000");
        assertThat(ccm.getNewCacheVariables().size()).isEqualTo(6);
        assertThat(ccm.getNewCacheVariables().get("C")).isEqualTo("3");
        assertThat(ccm.getNewCacheVariables().get("B")).isEqualTo("(2+3)*5");

        varMap.put("B", "(2+3)*5");
        assertThat(ccm.getNewCacheVariables()).containsAllEntriesOf(varMap);
    }

    private static Map<String, String> createMap(final String... keyValue) {
        final Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValue.length; i += 2) {
            map.put(keyValue[i], keyValue[i + 1]);
        }
        return map;
    }

}
