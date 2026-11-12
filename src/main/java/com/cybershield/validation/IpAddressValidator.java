package com.cybershield.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/** Validates IPv4 dotted-quad addresses and IPv6 literals without any DNS lookup. */
public class IpAddressValidator implements ConstraintValidator<ValidIpAddress, String> {

    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
    private static final Pattern IPV6_CHARS = Pattern.compile("^[0-9A-Fa-f:.]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return isValidAddress(value);
    }

    public static boolean isValidAddress(String value) {
        if (value == null || value.isBlank()) {
            return true; // optional field
        }
        String v = value.trim();
        if (IPV4.matcher(v).matches()) {
            return true;
        }
        if (!v.contains(":") || !IPV6_CHARS.matcher(v).matches()) {
            return false;
        }
        return isValidIpv6(v);
    }

    private static boolean isValidIpv6(String v) {
        int doubleColon = v.indexOf("::");
        if (doubleColon != v.lastIndexOf("::")) {
            return false; // "::" may appear only once
        }
        String[] groups = v.split(":", -1);
        int count = 0;
        for (int i = 0; i < groups.length; i++) {
            String g = groups[i];
            if (g.isEmpty()) {
                continue;
            }
            if (g.contains(".")) {
                // embedded IPv4 must be the last group, e.g. ::ffff:10.0.0.1
                if (i != groups.length - 1 || !IPV4.matcher(g).matches()) {
                    return false;
                }
                count += 2;
                continue;
            }
            if (g.length() > 4) {
                return false;
            }
            count++;
        }
        return doubleColon >= 0 ? count < 8 : count == 8;
    }
}
