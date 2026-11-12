package com.cybershield.util;

import java.util.UUID;

/**
 * Human-readable business identifiers (e.g. REM-0007). The surrogate BIGINT id stays the relational key;
 * the code is generated from it right after the INSERT, so it is unique and never re-used.
 */
public final class Codes {

    private Codes() {
    }

    /** Temporary unique value used for the NOT NULL code column until the id is known. */
    public static String temporary() {
        return "TMP-" + UUID.randomUUID();
    }

    public static String format(String prefix, Long id) {
        return String.format("%s-%04d", prefix, id);
    }
}
