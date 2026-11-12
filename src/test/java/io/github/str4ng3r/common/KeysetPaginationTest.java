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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import io.github.str4ng3r.exceptions.InvalidSqlGenerationException;

/**
 * Keyset (cursor) pagination: no OFFSET, predicate built from the last-seen
 * values of the ORDER BY columns, limit rendered per dialect.
 */
public class KeysetPaginationTest {

    private Selector base(Constants.SqlDialect d) {
        return new Selector().select("usuarios", "id", "nombre").setDialect(d);
    }

    @Test
    public void primeraPaginaSoloAplicaLimite() throws InvalidSqlGenerationException {
        SqlParameter sp = base(Constants.SqlDialect.Postgres)
                .orderBy("id", false).keyset(11, null).getSqlAndParameters();
        assertEquals("SELECT id, nombre FROM usuarios ORDER BY id ASC LIMIT 11", sp.getSql());
        assertEquals(0, sp.getListParameters().size());
    }

    @Test
    public void unaColumnaAscendente() throws InvalidSqlGenerationException {
        SqlParameter sp = base(Constants.SqlDialect.Postgres)
                .orderBy("id", false).keyset(11, Arrays.<Object>asList(42)).getSqlAndParameters();
        assertEquals("SELECT id, nombre FROM usuarios WHERE ((id > ?)) ORDER BY id ASC LIMIT 11", sp.getSql());
        assertEquals(Arrays.<Object>asList(42), sp.getListParameters());
    }

    @Test
    public void dosColumnasDescendentesExpandePredicado() throws InvalidSqlGenerationException {
        SqlParameter sp = base(Constants.SqlDialect.Postgres)
                .orderBy("createdAt", true).orderBy("id", true)
                .keyset(6, Arrays.<Object>asList("2023-01-01", 7)).getSqlAndParameters();
        assertEquals("SELECT id, nombre FROM usuarios WHERE ((createdAt < ?) OR (createdAt = ? AND id < ?))"
                + " ORDER BY createdAt DESC, id DESC LIMIT 6", sp.getSql());
        // each placeholder is bound positionally, repeated values included
        assertEquals(Arrays.<Object>asList("2023-01-01", "2023-01-01", 7), sp.getListParameters());
    }

    @Test
    public void direccionesMixtas() throws InvalidSqlGenerationException {
        SqlParameter sp = base(Constants.SqlDialect.Mysql)
                .orderBy("role", false).orderBy("id", true)
                .keyset(5, Arrays.<Object>asList("user", 3)).getSqlAndParameters();
        assertEquals("SELECT id, nombre FROM usuarios WHERE ((role > ?) OR (role = ? AND id < ?))"
                + " ORDER BY role ASC, id DESC LIMIT 5", sp.getSql());
        assertEquals(Arrays.<Object>asList("user", "user", 3), sp.getListParameters());
    }

    @Test
    public void seCombinaConWhereExistenteYOrdenaLosParametros() throws InvalidSqlGenerationException {
        SqlParameter sp = base(Constants.SqlDialect.Postgres)
                .where("rol = :rol", p -> p.put("rol", "admin"))
                .orderBy("id", false).keyset(3, Arrays.<Object>asList(10)).getSqlAndParameters();
        assertEquals("SELECT id, nombre FROM usuarios WHERE rol = ? AND ((id > ?)) ORDER BY id ASC LIMIT 3",
                sp.getSql());
        assertEquals(Arrays.<Object>asList("admin", 10), sp.getListParameters());
    }

    @Test
    public void oracleUsaFetchFirst() throws InvalidSqlGenerationException {
        SqlParameter sp = base(Constants.SqlDialect.Oracle)
                .orderBy("id", false).keyset(11, Arrays.<Object>asList(1)).getSqlAndParameters();
        assertEquals("SELECT id, nombre FROM usuarios WHERE ((id > ?)) ORDER BY id ASC FETCH FIRST 11 ROWS ONLY",
                sp.getSql());
    }

    @Test
    public void generarDosVecesNoDuplicaElPredicado() throws InvalidSqlGenerationException {
        Selector s = base(Constants.SqlDialect.Postgres)
                .orderBy("id", false).keyset(11, Arrays.<Object>asList(42));
        assertEquals(s.getSqlAndParameters().getSql(), s.getSqlAndParameters().getSql());
    }

    @Test
    public void clearKeysetRestauraLaQueryOriginal() throws InvalidSqlGenerationException {
        Selector s = base(Constants.SqlDialect.Postgres).orderBy("id", false);
        String original = s.getSqlAndParameters().getSql();
        s.keyset(11, Arrays.<Object>asList(42));
        s.clearKeyset();
        SqlParameter sp = s.getSqlAndParameters();
        assertEquals(original, sp.getSql());
        assertEquals(0, sp.getListParameters().size());
    }

    @Test
    public void exponeColumnasDeOrden() {
        Selector s = base(Constants.SqlDialect.Sql).orderBy("a", false).orderBy("b", true);
        assertEquals(Arrays.asList("a", "b"), s.getOrderColumns());
    }

    @Test(expected = InvalidSqlGenerationException.class)
    public void sinOrderByLanzaExcepcion() throws InvalidSqlGenerationException {
        base(Constants.SqlDialect.Postgres).keyset(10, null).getSqlAndParameters();
    }

    @Test(expected = InvalidSqlGenerationException.class)
    public void cantidadDeValoresDistintaALaDeColumnas() throws InvalidSqlGenerationException {
        base(Constants.SqlDialect.Postgres).orderBy("a", false).orderBy("id", false)
                .keyset(10, Arrays.<Object>asList(1)).getSqlAndParameters();
    }

    @Test(expected = InvalidSqlGenerationException.class)
    public void valorNuloEnElCursorLanzaExcepcion() throws InvalidSqlGenerationException {
        base(Constants.SqlDialect.Postgres).orderBy("id", false)
                .keyset(10, Arrays.<Object>asList((Object) null)).getSqlAndParameters();
    }

    @Test(expected = IllegalArgumentException.class)
    public void limiteInvalido() {
        base(Constants.SqlDialect.Postgres).keyset(0, null);
    }

    @Test
    public void orderByConEnumEquivaleAlBooleano() throws InvalidSqlGenerationException {
        String conBoolean = base(Constants.SqlDialect.Postgres)
                .orderBy("createdAt", true).orderBy("id", false)
                .keyset(6, Arrays.<Object>asList("2023-01-01", 7)).getSqlAndParameters().getSql();
        String conEnum = base(Constants.SqlDialect.Postgres)
                .orderBy("createdAt", Order.DESC).orderBy("id", Order.ASC)
                .keyset(6, Arrays.<Object>asList("2023-01-01", 7)).getSqlAndParameters().getSql();
        assertEquals(conBoolean, conEnum);
        assertEquals("SELECT id, nombre FROM usuarios WHERE ((createdAt < ?) OR (createdAt = ? AND id > ?))"
                + " ORDER BY createdAt DESC, id ASC LIMIT 6", conEnum);
    }

    @Test(expected = IllegalArgumentException.class)
    public void orderByConEnumNuloLanzaExcepcion() {
        base(Constants.SqlDialect.Postgres).orderBy("id", (Order) null);
    }
}
