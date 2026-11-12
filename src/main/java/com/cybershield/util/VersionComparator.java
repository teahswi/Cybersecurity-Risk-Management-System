package com.cybershield.util;

/**
 * Compares dotted version strings numerically, e.g. "8.9" < "9.6" and "2.4.52" < "2.4.62".
 * Non-numeric parts are compared as text. Used to detect outdated software installations.
 */
public final class VersionComparator {

    private VersionComparator() {
    }

    public static int compare(String a, String b) {
        String[] left = a.trim().split("[.\\-_]");
        String[] right = b.trim().split("[.\\-_]");
        int length = Math.max(left.length, right.length);
        for (int i = 0; i < length; i++) {
            String l = i < left.length ? left[i] : "0";
            String r = i < right.length ? right[i] : "0";
            int result;
            if (l.matches("\\d+") && r.matches("\\d+")) {
                result = Long.compare(Long.parseLong(l), Long.parseLong(r));
            } else {
                result = l.compareToIgnoreCase(r);
            }
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    /** True when an installed version is known to be lower than the approved baseline version. */
    public static boolean isOutdated(String installedVersion, String baselineVersion) {
        if (installedVersion == null || baselineVersion == null
                || installedVersion.isBlank() || baselineVersion.isBlank()) {
            return false;
        }
        return compare(installedVersion, baselineVersion) < 0;
    }
}
