package com.cybershield.entity;

import jakarta.persistence.*;

/**
 * Compliance framework (demonstration subsets of ISO 27001 and NIST CSF).
 * Table: compliance_frameworks
 */
@Entity
@Table(name = "compliance_frameworks")
public class ComplianceFramework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "version_label", nullable = true, length = 50)
    private String versionLabel;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;

    public ComplianceFramework() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
