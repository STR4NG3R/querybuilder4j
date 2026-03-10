package io.github.str4ng3r.common;

import io.github.str4ng3r.exceptions.InvalidSqlGenerationException;

import java.util.ArrayList;
import java.util.List;

public class Insert {

    StringBuilder columns;
    List<Object> values;

    StringBuilder valuesQuestion;

    String table;

    public Insert() {
        columns = new StringBuilder();
        values = new ArrayList<>();
        valuesQuestion = new StringBuilder();
    }

    public Insert(String table) {
        this();
    }

    public Insert setColumns(String... columns) {
        this.columns.append(String.join(",", columns));
        return this;
    }

    public Insert setValues(Object... values) {
        int lastElement = values.length - 1;
        for (int i = 0; i < lastElement; i++) {
            valuesQuestion.append("?,");
            this.values.add(values[i]);
        }
        valuesQuestion.append("?");
        this.values.add(values[lastElement]);
        return this;
    }


    public Insert setTable(String table) {
        this.table = table;
        return this;
    }

    public String write() throws InvalidSqlGenerationException {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ")
                .append(table)
                .append(" ( ")
                .append(columns)
                .append(" ) ")
                .append(" VALUES (")
                .append(this.valuesQuestion)
                .append(" )");
        return sql.toString();
    }

    public String getSql() throws InvalidSqlGenerationException {
        return write();
    }

}
