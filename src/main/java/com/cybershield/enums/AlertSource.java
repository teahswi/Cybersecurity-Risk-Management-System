package com.cybershield.enums;

/**
 * Type of system that reported an alert (alerts are stored records; no live SIEM is connected).
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum AlertSource implements LabeledEnum {
    IDS, EDR, FIREWALL, ANTIVIRUS, EMAIL_GATEWAY, CLOUD_MONITOR, DLP, MANUAL
}
