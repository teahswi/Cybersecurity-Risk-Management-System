package com.cybershield.service;

import com.cybershield.dto.ReferenceForm;
import com.cybershield.dto.UserViews;
import com.cybershield.entity.Department;
import com.cybershield.entity.OfficeLocation;
import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditModule;
import com.cybershield.enums.RecordType;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.exception.ResourceNotFoundException;
import com.cybershield.repository.DepartmentRepository;
import com.cybershield.repository.OfficeLocationRepository;
import com.cybershield.security.Roles;
import com.cybershield.util.Texts;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Departments and office locations, managed by the Security Manager. */
@Service
@PreAuthorize(Roles.IS_MANAGER)
public class ReferenceAdminService {

    private final DepartmentRepository departmentRepository;
    private final OfficeLocationRepository locationRepository;
    private final AuditLogService auditLogService;

    public ReferenceAdminService(DepartmentRepository departmentRepository, OfficeLocationRepository locationRepository,
                                 AuditLogService auditLogService) {
        this.departmentRepository = departmentRepository;
        this.locationRepository = locationRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<UserViews.DepartmentRow> departments() {
        Map<Long, Long> counts = toMap(departmentRepository.countAssetsPerDepartment());
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(d -> new UserViews.DepartmentRow(d.getId(), d.getCode(), d.getName(), d.getDescription(),
                        counts.getOrDefault(d.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public List<UserViews.LocationRow> locations() {
        Map<Long, Long> counts = toMap(locationRepository.countAssetsPerLocation());
        return locationRepository.findAllByOrderByNameAsc().stream()
                .map(l -> new UserViews.LocationRow(l.getId(), l.getCode(), l.getName(), l.getCity(), l.getCountry(),
                        l.getAddress(), counts.getOrDefault(l.getId(), 0L))).toList();
    }

    @Transactional
    public void saveDepartment(Long id, ReferenceForm form) {
        String code = form.getCode().trim().toUpperCase();
        String name = form.getName().trim();
        Department d;
        if (id == null) {
            if (departmentRepository.existsByCodeIgnoreCase(code) || departmentRepository.existsByNameIgnoreCase(name)) {
                throw new BusinessRuleException("A department with this code or name already exists.");
            }
            d = new Department();
        } else {
            d = departmentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Department", id));
            if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(code, id)
                    || departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw new BusinessRuleException("A department with this code or name already exists.");
            }
        }
        d.setCode(code);
        d.setName(name);
        d.setDescription(Texts.clean(form.getDescription()));
        departmentRepository.save(d);
        auditLogService.log(id == null ? AuditAction.CREATE : AuditAction.UPDATE, AuditModule.SETTINGS,
                RecordType.DEPARTMENT, d.getId(), (id == null ? "Created" : "Updated") + " department " + name + ".");
    }

    @Transactional
    public void saveLocation(Long id, ReferenceForm form) {
        String code = form.getCode().trim().toUpperCase();
        String name = form.getName().trim();
        if (Texts.clean(form.getCity()) == null || Texts.clean(form.getCountry()) == null) {
            throw new BusinessRuleException("City and country are required for an office location.");
        }
        OfficeLocation l;
        if (id == null) {
            if (locationRepository.existsByCodeIgnoreCase(code) || locationRepository.existsByNameIgnoreCase(name)) {
                throw new BusinessRuleException("An office with this code or name already exists.");
            }
            l = new OfficeLocation();
        } else {
            l = locationRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Office location", id));
            if (locationRepository.existsByCodeIgnoreCaseAndIdNot(code, id)
                    || locationRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw new BusinessRuleException("An office with this code or name already exists.");
            }
        }
        l.setCode(code);
        l.setName(name);
        l.setCity(form.getCity().trim());
        l.setCountry(form.getCountry().trim());
        l.setAddress(Texts.clean(form.getAddress()));
        locationRepository.save(l);
        auditLogService.log(id == null ? AuditAction.CREATE : AuditAction.UPDATE, AuditModule.SETTINGS,
                RecordType.OFFICE_LOCATION, l.getId(), (id == null ? "Created" : "Updated") + " office location " + name + ".");
    }

    private static Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }
}
