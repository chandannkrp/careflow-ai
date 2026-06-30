package com.careflowai.staff;

import com.careflowai.common.StaffRole;
import com.careflowai.staff.dto.CreateStaffUserRequest;
import com.careflowai.staff.dto.StaffUserResponse;
import com.careflowai.staff.dto.UpdateStaffUserRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaffUserService {

    private final StaffUserRepository staffUserRepository;

    public StaffUserService(StaffUserRepository staffUserRepository) {
        this.staffUserRepository = staffUserRepository;
    }

    @Transactional
    public StaffUser resolveActor(String actorName, StaffRole actorRole, String department) {
        String displayName = StringUtils.hasText(actorName) ? actorName.trim() : "Demo Staff";
        StaffRole role = actorRole == null ? StaffRole.INTAKE_STAFF : actorRole;

        if (StringUtils.hasText(actorName)) {
            try {
                return getByLookup(displayName);
            } catch (ResponseStatusException ignored) {
                // Fall back to display-name matching or demo creation below.
            }
        }

        return staffUserRepository.findTopByDisplayNameOrderByCreatedAtDesc(displayName)
            .filter(user -> actorRole == null || user.hasRole(role))
            .orElseGet(() -> staffUserRepository.save(new StaffUser(displayName, role, department)));
    }

    @Transactional(readOnly = true)
    public StaffUser get(UUID staffUserId) {
        return staffUserRepository.findById(staffUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found."));
    }

    @Transactional(readOnly = true)
    public StaffUser getByLookup(String staffLookup) {
        try {
            return get(UUID.fromString(staffLookup));
        } catch (IllegalArgumentException ignored) {
            return staffUserRepository.findByStaffCodeIgnoreCase(staffLookup)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found."));
        }
    }

    @Transactional(readOnly = true)
    public List<StaffUserResponse> list(StaffRole role, String department) {
        List<StaffUser> staffUsers;
        if (role != null && StringUtils.hasText(department)) {
            staffUsers = staffUserRepository.findByRoleAndDepartmentIgnoreCaseOrderByCreatedAtDesc(role, department);
        } else if (role != null) {
            staffUsers = staffUserRepository.findByRoleOrderByCreatedAtDesc(role);
        } else if (StringUtils.hasText(department)) {
            staffUsers = staffUserRepository.findByDepartmentIgnoreCaseOrderByCreatedAtDesc(department);
        } else {
            staffUsers = staffUserRepository.findAll();
        }

        return staffUsers.stream()
            .map(StaffUserResponse::from)
            .toList();
    }

    @Transactional
    public StaffUserResponse create(CreateStaffUserRequest request) {
        String staffCode = normalizeStaffCode(request.staffCode(), request.displayName(), request.role());
        if (staffUserRepository.existsByStaffCodeIgnoreCase(staffCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Staff code already exists.");
        }

        StaffUser staffUser = new StaffUser(
            request.displayName().trim(),
            staffCode,
            request.role(),
            cleanOptional(request.department()),
            cleanOptional(request.specialty())
        );
        staffUser.updateProfile(
            staffUser.getDisplayName(),
            staffUser.getStaffCode(),
            staffUser.getRole(),
            staffUser.getDepartment(),
            staffUser.getSpecialty(),
            request.active() == null || request.active()
        );
        return StaffUserResponse.from(staffUserRepository.save(staffUser));
    }

    @Transactional
    public StaffUserResponse update(UUID staffUserId, UpdateStaffUserRequest request) {
        StaffUser staffUser = get(staffUserId);
        String staffCode = request.staffCode().trim().toUpperCase();
        staffUserRepository.findByStaffCodeIgnoreCase(staffCode)
            .filter(existing -> !existing.getId().equals(staffUserId))
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Staff code already exists.");
            });

        staffUser.updateProfile(
            request.displayName().trim(),
            staffCode,
            request.role(),
            cleanOptional(request.department()),
            cleanOptional(request.specialty()),
            request.active()
        );
        return StaffUserResponse.from(staffUser);
    }

    @Transactional
    public void deactivate(UUID staffUserId) {
        StaffUser staffUser = get(staffUserId);
        staffUser.deactivate();
    }

    private String normalizeStaffCode(String staffCode, String displayName, StaffRole role) {
        if (StringUtils.hasText(staffCode)) {
            return staffCode.trim().toUpperCase();
        }
        String prefix = switch (role) {
            case INTAKE_STAFF -> "INTAKE";
            case DOCTOR -> "DOCTOR";
            case TRIAGE_NURSE -> "TRIAGE";
            case CHARGE_NURSE -> "CHARGE";
            case ADMIN -> "ADMIN";
        };
        String namePart = displayName == null ? "STAFF" : displayName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (namePart.length() > 6) {
            namePart = namePart.substring(0, 6);
        }
        if (namePart.isBlank()) {
            namePart = "STAFF";
        }
        return prefix + "-" + namePart + "-" + System.currentTimeMillis();
    }

    private String cleanOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
