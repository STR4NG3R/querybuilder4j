//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.github.str4ng3r.common;

import io.github.str4ng3r.common.Tables.ACTIONSQL;
import io.github.str4ng3r.exceptions.InvalidSqlGenerationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

abstract class QueryBuilder<T> {
    protected Constants constants = new Constants();
    protected Parameter parameter = new Parameter();
    protected WhereHaving where;
    protected LinkedHashMap<String, Object> columnValue;
    protected Tables tables;
    protected T t;

    QueryBuilder() {
        this.where = new WhereHaving(" WHERE ", this.parameter);
    }

    public List<Table> getTables() {
        return this.tables.getTables();
    }

    protected void setReferenceObject(T t) {
        this.t = t;
    }

    public Parameter addParameter(String column, Object value) {
        return this.parameter.addParameter(column, value);
    }

    protected abstract String write() throws InvalidSqlGenerationException;

    /**
     * Generate SQL Statement with paramaters as an array
     *
     * @return SqlParameter
     * @throws InvalidSqlGenerationException
     */
    public SqlParameter getSqlAndParameters() throws InvalidSqlGenerationException {
        String sql = this.write();

        List<Object> orderParameters = parameter.sortParameters(parameter.getIndexesOfOcurrences(sql));
        sql = parameter.replaceParamatersOnSql(sql);

        return new SqlParameter(sql, orderParameters);
    }

    public SqlParameter getSqlAndParametersDictionarie() throws InvalidSqlGenerationException {
        String sql = this.write();
        return new SqlParameter(sql, this.parameter.parameters);
    }

    public T where(String criteria, Consumer<HashMap<String, Object>> parameters) {
        this.where.addCriteria(criteria, parameters);
        return this.t;
    }

    public T where(String criteria) {
        this.where.addCriteria(criteria, null);
        return this.t;
    }


    public T andWhere(String criteria, Consumer<HashMap<String, Object>> parameters) {
        this.where.andAddCriteria(criteria, parameters);
        return this.t;
    }

    public T andWhere(String criteria) {
        this.where.andAddCriteria(criteria, null);
        return this.t;
    }

    public T setDialect(Constants.SqlDialect sqlDialect) {
        this.constants.setDialect(sqlDialect);
        return this.t;
    }

    public T join(Join join, String tableName, String on) {
        this.tables.addJoin(join, tableName, on);
        return this.t;
    }


    public T addFrom(String tableName) {
        this.tables.from(new String[]{tableName});
        return this.t;
    }
}
