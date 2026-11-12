package com.cybershield.util;

import com.cybershield.enums.RecordType;

/** Builds the detail-page URL for a record referenced by a notification, audit log or search result. */
public final class RecordLinks {

    private RecordLinks() {
    }

    public static String urlFor(RecordType type, Long id) {
        if (type == null || id == null) {
            return null;
        }
        return switch (type) {
            case ASSET -> "/assets/" + id;
            case SOFTWARE -> "/software/" + id;
            case VULNERABILITY -> "/vulnerabilities/" + id;
            case ASSET_VULNERABILITY -> "/asset-vulnerabilities/" + id;
            case REMEDIATION_TASK -> "/remediation/" + id;
            case ALERT -> "/alerts/" + id;
            case INCIDENT -> "/incidents/" + id;
            case AUDIT_PLAN -> "/audits/" + id;
            case AUDIT_FINDING -> "/audits/findings/" + id;
            case CORRECTIVE_ACTION -> "/audits/actions/" + id;
            case RISK -> "/risks/" + id;
            case USER -> "/users/" + id;
            default -> null;
        };
    }
}
