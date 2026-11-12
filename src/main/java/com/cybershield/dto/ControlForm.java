package com.cybershield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ControlForm {

    @NotNull(message = "Choose a framework")
    private Long frameworkId;

    @NotBlank(message = "Control code is required")
    @Size(max = 30)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9.\\-]*$", message = "Use letters, digits, dots and dashes, e.g. A.8.8")
    private String controlCode;

    @NotBlank(message = "Control title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 4000)
    private String description;

    @Size(max = 100)
    private String category;

    public Long getFrameworkId() { return frameworkId; }
    public void setFrameworkId(Long frameworkId) { this.frameworkId = frameworkId; }
    public String getControlCode() { return controlCode; }
    public void setControlCode(String controlCode) { this.controlCode = controlCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
