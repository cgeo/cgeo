package cgeo.geocaching.storage;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Func1;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public final class DataStoreUtils {

    private DataStoreUtils() {
        //No instance of this class
    }

    /** Enum for DB types used */
    public enum DBType {
        INTEGER, TEXT, FLOAT, DOUBLE, LONG
    }

    /**
     * Represents a single DB table. Meant for instantiating as a constant. Provides helper methods
     * when working with tables
     */
    public static class DBTable {
        @NonNull
        public final String name;
        public final List<DBColumn> columns = new ArrayList<>();
        private final Map<String, DBColumn> columnsByName = new HashMap<>();
        //lazy initialized
        private DBColumn[] columnsAsArray;

        public DBColumn[] getColumnsAsArray() {
            ensureLazyInitialized();
            return columnsAsArray;
        }

        private void ensureLazyInitialized() {
            if (columnsAsArray == null || columnsAsArray.length != columns.size()) {
                columnsAsArray = columns.toArray(new DBColumn[columns.size()]);
            }
        }

        public DBColumn getColumnByName(final String name) {
            return columnsByName.get(name);
        }

        DBTable(@NonNull final String name) {
            this.name = name;
        }

        /** meant for usage when creating constants */
        public DBColumn add(final String name, final DBType type) {
            return add(name, type, null);
        }

        /** meant for usage when creating constants */
        public DBColumn add(final String name, final DBType type, final String addProp) {
            final DBColumn column = new DBColumn(this, name, type, addProp, this.columns.size());
            this.columns.add(column);
            this.columnsByName.put(column.name, column);
            return column;
        }

        // database table manipulation

        /** creates a new column for this table */
        public void createColumn(final SQLiteDatabase db, final DBColumn column, final boolean failIfExists) {
            try {
                db.execSQL(getSqlCreateColumn(column));
                Log.i("[DB] Column '" + name + "'.'" + column.name + "' created");
            } catch (SQLiteException sle) {
                if (failIfExists || !sle.getMessage().contains("duplicate column name")) {
                    throw sle;
                }
                Log.i("[DB] Column '" + name + "'.'" + column.name + "' NOT created because it already exists");
            }
        }

        public String getSqlCreateColumn(final DBColumn column) {
            return "ALTER TABLE " + name + " ADD COLUMN " + column.sqlForField();
        }

        /** creates this table in db (if not exists yet) */
        public void createTable(final SQLiteDatabase db) {
            db.execSQL(getSqlCreateTable());
            Log.i("[DB] Table '" + name + "' created");
        }

        public String getSqlCreateTable() {
            //well, this would be easier if we could us
            // e streams...
            final StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS " + name + " ( ");
            //TODO: replace loop with CollectionStream when this is available in master
            //sb.append(CollectionStream.of(columns).map(c -> c.sqlForField()).toJoinedString(","));
            boolean first = true;
            for (DBColumn c : columns) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(c.sqlForField());
            }
            sb.append(")");
            return sb.toString();
        }

        /** drops this table in db (if it exists) */
        public void dropTable(final SQLiteDatabase db) {
            db.execSQL(getSqlDropTable());
            Log.i("[DB] Table '" + name + "' dropped");
        }

        public String getSqlDropTable() {
            return "DROP TABLE IF EXISTS " + name;
        }

        // database querying

        /** selects rows from this table */
        public <T> List<T> selectRows(final SQLiteDatabase db, final QueryParams query, final Func1<Cursor, T> mapper) {

            Cursor c = null;
            try {
                c = openCursorFor(db, query, null);
                final List<T> result = new ArrayList<>();
                while (c.moveToNext()) {
                    result.add(mapper.call(c));
                }
                return result;

            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        /** selects one row (first one from a given query) from this table, returns null if result is empty */
        public <T> T selectFirstRow(final SQLiteDatabase db, final QueryParams query, final Func1<Cursor, T> mapper) {

            Cursor c = null;
            try {
                c = openCursorFor(db, query, "1");
                if (c.moveToNext()) {
                    return mapper.call(c);
                }
                return null;
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        private Cursor openCursorFor(final SQLiteDatabase db, final QueryParams query, final String limitOverride) {
            return db.query(
                    this.name,
                    toNames(query.columns == null || query.columns.length == 0 ? this.getColumnsAsArray() : query.columns),
                    query.whereClause, query.whereArgs, query.groupBy, query.having, query.orderBy, limitOverride == null ? query.limit : limitOverride
            );
        }

        // database data manipulation

        /** inserts a new row into this table */
        public long insertRow(final SQLiteDatabase db, final ContentValues values) {
            return db.insert(this.name, null, values);
        }

        /** updates existing row(s) in this table */
        public int updateRows(final SQLiteDatabase db, final ContentValues values, final String whereClause, final String[] whereArgs) {
            return db.update(this.name, values, whereClause, whereArgs);
        }

        /** deletes existing row(s) in this table */
        public int deleteRows(final SQLiteDatabase db, final String whereClause) {
            return db.delete(this.name, whereClause, null);
        }

        /** deletes existing row(s) in this table */
        public int deleteRows(final SQLiteDatabase db, final String whereClause, final String[] whereArgs) {
            final int cnt = db.delete(this.name, whereClause, whereArgs);
            Log.d("[DB] Deleted " + cnt + " rows from '" + this.name + "'");
            return cnt;
        }

        private String[] toNames(final DBColumn[] columns) {
            //TODO: replace with COllectionStream when this is available in master
            //return CollectionStream.of(columns).map(c -> c.name).toArray(String.class);
            final String[] columnNames = new String[columns.length];
            for (int i = 0; i < columns.length; i++) {
                columnNames[i] = columns[i].name;
            }
            return columnNames;
        }

        /**
         * must return table name as usable in SQL expressions
         */
        public String toString() {
            return name;
        }
    }

    /**
     * Represents a single DB column of a table. Meant for usage as code constants
     * DColumns should only ever be created in the context of a DBTable
     */
    public static class DBColumn {
        @NonNull
        public final DBTable table;
        @NonNull
        public final String name;
        @NonNull
        public final DBType type;
        /**
         * Index of this column in the context of its table
         */
        public final int index;
        public final String additionalProperties;

        DBColumn(@NonNull final DBTable table, @NonNull final String name, @NonNull final DBType type, final String additionalProperties, final int index) {
            this.table = table;
            this.name = name;
            this.type = type;
            this.index = index;
            this.additionalProperties = additionalProperties;
        }

        public String sqlForField() {
            return name + " " + type + (additionalProperties == null ? "" : " " + additionalProperties);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final DBColumn dbColumn = (DBColumn) o;
            return table.name.equals(dbColumn.table.name) &&
                    name.equals(dbColumn.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        /**
         * must return column name as usable in SQL expressions
         */
        public String toString() {
            return name;
        }
    }

    /** represents and holds parameters for a query going to a single table */
    public static class QueryParams {
        public final DBColumn[] columns;
        public final String whereClause;
        public final String[] whereArgs;
        public final String having;
        public final String groupBy;
        public final String orderBy;
        public final String limit;

        private QueryParams(final Builder builder) {
            this.columns = builder.columns.toArray(new DBColumn[0]);
            this.whereClause = builder.whereClause;
            this.whereArgs = builder.whereArgs;
            this.having = builder.having;
            this.groupBy = builder.groupBy;
            this.orderBy = builder.orderBy;
            this.limit = builder.limit;
        }

        /** returns the 'raw' SQL select statement (for SQLite syntax) */
        public String toString() {
            if (columns.length == 0) {
                return "no columns";
            }

            //TODO: replace with COllectionStream when this is available in master
            //String colNames = CollectionStream.of(columns).map(c -> c.name).toJoinedString(",");
            final StringBuilder colNames = new StringBuilder();
            boolean first = true;
            for (DBColumn c : columns) {
                if (!first) {
                    colNames.append(",");
                }
                first = false;
                colNames.append(c.name);
            }

            return
                "SELECT " + colNames + " FROM " + columns[0].table +
                (StringUtils.isBlank(whereClause) ? "" : " WHERE " + whereClause) +
                (StringUtils.isBlank(groupBy) ? "" : " GROUP BY " + groupBy) +
                (StringUtils.isBlank(having) ? "" : " HAVING " + having) +
                (StringUtils.isBlank(orderBy) ? "" : " ORDER BY " + orderBy) +
                (StringUtils.isBlank(limit) ? "" : " LIMIT " + limit);
        }

        public static class Builder {
            private List<DBColumn> columns = new ArrayList<>();
            private String whereClause;
            private String[] whereArgs;
            private String having;
            private String groupBy;
            private String orderBy;
            private String limit;

            public QueryParams build() {
                return new QueryParams(this);
            }

            public Builder addColumn(final DBColumn column) {
                this.columns.add(column);
                return this;
            }

            public Builder setWhereClause(final String whereClause) {
                this.whereClause = whereClause;
                return this;
            }

            public Builder setWhereArgs(final String[] whereArgs) {
                this.whereArgs = whereArgs;
                return this;
            }

            public Builder setHaving(final String having) {
                this.having = having;
                return this;
            }

            public Builder setGroupBy(final String groupBy) {
                this.groupBy = groupBy;
                return this;
            }

            public Builder setOrderBy(final String orderBy) {
                this.orderBy = orderBy;
                return this;
            }

            public Builder setLimit(final String limit) {
                this.limit = limit;
                return this;
            }
        }
    }

    /**
     * gives the name of a table
     * TODO: this method exists only because of a codacy rule: "Class cannot be instantiated and does not provide any static methods or fields"
     */
    public static String getTableName(final DBTable table) {
        return table.name;
    }

}
