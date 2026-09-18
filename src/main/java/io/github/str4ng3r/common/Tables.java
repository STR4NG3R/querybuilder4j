/*
 * The GPLv3 License (GPLv3)
 *
 * Copyright (c) 2023 Pablo Eduardo Martinez Solis
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.str4ng3r.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.github.str4ng3r.exceptions.InvalidSqlGenerationException;

/**
 * @author Pablo Eduardo Martinez Solis
 */
class Tables {
    private List<String> fields;
    private List<Table> tables;
    private ACTIONSQL action;
    private boolean withDeleted = true;
    // Maps a real table name to its soft-delete column. Only tables present here
    // receive the "deletedAt IS NULL" filter; tables without a soft-delete column
    // (e.g. join tables) are left untouched.
    private Map<String, String> softDeleteColumns;

    public static enum ACTIONSQL {
        DELETE("DELETE "), UPDATE("UPDATE "), SELECT("SELECT ");

        public String action;

        ACTIONSQL(String action) {
            this.action = action;
        }
    }

    public void setAction(ACTIONSQL action) {
        this.action = action;
    }

    public void setWithDeleted(boolean withDeleted) {
        this.withDeleted = withDeleted;
    }

    public boolean isWithDeleted() {
        return withDeleted;
    }

    public void setSoftDeleteColumns(Map<String, String> softDeleteColumns) {
        this.softDeleteColumns = softDeleteColumns;
    }

    public Tables(ACTIONSQL action) {
        this.action = action;
        this.fields = new ArrayList<>();
        this.tables = new ArrayList<>();
    }

    public void addFields(String... fields) {
        this.fields.addAll(Arrays.asList(fields));
    }

    public void from(String... tableNames) {
        if (tableNames == null || tableNames.length == 0)
            throw new IllegalArgumentException("At least one table name is required");
        for (String t : tableNames) {
            if (t == null || t.trim().isEmpty())
                throw new IllegalArgumentException("Table name must not be null or empty");
            this.tables.add(new Table(t));
        }
    }

    public void addTable(String tableName, String... fields) {
        if (tableName == null || tableName.trim().isEmpty())
            throw new IllegalArgumentException("Table name must not be null or empty");
        this.fields.clear();
        this.tables.add(new Table(tableName));
        this.addFields(fields);
    }

    public void addJoin(Join join, String name, String on) {
        if (join == null)
            throw new IllegalArgumentException("Join type must not be null");
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Join table name must not be null or empty");
        this.tables.add(new Table(join.joinOpt, name, on));
    }

    private void addSeparator(List<String> list, StringBuilder sql) {
        sql.append(String.join(", ", list).concat(" "));
    }

    public StringBuilder write() throws InvalidSqlGenerationException {
        StringBuilder sql = new StringBuilder();
        if (getTables().isEmpty())
            throw new InvalidSqlGenerationException("Tables array is empty, so it could not generate the query");

        // Las tablas base son las que no provienen de un join (join == null); se
        // unen con coma en el FROM. Las tablas de join se emiten aparte con su
        // propia cláusula (INNER/LEFT/... JOIN ... ON ...).
        List<String> baseTables = new ArrayList<>();
        for (Table t : tables)
            if (t.join == null)
                baseTables.add(t.name);

        if (baseTables.isEmpty())
            throw new InvalidSqlGenerationException("No base table was provided for the query");

        String baseFrom = String.join(", ", baseTables);

        sql.append(this.action.action);

        if (this.action == ACTIONSQL.SELECT) {
            if (fields.isEmpty()) sql.append("* ");
            else addSeparator(fields, sql);
            sql.append("FROM ");
            sql.append(baseFrom);
        } else if (this.action == ACTIONSQL.DELETE) {
            sql.append("FROM ");
            sql.append(baseFrom);
        } else if (this.action == ACTIONSQL.UPDATE) {
            sql.append(baseFrom);
            sql.append(" SET ");
            addSeparator(fields, sql);
        }

        for (Table table : tables) {
            if (table.join == null)
                continue; // ya incluida en el FROM
            sql.append(table.join).append(table.name);
            // CROSS JOIN (o cualquier join sin condición) no debe generar 'ON' colgante
            if (table.on != null && !table.on.trim().isEmpty()) {
                sql.append(" ON ").append(table.on);
                // Excluir filas soft-deleted SOLO si esta tabla tiene columna de
                // soft-delete registrada. El filtro va dentro del ON para no
                // convertir un LEFT/RIGHT JOIN en un INNER.
                String col = softDeleteColumnFor(table);
                if (col != null) {
                    sql.append(" AND ")
                       .append(getAliasTable(table))
                       .append(".").append(col).append(" IS NULL");
                }
            }
        }

        return sql;
    }

    /**
     * Returns the soft-delete column for a table if filtering is enabled and the
     * table's real name is registered as having one; otherwise null.
     */
    private String softDeleteColumnFor(Table t) {
        if (withDeleted || softDeleteColumns == null || softDeleteColumns.isEmpty())
            return null;
        return softDeleteColumns.get(getTableName(t));
    }

    /**
     * Returns the real table name of an expression, dropping any alias:
     * "userAddress as ua" -> "userAddress", "users u" -> "users", "users" -> "users".
     */
    static String getTableName(Table t) {
        String[] words = t.name.trim().split("\\s+");
        return words.length > 0 ? words[0] : t.name;
    }

    /**
     * Returns the alias of a table expression ("users u" -> "u", "users" -> "users").
     */
    static String getAliasTable(Table t) {
        String[] words = t.name.trim().split("\\s+");
        return words.length > 0 ? words[words.length - 1] : t.name;
    }

    /**
     * Alias of the first base (non-join) table, or null if none exists.
     */
    public String baseTableAlias() {
        for (Table t : tables) {
            if (t.join == null) {
                return getAliasTable(t);
            }
        }
        return null;
    }

    /**
     * Real table name of the first base (non-join) table, or null if none exists.
     */
    public String baseTableName() {
        for (Table t : tables) {
            if (t.join == null) {
                return getTableName(t);
            }
        }
        return null;
    }

    /**
     * Real names (alias stripped) of every table referenced by the query,
     * including base and joined tables. Lets callers resolve per-table metadata
     * without inspecting the generated SQL.
     */
    public java.util.List<String> getTableNames() {
        java.util.List<String> names = new ArrayList<>();
        for (Table t : tables) names.add(getTableName(t));
        return names;
    }

    private class Table {
        private String name;
        private String on;
        private String join;

        public Table(String name) {
            this.name = name;
        }

        public Table(String join, String name, String on) {
            this(name);
            this.join = join;
            this.on = on;
        }
    }

    public List<String> getFields() {
        return fields;
    }

    public List<Table> getTables() {
        return tables;
    }
}