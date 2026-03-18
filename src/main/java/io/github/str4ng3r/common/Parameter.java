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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Pablo Eduardo Martinez Solis
 */
public final class Parameter {
    public static String p = ":([a-zA-Z0-9\\.]+)";
    public static Pattern pattern = Pattern.compile(p);
    HashMap<String, Object> parameters = new HashMap<>();

    public Parameter addParameter(String column, Object value) {
        this.parameters.put(column, value);
        return this;
    }

    public List<String> getIndexesOfOcurrences(String sql) {
        List<String> indexes = new ArrayList<>();
        Matcher m = Parameter.pattern.matcher(sql);
        while (m.find())
            indexes.add(m.group(1));
        return indexes;
    }

    String replaceParamatersOnSql(String sql) {
        Matcher m = pattern.matcher(sql);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String param = m.group(1);
            System.out.println(param);
            Object value = parameters.get(param);
            System.out.println(value);
            if (value instanceof Collection<?>) {
                System.out.println(value);
                int size = ((Collection<?>) value).size();
                String placeholders =
                        String.join(",", Collections.nCopies(size, "?"));
                m.appendReplacement(sb, placeholders);
            } else {
                m.appendReplacement(sb, "?");
            }
        }

        m.appendTail(sb);
        return sb.toString();
    }

    static String setParameter(String sql, String... parameters) {
        Matcher m = Parameter.pattern.matcher(sql);
        int c = 0, groupPosition = 1;
        StringBuffer sb = new StringBuffer();

        while (m.find() && c < parameters.length) {
            StringBuilder buf = new StringBuilder(m.group());
            buf.replace(m.start(groupPosition) - m.start(), m.end(groupPosition) - m.start(), parameters[c++]);
            m.appendReplacement(sb, buf.toString());
        }

        m.appendTail(sb);
        return sb.toString();
    }

    List<Object> sortParameters(List<String> indexes) {
        List<Object> sorted = new ArrayList<>();

        for (String key : indexes) {

            Object value = parameters.get(key);

            if (value instanceof Collection<?>) {
                sorted.addAll((Collection<?>) value);
            } else {
                sorted.add(value);
            }
        }
        return sorted;
    }

    /**
     * Filter parameters
     *
     * @param parameterToRemove
     */
    void filterParameter(List<String> parameterToRemove) {
        parameterToRemove.forEach(p -> parameters.remove(p));
    }
}
