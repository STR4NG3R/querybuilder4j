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

import io.github.str4ng3r.exceptions.InvalidSqlGenerationException;
import io.github.str4ng3r.common.Tables.ACTIONSQL;

import java.sql.Timestamp;
import java.util.LinkedHashMap;

/**
 *
 * @author Pablo Eduardo Martinez Solis
 */
public class Delete extends QueryBuilder<Delete> {

  private boolean hardDelete = true;
  private String deletedAtColumn;

  public Delete() {
    super();
    super.setReferenceObject(this);
    initialize();
  }

  private void initialize() {
    this.tables = new Tables(ACTIONSQL.DELETE);
    this.columnValue = new LinkedHashMap<>();
  }

  /**
   * This should init from
   *
   * @param tableNames
   *
   * @return same object as pipe
   */
  public Delete from(String... tableNames) {
    this.tables.from(tableNames);
    return this;
  }

  /**
   * Returns the real name (alias stripped) of the delete's base table, so
   * callers can resolve per-entity metadata without parsing the SQL string.
   */
  public String getBaseTableName() {
    return this.tables.baseTableName();
  }

  /**
   * Sets the column used for soft deletes. When present and hardDelete is false,
   * the DELETE is rewritten as an UPDATE that stamps this column with the current time.
   */
  public Delete setDeletedAtColumn(String deletedAtColumn) {
    this.deletedAtColumn = deletedAtColumn;
    return this;
  }

  /**
   * When true (default) a physical DELETE is generated. When false and a
   * deletedAtColumn is set, a soft-delete UPDATE is generated instead.
   */
  public Delete setHardDelete(boolean hardDelete) {
    this.hardDelete = hardDelete;
    return this;
  }

  @Override
  protected String write() throws InvalidSqlGenerationException {
    if (this.where.listFilterCriteria.isEmpty())
      throw new InvalidSqlGenerationException("It's dangerous to create a delete without where");

    boolean softDelete = !this.hardDelete && this.deletedAtColumn != null;
    if (softDelete) {
      if (this.tables.getTables().size() > 1)
        throw new InvalidSqlGenerationException("Soft delete can only target a single table");
      // Rewrite as UPDATE table SET deletedAt = ? WHERE ...
      // The timestamp is bound as a parameter (prepended in getSqlAndParameters).
      this.tables.setAction(ACTIONSQL.UPDATE);
      this.columnValue.put(this.deletedAtColumn, new Timestamp(System.currentTimeMillis()));
      this.tables.addFields(this.deletedAtColumn + " = ?");
    }

    StringBuilder sql = this.tables.write();
    this.where.write(sql);

    return sql.toString();
  }

  boolean isSoftDelete() {
    return !this.hardDelete && this.deletedAtColumn != null;
  }

}
