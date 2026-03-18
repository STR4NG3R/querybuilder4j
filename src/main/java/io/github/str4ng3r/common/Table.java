//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.github.str4ng3r.common;

public class Table {
    public String database;
    public String schema;
    public String name;
    public String on;
    public String join;
    public String deletedAtColumn;

    public Table(String name) {
        this.name = name;
    }

    public Table(String join, String name, String on) {
        this(name);
        this.join = join;
        this.on = on;
    }
}
