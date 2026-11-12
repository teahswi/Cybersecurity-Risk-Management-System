package com.cybershield.dto;

import com.cybershield.enums.SoftwareCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form for the software catalogue. */
public class SoftwareForm {

    @NotBlank(message = "Software name is required")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Vendor is required")
    @Size(max = 150)
    private String vendor;

    @NotNull(message = "Choose a category")
    private SoftwareCategory category;

    @Size(max = 50)
    @Pattern(regexp = "^$|^[0-9A-Za-z][0-9A-Za-z._-]*$", message = "Use a version such as 2.4.62")
    private String baselineVersion;

    @Size(max = 2000)
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public SoftwareCategory getCategory() { return category; }
    public void setCategory(SoftwareCategory category) { this.category = category; }
    public String getBaselineVersion() { return baselineVersion; }
    public void setBaselineVersion(String baselineVersion) { this.baselineVersion = baselineVersion; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
