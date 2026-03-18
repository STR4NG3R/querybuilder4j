//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.github.str4ng3r.common;

import io.github.str4ng3r.common.Constants.SqlDialect;
import io.github.str4ng3r.common.Tables.ACTIONSQL;
import io.github.str4ng3r.exceptions.InvalidCurrentPageException;
import io.github.str4ng3r.exceptions.InvalidSqlGenerationException;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;

public class Selector extends QueryBuilder<Selector> {
    private OrderGroupBy orderBy;
    private OrderGroupBy groupBy;
    private WhereHaving having;

    public Selector() {
        super.setReferenceObject(this);
        this.initialize();
    }

    private void initialize() {
        this.tables = new Tables(ACTIONSQL.SELECT);
        this.tables.setWithDeleted(true);
        this.orderBy = null;
        this.groupBy = null;
        this.having = null;
    }

    public Selector setWithDeleted(boolean withDeleted) {
        this.tables.setWithDeleted(withDeleted);
        return this;
    }

    public Selector select(String tableName, String... fields) {
        this.tables.addTable(tableName, fields);
        return this;
    }

    public Selector addSelect(String... fields) {
        this.tables.addFields(fields);
        return this;
    }

    public Selector orderBy(String orderBy, boolean descending) {
        if (this.orderBy == null) {
            this.orderBy = new OrderGroupBy();
        }

        this.orderBy.orderBy(orderBy, descending);
        return this;
    }

    public Selector groupBy(String columns) {
        this.groupBy = new OrderGroupBy();
        this.groupBy.groupBy(columns);
        return this;
    }

    public Selector having(String criteria, Consumer<HashMap<String, Object>> parameters) {
        if (this.having == null) {
            this.having = new WhereHaving(" HAVING ", this.parameter);
        }

        this.having.addCriteria(criteria, parameters);
        return this;
    }

    public Selector andHaving(String criteria, Consumer<HashMap<String, Object>> parameters) {
        if (this.having == null) {
            this.having = new WhereHaving(" HAVING ", this.parameter);
        }

        this.having.andAddCriteria(criteria, parameters);
        return this;
    }

    public String getCount(String sql) {
        String newSql = "SELECT COUNT(*) FROM ( " + sql + " )";
        if (Objects.equals(this.constants.getSqlDialect(), SqlDialect.Mysql.sqlDialect)) {
            newSql = newSql + " AS  temp_count";
        }

        return newSql;
    }

    public Selector setPagination(SqlParameter sqlParameter, Pagination pagination) throws InvalidCurrentPageException {
        pagination.calculatePagination(sqlParameter, this.constants, this.parameter);
        sqlParameter.p = pagination;
        return this;
    }

    protected String write() throws InvalidSqlGenerationException {
        StringBuilder sql = this.tables.write();
        if (!this.tables.isWithDeleted()) {
            Table t = (Table) this.tables.getTables().get(0);
            String name = this.tables.getAliasTable(t);
            if (t.deletedAtColumn != null) {
                this.andWhere(
                        (name != null ? name + "." : "")
                                .concat( t.deletedAtColumn + " IS NULL"));
            }
        }

        this.where.write(sql);
        if (this.groupBy != null) {
            sql.append(" GROUP BY ").append(this.groupBy.write());
        }

        if (this.having != null) {
            this.having.write(sql);
        }

        if (this.orderBy != null) {
            sql.append(" ORDER BY ").append(this.orderBy.write());
        }

        return sql.toString();
    }
}
