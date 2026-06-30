package com.careflowai.staff;

import com.careflowai.common.StaffRole;
import com.careflowai.staff.dto.CreateStaffUserRequest;
import com.careflowai.staff.dto.StaffUserResponse;
import com.careflowai.staff.dto.UpdateStaffUserRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
public class StaffUserController {

    private final StaffUserService staffUserService;

    public StaffUserController(StaffUserService staffUserService) {
        this.staffUserService = staffUserService;
    }

    @GetMapping("/{staffLookup}")
    public StaffUserResponse get(@PathVariable String staffLookup) {
        return StaffUserResponse.from(staffUserService.getByLookup(staffLookup));
    }

    @GetMapping
    public List<StaffUserResponse> list(@RequestParam(required = false) StaffRole role,
                                        @RequestParam(required = false) String department) {
        return staffUserService.list(role, department);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffUserResponse create(@Valid @RequestBody CreateStaffUserRequest request) {
        return staffUserService.create(request);
    }

    @PutMapping("/{staffUserId}")
    public StaffUserResponse update(@PathVariable UUID staffUserId,
                                    @Valid @RequestBody UpdateStaffUserRequest request) {
        return staffUserService.update(staffUserId, request);
    }

    @DeleteMapping("/{staffUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID staffUserId) {
        staffUserService.deactivate(staffUserId);
    }
}
