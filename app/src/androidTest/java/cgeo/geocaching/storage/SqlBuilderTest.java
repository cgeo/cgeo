package cgeo.geocaching.storage;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class SqlBuilderTest {

    @Test
    public void simple() {
        final SqlBuilder sb = new SqlBuilder("mytable", new String[]{"col1", "col2"});
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t");
    }

    @Test
    public void simpleWhere() {
        final SqlBuilder sb = new SqlBuilder("mytable", new String[]{"col1", "col2"}).addWhere("col1='abc'");
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t WHERE (col1='abc')");
    }

    @Test
    public void complexWhere() {
        final SqlBuilder sb = new SqlBuilder("mytable", new String[]{"col1", "col2"})
                .addWhere("col1='abc'")
                .openWhere(SqlBuilder.WhereType.OR).addWhere("col1=3").addWhere("col2=4").closeWhere()
                .openWhere(SqlBuilder.WhereType.OR).closeWhere()
                .openWhere(SqlBuilder.WhereType.NOT).addWhere("x=y").closeWhere()
                .openWhere(SqlBuilder.WhereType.NOT).addWhere("");
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t WHERE (col1='abc') AND ((col1=3) OR (col2=4)) AND (NOT(x=y))");
    }

    @Test
    public void simpleJoin() {
        final SqlBuilder sb = new SqlBuilder("mytable", new String[]{"col1", "col2"});
        final String tid = sb.getNewTableId();
        sb.addJoin("LEFT JOIN joinedtable " + tid + " ON " + sb.getMainTableId() + ".id=" + tid + ".id");
        sb.addWhere(tid + ".id is not null");
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t LEFT JOIN joinedtable " + tid + " ON t.id=" + tid + ".id WHERE (" + tid + ".id is not null)");
    }

    @Test
    public void simpleOrder() {
        final SqlBuilder sb = new SqlBuilder("mytable", new String[]{"col1", "col2"});
        sb.addOrder("").addOrder("col1 asc").addOrder("col2 desc").addOrder("col3", true);
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t ORDER BY col1 asc, col2 desc, (col3) DESC");
    }

    @Test
    public void whereAlwaysInclude() {
        final SqlBuilder sb = new SqlBuilder("mytable", new String[]{"col1", "col2"});
        sb.openWhere(SqlBuilder.WhereType.NOT).addWhereAlwaysInclude().closeWhere();
        sb.addWhereAlwaysInclude();
        sb.openWhere(SqlBuilder.WhereType.NOT).openWhere(SqlBuilder.WhereType.NOT).addWhereAlwaysInclude().closeAllOpenWheres();
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t WHERE (NOT(1=0)) AND (1=1) AND (NOT(NOT(1=1)))");
    }

    @Test
    public void limit() {
        final SqlBuilder sb = new SqlBuilder("mytable", new String[]{"col1", "col2"});
        sb.setLimit(100);
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t LIMIT 100");
    }

    @Test
    public void countUnlimited() {
        final SqlBuilder sb = new SqlBuilder("mytable", new String[]{"col1", "col2"});
        sb.addWhere("col1 = '5'");
        sb.addOrder("col1", false);
        sb.setLimit(100);
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t WHERE (col1 = '5') ORDER BY (col1) ASC LIMIT 100");

        //order and limit shall be removed!
        assertThat(sb.getSqlForUnlimitedCount()).isEqualTo("SELECT count(*) FROM mytable t WHERE (col1 = '5')");
    }

    @Test
    public void complexWhereWithArgs() {
        final SqlBuilder sb = new SqlBuilder("mytable", new String[]{"col1", "col2"})
                .addWhere("col1=?", new String[]{"abc"})
                .openWhere(SqlBuilder.WhereType.OR).addWhere("col1=?", new String[]{"3"}).addWhere("col2=?", new String[]{"4"}).closeWhere()
                .openWhere(SqlBuilder.WhereType.OR).closeWhere()
                .openWhere(SqlBuilder.WhereType.NOT).addWhere("x=y").closeWhere()
                .openWhere(SqlBuilder.WhereType.NOT).addWhere("");
        assertThat(sb.getSql()).isEqualTo("SELECT t.col1, t.col2 FROM mytable t WHERE (col1=?) AND ((col1=?) OR (col2=?)) AND (NOT(x=y))");
        assertThat(sb.getSqlWhereArgsArray()).isEqualTo(new String[]{"abc", "3", "4"});
    }


}
