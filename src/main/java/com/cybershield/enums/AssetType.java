package com.cybershield.enums;

/**
 * Kind of IT asset.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum AssetType implements LabeledEnum {
    SERVER, WORKSTATION, LAPTOP, NETWORK_DEVICE, DATABASE, CLOUD_INSTANCE, APPLICATION, MOBILE_DEVICE
}
