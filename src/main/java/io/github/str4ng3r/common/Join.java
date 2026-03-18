package io.github.str4ng3r.common;

public enum Join {
    INNER(" INNER JOIN "), LEFT(" LEFT JOIN "), RIGHT(" RIGHT JOIN "), CROSS(" CROSS JOIN ");

    public String joinOpt;

    private Join(String joinOpt) {
        this.joinOpt = joinOpt;
    }
}
