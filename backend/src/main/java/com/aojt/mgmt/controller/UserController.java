package com.aojt.mgmt.controller;

import com.aojt.mgmt.dto.UserResponse;
import com.aojt.mgmt.entity.Role;
import com.aojt.mgmt.entity.User;
import com.aojt.mgmt.exception.ResourceNotFoundException;
import com.aojt.mgmt.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Returns a user without the password hash. Non-admins may only read their own record. */
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Integer id, Authentication authentication) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.ADMIN.name()));
        if (!admin && !user.getEmail().equals(authentication.getName())) {
            throw new AccessDeniedException("Access denied");
        }
        return UserResponse.from(user);
    }
}
