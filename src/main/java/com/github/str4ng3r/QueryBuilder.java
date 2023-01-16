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

import java.util.List;

/**
 *
 * @author Pablo Eduardo Martinez Solis
 */
abstract class QueryBuilder {
  protected Constants constants = new Constants();
  protected Parameter parameter = new Parameter();

  QueryBuilder() {
  }

  public Parameter addParameter(String column, String value) {
    return this.parameter.addParameter(column, value);
  }

  public Parameter addParameter(String column, String value, boolean b) {
    return this.parameter.addParameter(column, value, b);
  }

  abstract protected String write();

  /**
   * Generate SQL Statement with paramaters
   *
   * @return SqlParameter
   */
  public SqlParameter getSqlAndParameters() {
    String sql = this.write();

    List<String> orderParameters = parameter.sortParameters(parameter.getIndexesOfOcurrences(sql));
    sql = parameter.replaceParamatersOnSql(sql);

    return new SqlParameter(sql, orderParameters);
  }
}