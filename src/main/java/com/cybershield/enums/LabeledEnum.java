package com.cybershield.enums;

/**
 * Gives every enum a human-readable label for the UI, e.g. PENDING_VERIFICATION -> "Pending Verification".
 * Thymeleaf templates use it as ${task.status.label}.
 */
public interface LabeledEnum {

    String name();

    default String getLabel() {
        String[] words = name().toLowerCase().split("_");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return label.toString();
    }

    /** CSS-friendly key used for coloured badges, e.g. "pending-verification". */
    default String getCssKey() {
        return name().toLowerCase().replace('_', '-');
    }
}
