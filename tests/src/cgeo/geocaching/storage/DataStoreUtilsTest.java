package cgeo.geocaching.storage;

import static cgeo.geocaching.storage.DataStoreUtils.DBColumn;
import static cgeo.geocaching.storage.DataStoreUtils.DBTable;
import static cgeo.geocaching.storage.DataStoreUtils.DBType;
import static cgeo.geocaching.storage.DataStoreUtils.QueryParams;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;


public class DataStoreUtilsTest {

    //set up a simple test table configuration for tests
    //TODO I would name this class "DBTestTable", but codacy does not allow that, needs conformance to regex '[A-Z][a-zA-Z0-9]+(Utils?|Helper|Constants)'
    public static class DBTestTableUtils {
        public static final DBTable T = new DBTable("cg_test");
        public static final DBColumn C_ID = T.add("_id", DBType.INTEGER, "PRIMARY KEY AUTOINCREMENT");
        public static final DBColumn C_GEOCODE = T.add("geocode", DBType.TEXT, "NOT NULL");
        public static final DBColumn C_TESTNAME = T.add("name", DBType.TEXT);
    }

    @Test
    public void accessBasicProperties() {
        assertThat(DBTestTableUtils.T.name).isEqualTo("cg_test");
        assertThat(DBTestTableUtils.T.toString()).isEqualTo("cg_test");
        assertThat(DBTestTableUtils.C_TESTNAME.toString()).isEqualTo("name");
        assertThat(DBTestTableUtils.T.getColumnByName(DBTestTableUtils.C_TESTNAME.name)).isEqualTo(DBTestTableUtils.C_TESTNAME);
        assertThat(DBTestTableUtils.T.getColumnByName("nonexisting")).isNull();
        assertThat(DBTestTableUtils.T.getColumnsAsArray().length).isEqualTo(3);
    }

    @Test
    public void sqlStatementsForTableManipulation() {
        assertSqlEquality(DBTestTableUtils.T.getSqlCreateTable(),
                "create table if not exists cg_test(_id INTEGER PRIMARY KEY AUTOINCREMENT, geocode TEXT NOT NULL, name TEXT)"
        );
        assertSqlEquality(DBTestTableUtils.T.getSqlCreateColumn(DBTestTableUtils.C_GEOCODE),
                "alter table cg_test add column geocode TEXT NOT NULL"
        );
        assertSqlEquality(DBTestTableUtils.T.getSqlDropTable(),
                "DROP TABLE IF EXISTS cg_test");
    }

    @Test
    public void sqlQuerying() {

        QueryParams qp = new QueryParams.Builder()
                .addColumn(DBTestTableUtils.C_TESTNAME)
                .setWhereClause(DBTestTableUtils.C_GEOCODE + " = ?")
                .setGroupBy("" + DBTestTableUtils.C_TESTNAME)
                .setHaving(DBTestTableUtils.C_TESTNAME + " is not null")
                .setLimit("10")
                .build();
        assertSqlEquality(qp.toString(),
        "select name from cg_test where geocode = ? group by name having name is not null limit 10"
        );
        qp = new QueryParams.Builder()
                .addColumn(DBTestTableUtils.C_TESTNAME)
                .addColumn(DBTestTableUtils.C_ID)
                .build();
        assertSqlEquality(qp.toString(),
                "select name,_id from cg_test"
        );


    }

    private void assertSqlEquality(final String sql, final String expectedSql) {
        final String sqlCleaned = sql.replaceAll("[\\s]", "").toUpperCase();
        final String expectedSqlCleaned = expectedSql.replaceAll("[\\s]", "").toUpperCase();
        assertThat(sqlCleaned).isEqualTo(expectedSqlCleaned);
    }


}
