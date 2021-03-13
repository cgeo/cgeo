package cgeo.geocaching.storage;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class SqlBuilderTest {

    @Test
    public void simple() {
        final SqlBuilder sb = new SqlBuilder("mytable", new String[]{ "col1", "col2"});
        assertThat(sb.getSql()).isEqualTo("SELECT col1, col2 FROM mytable t");
    }

    @Test
    public void simpleWhere() {
        final SqlBuilder sb = new SqlBuilder("mytable", new String[]{ "col1", "col2"}).addWhere("col1='abc'");
        assertThat(sb.getSql()).isEqualTo("SELECT col1, col2 FROM mytable t WHERE (col1='abc')");
    }

    @Test
    public void complexWhere() {
        final SqlBuilder sb = new SqlBuilder("mytable", new String[]{ "col1", "col2"})
            .addWhere("col1='abc'")
            .openWhere(SqlBuilder.WhereType.OR).addWhere("col1=3").addWhere("col2=4").closeWhere()
            .openWhere(SqlBuilder.WhereType.OR).closeWhere()
            .openWhere(SqlBuilder.WhereType.NOT).addWhere("x=y").closeWhere()
            .openWhere(SqlBuilder.WhereType.NOT).addWhere("");
        assertThat(sb.getSql()).isEqualTo("SELECT col1, col2 FROM mytable t WHERE (col1='abc') AND ((col1=3) OR (col2=4)) AND (NOT(x=y))");
    }

    @Test
    public void simpleJoin() {
        final SqlBuilder sb = new SqlBuilder("mytable", new String[]{ "col1", "col2"});
        final String tid = sb.getNewTableId();
        sb.addTable("joinedtable " + tid).addJoin(sb.getMainTableId() + ".id=" + tid + ".id");
        sb.addWhere(tid + ".id is not null");
        assertThat(sb.getSql()).isEqualTo("SELECT col1, col2 FROM mytable t, joinedtable " + tid + " ON t.id=" + tid + ".id WHERE (" + tid + ".id is not null)");
    }

    @Test
    public void simpleOrder() {
        final SqlBuilder sb = new SqlBuilder("mytable", new String[]{ "col1", "col2"});
        sb.addOrder("").addOrder("col1 asc").addOrder("col2 desc");
        assertThat(sb.getSql()).isEqualTo("SELECT col1, col2 FROM mytable t ORDER BY (col1 asc), (col2 desc)");
    }

}
