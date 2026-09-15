package com.wrg.api;

import com.wrg.api.dto.UserDtos.MemberProfileResponse;
import com.wrg.api.dto.UserDtos.UpdateUserRequest;
import com.wrg.api.dto.UserDtos.UserResponse;
import com.wrg.security.CurrentUser;
import com.wrg.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public List<UserResponse> list() {
        return users.list();
    }

    @GetMapping("/{id}/profile")
    public MemberProfileResponse profile(@PathVariable Long id) {
        return users.profile(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return users.update(id, CurrentUser.require(), request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        users.delete(id, CurrentUser.require());
    }
}
