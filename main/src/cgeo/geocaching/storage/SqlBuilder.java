package cgeo.geocaching.storage;

import cgeo.geocaching.utils.CollectionStream;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;

public class SqlBuilder {

    public enum WhereType { AND, OR, NOT }

    private int tableIdCnt = 1;

    private final String mainTable;
    private final String[] columns;


    private final List<String> tables = new ArrayList<>();
    private final List<String> joins = new ArrayList<>();
    private final List<String> orders = new ArrayList<>();

    private final Stack<ImmutableTriple<WhereType, StringBuilder, List<String>>> whereStack = new Stack<>();
    private boolean whereInvertTrue = false;

    private int limit = -1;


    public SqlBuilder(final String mainTable, final String[] columns) {
        this.mainTable = mainTable;
        this.columns = columns;
        whereStack.push(new ImmutableTriple<>(WhereType.AND, new StringBuilder(), new ArrayList<>()));
    }

    public String getNewTableId() {
        return "tt" + (tableIdCnt++);
    }

    public String getMainTableId() {
        return "t";
    }

    public SqlBuilder addTable(final String table) {
        tables.add(table);
        return this;
    }

    public SqlBuilder addJoin(final String join) {
        joins.add(join);
        return this;
    }

    public SqlBuilder openWhere(final WhereType operator) {
        whereStack.push(new ImmutableTriple<>(operator, new StringBuilder(), new ArrayList<>()));
        if (operator == WhereType.NOT) {
            whereInvertTrue = !whereInvertTrue;
        }
        return this;
    }

    public SqlBuilder addWhere(final String whereClause) {
        return addWhere(whereClause, (List<String>) null);
    }

    public SqlBuilder addWhere(final String whereClause, final String... whereArgs) {
        return addWhere(whereClause, Arrays.asList(whereArgs));
    }

    public SqlBuilder addWhere(final String whereClause, final List<String> whereArgs) {

        if (!StringUtils.isBlank(whereClause) && !whereStack.isEmpty()) {
            final ImmutableTriple<WhereType, StringBuilder, List<String>> w = whereStack.peek();
            if (!w.middle.toString().isEmpty()) {
                w.middle.append(" ").append(w.left == WhereType.NOT ? WhereType.AND : w.left).append(" ");
            } else if (w.left == WhereType.NOT) {
                w.middle.append(w.left);
            }
            w.middle.append("(").append(whereClause).append(")");
            if (whereArgs != null) {
                w.right.addAll(whereArgs);
            }
        }
        return this;
    }

    public SqlBuilder addWhereTrue() {
        return addWhere("1=1");
    }

    /**
     * adds an SQL condition to the where clause which will always lead to an "include" of lines, never to an "exclude".
     * Normally this will be an SQl-Expression always evaluating to "true".
     * However, if we are currently inside a NOT, then it will be an SQL-Expression always evaluating to "false" (hence "true" when negated).
     * This method handles also multilevel NOTs (e.g. inside a NOT(NOT(NOT(x))) it will use "false"-expression, inside NOT(NOT(NOT(NOT(x)))) is will use "true"-expression
     */
    public SqlBuilder addWhereAlwaysInclude() {
        return whereInvertTrue ? addWhere("1=0") : addWhere("1=1");
    }

    public SqlBuilder closeWhere() {
        if (whereStack.size() > 1) {
            final ImmutableTriple<WhereType, StringBuilder, List<String>> current = whereStack.pop();
            if (current.left == WhereType.NOT) {
                whereInvertTrue = !whereInvertTrue;
            }
            addWhere(current.middle.toString(), current.right);
        }
        return this;
    }

    public SqlBuilder addOrder(final String order, final boolean sortDesc) {
        return addOrder("(" + order + ") " + (sortDesc ? "DESC" : "ASC"));
    }

    public SqlBuilder addOrder(final String order) {
        if (!StringUtils.isBlank(order)) {
            orders.add(order);
        }
        return this;
    }

    public SqlBuilder setLimit(final int limit) {
        this.limit = limit;
        return this;
    }

    public boolean allWheresClosed() {
        return whereStack.size() == 1;
    }

    public SqlBuilder closeAllOpenWheres() {
        while (whereStack.size() > 1) {
            closeWhere();
        }
        return this;
    }

    /**
     * Returns SQL as currently defined by this builder.
     *
     * Method does not change the classes state. It may return incomplete SQL e.g. if not all where's are closed yet
     */
    @NonNull
    public String getSql() {
        return constructSqlInternal(CollectionStream.of(columns).map(c -> getMainTableId() + "." + c).toJoinedString(", "), true, true);
    }

    @NonNull
    public List<String> getSqlWhereArgs() {
        return !whereStack.isEmpty() ? whereStack.get(0).right : Collections.emptyList();
    }

    @NonNull
    public String[] getSqlWhereArgsArray() {
        return getSqlWhereArgs().toArray(new String[0]);
    }

    /**
     * Returns an SQL statement ready to count(*) the number of rows which would be returned by this statement. If limit is set, this is ignored
     * Note: if order is set, this is ignored in returend statement as well because order does not change the count(*)
     */
    @NonNull
    public String getSqlForUnlimitedCount() {
        return constructSqlInternal("count(*)", false, false);
    }

    // readability of this method would decrease signigicantly if splitted across submethods
    @SuppressWarnings("PMD.NPathComplexity")
    private String constructSqlInternal(final String columnString, final boolean includeOrderBy, final boolean includeLimit) {

        final StringBuilder sb = new StringBuilder("SELECT " + columnString + " FROM " + mainTable + " " + getMainTableId());
        for (String tab : tables) {
            sb.append(", ").append(tab);
        }

        if (!joins.isEmpty()) {
            for (String join : joins) {
                sb.append(" ").append(join);
            }
        }

        if (!whereStack.isEmpty() && !StringUtils.isBlank(whereStack.get(0).middle.toString())) {
            sb.append(" WHERE ").append(whereStack.get(0).middle.toString());
        }
        if (includeOrderBy && !orders.isEmpty()) {
            sb.append(" ORDER BY ").append(CollectionStream.of(orders).toJoinedString(", "));
        }

        if (includeLimit && limit >= 0) {
            sb.append(" LIMIT ").append(limit);
        }
        return sb.toString();

    }

    /**
     * Helper method for escaping text when building SQL
     */
    public static String escape(final String text) {
        return escape(text, false);
    }

    /**
     * Helper method for escaping text when building SQL
     */
    public static String escape(final String text, final boolean forLike) {
        if (text == null) {
            return "";
        }
        String escapedText = text.replaceAll("'", "''");
        if (forLike) {
            escapedText = escapedText.replaceAll("_", "\\\\_")
                    .replaceAll("%", "\\\\%")
                    .replaceAll("\\\\", "\\\\\\\\");
        }
        return escapedText;
    }

    /**
     * creates an SQL LIKE expression. E.g. given escapedPattern "test%" it will return "like 'test%'".
     * Backslash ('\') can be used to escape % and _. E.g. "test\%%" will result in "like 'test\%%' escape '\'
     */
    public static String createLikeExpression(final String escapedPattern) {
        if (escapedPattern == null) {
            return " IS NULL";
        }
        return " LIKE '" + escapedPattern + "' ESCAPE '\\'";
    }


    @NonNull
    @Override
    public String toString() {
        return getSql();
    }
}
