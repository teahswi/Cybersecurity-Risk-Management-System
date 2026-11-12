package com.cybershield.dto;

import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.AssetType;
import com.cybershield.enums.Environment;

/** Search and filter parameters of the asset inventory page (bound from the query string). */
public class AssetFilter {

    private String q;
    private Long departmentId;
    private Long locationId;
    private AssetType type;
    private Environment environment;
    private AssetStatus status;

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public AssetType getType() { return type; }
    public void setType(AssetType type) { this.type = type; }
    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }
    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }
}
