//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.github.str4ng3r.common;

import io.github.str4ng3r.exceptions.InvalidSqlGenerationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class Tables {
    private List<String> fields;
    private List<Table> tables;
    private ACTIONSQL action;
    private boolean withDeleted;

    public boolean isWithDeleted() {
        return this.withDeleted;
    }

    public void setWithDeleted(boolean withDeleted) {
        this.withDeleted = withDeleted;
    }

    public ACTIONSQL getAction() {
        return this.action;
    }

    public void setAction(ACTIONSQL action) {
        this.action = action;
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
        for (String table: tableNames) this.tables.add(new Table(table));
    }

    public void addTable(String tableName, String... fields) {
        this.fields.clear();
        this.tables.add(new Table(tableName));
        this.addFields(fields);
    }

    public void addJoin(Join join, String name, String on) {
        this.tables.add(new Table(join.joinOpt, name, on));
    }

    private void addSeparator(List<String> list, StringBuilder sql) {
        sql.append(String.join(", ", list).concat(" "));
    }

    private void addSeparatorTables(List<Table> list, StringBuilder sql) {
        sql.append((String) list.stream().map((f) -> {
            return f.name;
        }).collect(Collectors.joining(", ")));
    }

    public StringBuilder write() throws InvalidSqlGenerationException {
        StringBuilder sql = new StringBuilder();
        if (this.getTables().isEmpty()) {
            throw new InvalidSqlGenerationException("Tables array is empty, so it could not generate the query");
        } else {
            sql.append(this.action.action);
            if (this.action == Tables.ACTIONSQL.SELECT) {
                if (this.fields.isEmpty()) {
                    sql.append("* ");
                } else {
                    this.addSeparator(this.fields, sql);
                }

                sql.append("FROM ");
                sql.append(((Table) this.tables.get(0)).name);
            } else if (this.action == Tables.ACTIONSQL.DELETE) {
                sql.append("FROM ");
                this.addSeparatorTables(this.tables, sql);
            } else if (this.action == Tables.ACTIONSQL.UPDATE) {
                sql.append(((Table) this.tables.get(0)).name);
                sql.append(" SET ");
                this.addSeparator(this.fields, sql);
            }

            for (int i = 1; i < this.tables.size(); ++i) {
                Table table = (Table) this.tables.get(i);
                sql.append(table.join).append(table.name).append(" ON ").append(table.on);
                System.out.println(this.withDeleted);
                if (!this.withDeleted && table.deletedAtColumn != null) {
                    String name = this.getAliasTable(table);
                    sql.append(" AND ").append(name).append(".").append(table.deletedAtColumn).append(" IS NULL");
                }
            }

            return sql;
        }
    }

    public String getAliasTable(Table t) {
        String[] words = t.name.split("\\s+");
        return words.length > 0 ? words[words.length - 1] : "";
    }

    public List<String> getFields() {
        return this.fields;
    }

    public List<Table> getTables() {
        return this.tables;
    }

    public static enum ACTIONSQL {
        DELETE("DELETE "),
        UPDATE("UPDATE "),
        SELECT("SELECT ");

        public String action;

        private ACTIONSQL(String action) {
            this.action = action;
        }
    }
}
