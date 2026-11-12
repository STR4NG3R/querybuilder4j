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

import io.github.str4ng3r.exceptions.InvalidCurrentPageException;
import io.github.str4ng3r.exceptions.InvalidSqlGenerationException;
import io.github.str4ng3r.common.Tables.ACTIONSQL;

import java.util.HashMap;
import java.util.function.Consumer;

/**
 * A class that handles select statements
 *
 * @author Pablo Eduardo Martinez Solis
 */
public class Selector extends QueryBuilder<Selector> {

    private OrderGroupBy orderBy;
    private OrderGroupBy groupBy;
    private WhereHaving having;
    private boolean withDeleted = true;
    // Keyset (cursor) pagination state. keysetLimit == null means disabled.
    private Integer keysetLimit;
    private java.util.List<Object> keysetAfter;
    // table name -> soft-delete column. Only listed tables get the IS NULL filter.
    private java.util.Map<String, String> softDeleteColumns;

    public Selector() {
        super();
        super.setReferenceObject(this);
        initialize();
    }

    private void initialize() {
        this.tables = new Tables(ACTIONSQL.SELECT);
        this.orderBy = null;
        this.groupBy = null;
        this.having = null;
    }

    /**
     * When false, tables listed via setSoftDeleteColumns get a
     * "&lt;column&gt; IS NULL" filter so soft-deleted rows are excluded.
     * Defaults to true (include all).
     *
     * @param withDeleted whether soft-deleted rows should be included
     * @return same object as pipe
     */
    public Selector setWithDeleted(boolean withDeleted) {
        this.withDeleted = withDeleted;
        return this;
    }

    /**
     * Registers, per table name, the soft-delete column to filter on when
     * withDeleted is false. Tables not present in the map are never filtered,
     * so joins to tables without a soft-delete column stay valid.
     *
     * @param softDeleteColumns map of real table name to its soft-delete column
     * @return same object as pipe
     */
    public Selector setSoftDeleteColumns(java.util.Map<String, String> softDeleteColumns) {
        this.softDeleteColumns = softDeleteColumns;
        return this;
    }

    /**
     * Real names (alias stripped) of every table referenced by this selector,
     * base and joined. Useful to resolve per-table metadata (e.g. soft-delete
     * columns) without parsing the generated SQL.
     *
     * @return list of table names in declaration order
     */
    public java.util.List<String> getTableNames() {
        return this.tables.getTableNames();
    }

    /**
     * Initialize select and put the first table to join
     *
     * @param tableName
     * @param fields
     * @return same object as pipe
     */
    public Selector select(String tableName, String... fields) {
        this.tables.addTable(tableName, fields);
        return this;
    }

    /**
     * Add more fields to satement
     *
     * @param fields An list to fields to fetch
     * @return same object as pipe
     */
    public Selector addSelect(String... fields) {
        this.tables.addFields(fields);
        return this;
    }

    /**
     * Add order by to statement
     *
     * @param orderBy
     * @param descending
     * @return same object as pipe
     */
    public Selector orderBy(String orderBy, boolean descending) {
        if (this.orderBy == null)
            this.orderBy = new OrderGroupBy();
        this.orderBy.orderBy(orderBy, descending);
        return this;
    }

    /**
     * Add order by to statement using a readable direction.
     *
     * <pre>{@code
     * selector.orderBy("createdAt", Order.DESC).orderBy("id", Order.DESC);
     * }</pre>
     *
     * @param orderBy column (or expression) to sort by
     * @param order   {@link Order#ASC} or {@link Order#DESC}
     * @return same object as pipe
     */
    public Selector orderBy(String orderBy, Order order) {
        if (order == null)
            throw new IllegalArgumentException("Order direction must not be null");
        return orderBy(orderBy, order.isDescending());
    }

    /**
     * Add group by to statement
     * Pass columns to group by separae by ,
     *
     * @param columns
     * @return same object as pipe
     */
    public Selector groupBy(String columns) {
        this.groupBy = new OrderGroupBy();
        this.groupBy.groupBy(columns);
        return this;
    }

    /**
     * This initialize the having (If there's any previous filter criteria should be
     * reset)
     *
     * @param criteria
     * @param parameters
     * @return same object as pipe
     */
    public Selector having(String criteria, Consumer<HashMap<String, Object>> parameters) {
        if (this.having == null)
            this.having = new WhereHaving(" HAVING ", this.parameter);
        this.having.addCriteria(criteria, parameters);
        return this;
    }

    /**
     * Add more filter criteria to previous criteria
     *
     * @param criteria
     * @param parameters
     * @return same object as pipe
     */
    public Selector andHaving(String criteria, Consumer<HashMap<String, Object>> parameters) {
        if (this.having == null)
            this.having = new WhereHaving(" HAVING ", this.parameter);
        this.having.andAddCriteria(criteria, parameters);
        return this;
    }

    /**
     * Columns of the ORDER BY clause, in declaration order. Cursor pagination
     * uses them as the sort key.
     *
     * @return unmodifiable list of order-by columns (empty if none)
     */
    public java.util.List<String> getOrderColumns() {
        return this.orderBy == null ? java.util.Collections.<String>emptyList() : this.orderBy.columns();
    }

