package cgeo.geocaching.storage;

import cgeo.geocaching.utils.CollectionStream;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;


public class SqlBuilder {

    public enum WhereType { AND, OR, NOT }

    private int tableIdCnt = 1;

    private final String mainTable;
    private final String[] columns;


    private final List<String> tables = new ArrayList<>();
    private final List<String> joins = new ArrayList<>();
    private final List<String> orders = new ArrayList<>();

    private final Stack<ImmutablePair<WhereType, StringBuilder>> whereStack = new Stack<>();
    private boolean whereInvertTrue = false;


    public SqlBuilder(final String mainTable, final String[] columns) {
        this.mainTable = mainTable;
        this.columns = columns;
        whereStack.push(new ImmutablePair<>(WhereType.AND, new StringBuilder()));
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
        whereStack.push(new ImmutablePair<>(operator, new StringBuilder()));
        if (operator == WhereType.NOT) {
            whereInvertTrue = !whereInvertTrue;
        }
        return this;
    }

    public SqlBuilder addWhere(final String whereClause) {
        if (!StringUtils.isBlank(whereClause) && !whereStack.isEmpty()) {
            final ImmutablePair<WhereType, StringBuilder> w = whereStack.peek();
            if (!w.right.toString().isEmpty()) {
                w.right.append(" ").append(w.left == WhereType.NOT ? WhereType.AND : w.left).append(" ");
            } else if (w.left == WhereType.NOT) {
                w.right.append(w.left);
            }
            w.right.append("(").append(whereClause).append(")");
        }
        return this;
    }

    /** adds an SQL "always true" condition to the where clsue */
    public SqlBuilder addWhereAlwaysTrue() {
        return whereInvertTrue ? addWhere("1=0") : addWhere("1=1");
    }

    public SqlBuilder closeWhere() {
        if (whereStack.size() > 1) {
            final ImmutablePair<WhereType, StringBuilder> current = whereStack.pop();
            if (current.left == WhereType.NOT) {
                whereInvertTrue = !whereInvertTrue;
            }
            addWhere(current.right.toString());
        }
        return this;
    }

    public SqlBuilder addOrder(final String order) {
        if (!StringUtils.isBlank(order)) {
            orders.add(order);
        }
        return this;
    }

    public String getSql() {
        final StringBuilder sb = new StringBuilder("SELECT " + CollectionStream.of(columns).toJoinedString(", ") + " FROM " + mainTable + " " + getMainTableId());
        for (String tab : tables) {
            sb.append(", ").append(tab);
        }

        if (!joins.isEmpty()) {
            boolean first = true;
            for (String join : joins) {
                sb.append(first ? " ON " : " AND ");
                first = false;
                sb.append(join);
            }
        }
        while (whereStack.size() > 1) {
            closeWhere();
        }
        if (!StringUtils.isBlank(whereStack.peek().right.toString())) {
            sb.append(" WHERE ").append(whereStack.peek().right.toString());
        }
        if (!orders.isEmpty()) {
            sb.append(" ORDER BY (").append(CollectionStream.of(orders).toJoinedString("), (")).append(")");
        }
        return sb.toString();
    }

    public static String escape(final String text) {
        return escape(text, false);
    }

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

    /** creates an SQL LIKE expression. E.g. given escapedPattern "test%" it will return "like 'test%'".
     *  Backslash ('\') can be used to escape % and _. E.g. "test\%%" will result in "like 'test\%%' escape '\'
     */
    public static String createLikeExpression(final String escapedPattern) {
        if (escapedPattern == null) {
            return "IS NULL";
        }
        return " LIKE '" + escapedPattern + "' ESCAPE '\\'";
    }


    @NonNull
    @Override
    public String toString() {
        return getSql();
    }
}
