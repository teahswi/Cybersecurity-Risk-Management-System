package com.cybershield.service;

import com.cybershield.dto.Option;
import com.cybershield.entity.User;
import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.RoleCode;
import com.cybershield.enums.UserStatus;
import com.cybershield.repository.AssetRepository;
import com.cybershield.repository.ComplianceControlRepository;
import com.cybershield.repository.ComplianceFrameworkRepository;
import com.cybershield.repository.DepartmentRepository;
import com.cybershield.repository.OfficeLocationRepository;
import com.cybershield.repository.SoftwareRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.repository.VulnerabilityRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Options for drop-down lists (departments, offices, users by role, assets, ...). */
@Service
@Transactional(readOnly = true)
public class ReferenceDataService {

    private final DepartmentRepository departmentRepository;
    private final OfficeLocationRepository locationRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final SoftwareRepository softwareRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final ComplianceFrameworkRepository frameworkRepository;
    private final ComplianceControlRepository controlRepository;

    public ReferenceDataService(DepartmentRepository departmentRepository, OfficeLocationRepository locationRepository,
                                UserRepository userRepository, AssetRepository assetRepository,
                                SoftwareRepository softwareRepository, VulnerabilityRepository vulnerabilityRepository,
                                ComplianceFrameworkRepository frameworkRepository,
                                ComplianceControlRepository controlRepository) {
        this.departmentRepository = departmentRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.softwareRepository = softwareRepository;
        this.vulnerabilityRepository = vulnerabilityRepository;
        this.frameworkRepository = frameworkRepository;
        this.controlRepository = controlRepository;
    }

    public List<Option> departments() {
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(d -> new Option(d.getId(), d.getName())).toList();
    }

    public List<Option> locations() {
        return locationRepository.findAllByOrderByNameAsc().stream()
                .map(l -> new Option(l.getId(), l.getName() + " (" + l.getCity() + ")")).toList();
    }

    public List<Option> activeUsers() {
        return userRepository.findByStatusWithRole(UserStatus.ACTIVE).stream().map(ReferenceDataService::userOption).toList();
    }

    public List<Option> engineers() {
        return usersWithRoles(List.of(RoleCode.IT_INFRASTRUCTURE_ENGINEER));
    }

    public List<Option> analystsAndManagers() {
        return usersWithRoles(List.of(RoleCode.SECURITY_ANALYST, RoleCode.SECURITY_MANAGER));
    }

    public List<Option> auditors() {
        return usersWithRoles(List.of(RoleCode.COMPLIANCE_AUDITOR));
    }

    public List<Option> activeAssets() {
        return assetRepository.findByStatusNotOrderByAssetCodeAsc(AssetStatus.ARCHIVED).stream()
                .map(a -> new Option(a.getId(), a.getAssetCode() + " - " + a.getName())).toList();
    }

    public List<Option> software() {
        return softwareRepository.findAllByOrderByNameAsc().stream()
                .map(s -> new Option(s.getId(), s.getName() + " (" + s.getVendor() + ")")).toList();
    }

    public List<Option> vulnerabilities() {
        return vulnerabilityRepository.findAllByOrderByVulnCodeAsc().stream()
                .map(v -> new Option(v.getId(), v.getVulnCode() + " - " + v.getTitle())).toList();
    }

    public List<Option> frameworks() {
        return frameworkRepository.findAllByOrderByNameAsc().stream()
                .map(f -> new Option(f.getId(), f.getName())).toList();
    }

    public List<Option> controls(Long frameworkId) {
        return controlRepository.findByFrameworkId(frameworkId).stream()
                .map(c -> new Option(c.getId(), c.getControlCode() + " - " + c.getTitle())).toList();
    }

    private List<Option> usersWithRoles(List<RoleCode> roles) {
        return userRepository.findByRolesAndStatus(roles, UserStatus.ACTIVE).stream()
                .map(ReferenceDataService::userOption).toList();
    }

    private static Option userOption(User u) {
        return new Option(u.getId(), u.getFullName() + " (" + u.getRole().getCode().getLabel() + ")");
    }
}
