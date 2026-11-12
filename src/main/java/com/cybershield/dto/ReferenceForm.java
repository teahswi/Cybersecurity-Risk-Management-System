package com.cybershield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Shared form for departments and office locations (city/country are used for offices only). */
public class ReferenceForm {

    @NotBlank(message = "Code is required")
    @Size(max = 20)
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Use letters, digits, dashes or underscores")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    @Size(max = 255)
    private String address;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