    /**
     * Enables keyset (cursor) pagination. Rows strictly after {@code after}
     * (following the ORDER BY of this selector) are returned, limited to
     * {@code limit} rows. No OFFSET is used, so the cost does not grow with the
     * depth of the page.
     * <p>
     * The ORDER BY must be declared (with {@link #orderBy(String, boolean)})
     * and must end with a unique column so the order is total. Each value in
     * {@code after} maps positionally to an ORDER BY column and is always bound
     * as a parameter.
     *
     * @param limit max rows to return (pass pageSize + 1 to detect a next page)
     * @param after last-seen values of the order columns, or null for the first page
     * @return same object as pipe
     */
    public Selector keyset(int limit, java.util.List<Object> after) {
        if (limit < 1)
            throw new IllegalArgumentException("The limit must be greater than 0");
        this.keysetLimit = limit;
        this.keysetAfter = after == null ? null : new java.util.ArrayList<>(after);
        return this;
    }

    /**
     * Disables keyset pagination on this selector, restoring the plain query
     * (no cursor predicate, no row limit).
     *
     * @return same object as pipe
     */
    public Selector clearKeyset() {
        this.keysetLimit = null;
        this.keysetAfter = null;
        return this;
    }

    /**
     * Builds (c1 > :ks0) OR (c1 = :ks0 AND c2 > :ks1) ... honoring the
     * direction of each column. Expanded form instead of row values
     * "(a,b) > (x,y)" because Oracle does not support it and it allows mixed
     * ASC/DESC.
     */
    private String keysetPredicate() throws InvalidSqlGenerationException {
        java.util.List<String> cols = getOrderColumns();
        if (cols.isEmpty())
            throw new InvalidSqlGenerationException("Keyset pagination requires an ORDER BY");
        if (keysetAfter.size() != cols.size())
            throw new InvalidSqlGenerationException("Cursor has " + keysetAfter.size()
                    + " values but ORDER BY has " + cols.size() + " columns");
        for (int i = 0; i < cols.size(); i++) {
            if (keysetAfter.get(i) == null)
                throw new InvalidSqlGenerationException(
                        "Cursor value for '" + cols.get(i) + "' is null; order columns must be NOT NULL");
            this.parameter.addParameter("ks" + i, keysetAfter.get(i));
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0)
                sb.append(" OR ");
            sb.append('(');
            for (int j = 0; j < i; j++)
                sb.append(cols.get(j)).append(" = :ks").append(j).append(" AND ");
            sb.append(cols.get(i)).append(this.orderBy.isDescending(i) ? " < " : " > ").append(":ks").append(i);
            sb.append(')');
        }
        return sb.toString();
    }

    public String getCount(String sql) {
        String newSql = "SELECT COUNT(*) FROM ( " + sql + " )";
        if (constants.getSqlDialect() == Constants.SqlDialect.Mysql.sqlDialect)
            newSql += " AS  temp_count";
        return newSql;
    }

    /**
     * Return a querybuilder with pagination
     *
     * @param sqlParameter
     * @param pagination
     * @return same object as pipe
     * @throws InvalidCurrentPageException
     */
    public Selector setPagination(SqlParameter sqlParameter, Pagination pagination) throws InvalidCurrentPageException {
        pagination.calculatePagination(sqlParameter, constants, parameter);
        sqlParameter.setPagination(pagination);
        return this;
    }

    @Override
    protected String write() throws InvalidSqlGenerationException {
        // Propagate soft-delete config to Tables so each JOIN to a soft-deletable
        // table gets the "IS NULL" condition inside its ON clause.
        this.tables.setWithDeleted(this.withDeleted);
        this.tables.setSoftDeleteColumns(this.softDeleteColumns);

        // Base table: exclude soft-deleted rows in the WHERE, only if that table
        // is registered with a soft-delete column. Correct predicate is "IS NULL".
        if (!this.withDeleted && this.softDeleteColumns != null) {
            String baseTable = this.tables.baseTableName();
            String col = baseTable == null ? null : this.softDeleteColumns.get(baseTable);
            if (col != null) {
                String baseAlias = this.tables.baseTableAlias();
                String qualified = baseAlias == null ? col : baseAlias + "." + col;
                this.andWhere(qualified + " IS NULL", p -> {});
            }
        }

        StringBuilder sql = this.tables.write();

        this.where.write(sql);

        if (this.keysetLimit != null && this.keysetAfter != null) {
            sql.append(this.where.getListFilterCriteria().isEmpty() ? " WHERE (" : " AND (")
                    .append(keysetPredicate()).append(')');
        }

        if (this.groupBy != null)
            sql.append(" GROUP BY ").append(this.groupBy.write());

        if (this.having != null)
            this.having.write(sql);

        if (this.orderBy != null)
            sql.append(" ORDER BY ").append(this.orderBy.write());

        if (this.keysetLimit != null) {
            if (this.orderBy == null)
                throw new InvalidSqlGenerationException("Keyset pagination requires an ORDER BY");
            sql.append(constants.replaceValues(constants.getAction(Constants.Actions.LIMIT),
                    Integer.toString(this.keysetLimit)));
        }

        return sql.toString();
    }

}
