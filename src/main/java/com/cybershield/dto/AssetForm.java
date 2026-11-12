package com.cybershield.dto;

import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.AssetType;
import com.cybershield.enums.Criticality;
import com.cybershield.enums.Environment;
import com.cybershield.validation.ValidIpAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form for creating and editing an asset. */
public class AssetForm {

    @NotBlank(message = "Asset code is required")
    @Size(max = 30, message = "Asset code must be at most 30 characters")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-]*$", message = "Use letters, digits and dashes only, e.g. SRV-104")
    private String assetCode;

    @NotBlank(message = "Asset name is required")
    @Size(max = 150, message = "Asset name must be at most 150 characters")
    private String name;

    @NotNull(message = "Choose an asset type")
    private AssetType assetType;

    @Size(max = 255, message = "Hostname must be at most 255 characters")
    @Pattern(regexp = "^$|^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$", message = "Enter a valid hostname")
    private String hostname;

    @ValidIpAddress
    @Size(max = 45)
    private String ipAddress;

    @Size(max = 100, message = "Operating system must be at most 100 characters")
    private String operatingSystem;

    @NotNull(message = "Choose a department")
    private Long departmentId;

    @NotNull(message = "Choose an office location")
    private Long locationId;

    private Long ownerId;

    @NotNull(message = "Choose a criticality")
    private Criticality criticality;

    @NotNull(message = "Choose an environment")
    private Environment environment;

    @NotNull(message = "Choose a status")
    private AssetStatus status = AssetStatus.ACTIVE;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    public String getAssetCode() { return assetCode; }
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Criticality getCriticality() { return criticality; }
    public void setCriticality(Criticality criticality) { this.criticality = criticality; }
    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }
    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
