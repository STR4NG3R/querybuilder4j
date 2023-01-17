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
package com.github.str4ng3r;

import com.github.str4ng3r.Constants.SqlDialect;
import com.github.str4ng3r.Join.JOIN;

/**
 *
 * @author Pablo Eduardo Martinez Solis
 */
public class Selector extends QueryBuilder {

  private Tables tables = new Tables();
  private OrderGroupBy orderBy = new OrderGroupBy();
  private OrderGroupBy groupBy = new OrderGroupBy();

  private WhereHaving where;
  private WhereHaving having;

  Selector() {
    super();
    initialize();
  }

  private void initialize() {
    this.where = new WhereHaving(" WHERE ", this.parameter);
    this.having = new WhereHaving(" HAVING ", this.parameter);
    this.orderBy = null;
    this.groupBy = null;
  }

  /**
   * Initialize select and put the first table to join
   *
   * @param tableName
   * @param fields
   *
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
   *
   * @return same object as pipe
   */
  public Selector addSelect(String... fields) {
    this.tables.addFields(fields);
    return this;
  }

  /**
   * Add join to statement
   *
   * @param join      An valid enum of join types
   * @param tableName A source to join could be a query or table
   * @param on        Login to join tables
   *
   * @return same object as pipe
   */
  public Selector join(JOIN join, String tableName, String on) {
    tables.addJoin(join, tableName, on);
    return this;
  }

  /**
   * Add order by to statement
   *
   * @param orderBy
   * @param descending
   *
   * @return same object as pipe
   */
  public Selector orderBy(String orderBy, boolean descending) {
    this.orderBy = new OrderGroupBy();
    this.orderBy.orderBy(orderBy, descending);
    return this;
  }

  /**
   * Add group by to statement
   * Pass columns to group by separae by ,
   *
   * @param columns
   *
   * @return same object as pipe
   */
  public Selector groupBy(String columns) {
    this.groupBy = new OrderGroupBy();
    this.groupBy.groupBy(columns);
    return this;
  }

  /**
   * Set valid SQL Synthax to generate SQL according to different types of
   * databases
   *
   * @param sqlDialect An enum of supported databases
   *
   * @return same object as pipe
   */
  public Selector setDialect(SqlDialect sqlDialect) {
    constants.setDialect(sqlDialect);
    return this;
  }

  /**
   * This initialize the having (If there's any previous filter criteria should be
   * reset)
   *
   * @param criteria
   * @param parameters
   *
   * @return same object as pipe
   */
  public Selector having(String criteria, Parameter... parameters) {
    this.having.addCriteria(criteria, parameters);
    return this;
  }

  /**
   * Add more filter criteria to previous criteria
   *
   * @param criteria
   * @param parameters
   *
   * @return same object as pipe
   */
  public Selector andHaving(String criteria, Parameter... parameters) {
    this.having.andAddCriteria(criteria, parameters);
    return this;
  }

  /**
   * This initialize the where (If there's any previous filter criteria should be
   * reset)
   *
   * @param criteria
   * @param parameters
   *
   * @return same object as pipe
   */
  public Selector where(String criteria, Parameter... parameters) {
    this.where.addCriteria(criteria, parameters);
    return this;
  }

  /**
   * Add more filter criteria to previous criteria
   *
   * @param criteria
   * @param parameters
   *
   * @return same object as pipe
   */
  public Selector andWhere(String criteria, Parameter... parameters) {
    this.where.andAddCriteria(criteria, parameters);
    return this;
  }

  public String getCount(String sql) {
    return "SELECT COUNT(*) FROM ( " + sql + " )";
  }

  /**
   * Return a querybuilder with pagination
   *
   * @param criteria
   * @param parameters
   *
   * @return same object as pipe
   */
  public Selector getPagination(SqlParameter sqlP, Pagination pagination) {
    int lower = pagination.pageSize * pagination.currentPage;
    int upper = lower + pagination.pageSize;
    pagination.totalPages = (int) Math.ceil(pagination.count / pagination.pageSize);
    sqlP.sql += parameter.setParameter(constants.getAction(Constants.Actions.PAGINATION),
        Integer.toString(lower), Integer.toString(upper));
    sqlP.p = pagination;
    return this;
  }

  @Override
  protected String write() {
    StringBuilder sql = this.tables.write();

    this.where.write(sql);

    if (this.groupBy != null)
      sql.append(" GROUP BY ").append(this.groupBy.write());

    this.having.write(sql);

    if (this.orderBy != null)
      sql.append(" ORDER BY ").append(this.orderBy.write());

    return sql.toString();
  }

}
