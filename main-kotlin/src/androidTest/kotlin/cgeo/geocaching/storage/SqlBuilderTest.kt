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

package cgeo.geocaching.storage

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class SqlBuilderTest {

    @Test
    public Unit simple() {
        val sb: SqlBuilder = SqlBuilder("mytable", String[]{"col1", "col2"})
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t")
    }

    @Test
    public Unit simpleWhere() {
        val sb: SqlBuilder = SqlBuilder("mytable", String[]{"col1", "col2"}).addWhere("col1='abc'")
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t WHERE (col1='abc')")
    }

    @Test
    public Unit complexWhere() {
        val sb: SqlBuilder = SqlBuilder("mytable", String[]{"col1", "col2"})
                .addWhere("col1='abc'")
                .openWhere(SqlBuilder.WhereType.OR).addWhere("col1=3").addWhere("col2=4").closeWhere()
                .openWhere(SqlBuilder.WhereType.OR).closeWhere()
                .openWhere(SqlBuilder.WhereType.NOT).addWhere("x=y").closeWhere()
                .openWhere(SqlBuilder.WhereType.NOT).addWhere("")
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t WHERE (col1='abc') AND ((col1=3) OR (col2=4)) AND (NOT(x=y))")
    }

    @Test
    public Unit simpleJoin() {
        val sb: SqlBuilder = SqlBuilder("mytable", String[]{"col1", "col2"})
        val tid: String = sb.getNewTableId()
        sb.addJoin("LEFT JOIN joinedtable " + tid + " ON " + sb.getMainTableId() + ".id=" + tid + ".id")
        sb.addWhere(tid + ".id is not null")
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t LEFT JOIN joinedtable " + tid + " ON t.id=" + tid + ".id WHERE (" + tid + ".id is not null)")
    }

    @Test
    public Unit simpleOrder() {
        val sb: SqlBuilder = SqlBuilder("mytable", String[]{"col1", "col2"})
        sb.addOrder("").addOrder("col1 asc").addOrder("col2 desc").addOrder("col3", true)
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t ORDER BY col1 asc, col2 desc, (col3) DESC")
    }

    @Test
    public Unit whereAlwaysInclude() {
        val sb: SqlBuilder = SqlBuilder("mytable", String[]{"col1", "col2"})
        sb.openWhere(SqlBuilder.WhereType.NOT).addWhereAlwaysInclude().closeWhere()
        sb.addWhereAlwaysInclude()
        sb.openWhere(SqlBuilder.WhereType.NOT).openWhere(SqlBuilder.WhereType.NOT).addWhereAlwaysInclude().closeAllOpenWheres()
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t WHERE (NOT(1=0)) AND (1=1) AND (NOT(NOT(1=1)))")
    }

    @Test
    public Unit limit() {
        val sb: SqlBuilder = SqlBuilder("mytable", String[]{"col1", "col2"})
        sb.setLimit(100)
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t LIMIT 100")
    }

    @Test
    public Unit countUnlimited() {
        val sb: SqlBuilder = SqlBuilder("mytable", String[]{"col1", "col2"})
        sb.addWhere("col1 = '5'")
        sb.addOrder("col1", false)
        sb.setLimit(100)
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t WHERE (col1 = '5') ORDER BY (col1) ASC LIMIT 100")

        //order and limit shall be removed!
        assertThat(sb.getSqlForUnlimitedCount()).isEqualTo("SELECT count(*) FROM mytable t WHERE (col1 = '5')")
    }

    @Test
    public Unit complexWhereWithArgs() {
        val sb: SqlBuilder = SqlBuilder("mytable", String[]{"col1", "col2"})
                .addWhere("col1=?", String[]{"abc"})
                .openWhere(SqlBuilder.WhereType.OR).addWhere("col1=?", String[]{"3"}).addWhere("col2=?", String[]{"4"}).closeWhere()
                .openWhere(SqlBuilder.WhereType.OR).closeWhere()
                .openWhere(SqlBuilder.WhereType.NOT).addWhere("x=y").closeWhere()
                .openWhere(SqlBuilder.WhereType.NOT).addWhere("")
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t WHERE (col1=?) AND ((col1=?) OR (col2=?)) AND (NOT(x=y))")
        assertThat(sb.getSqlWhereArgsArray()).isEqualTo(String[]{"abc", "3", "4"})
    }


}
