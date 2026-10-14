package com.aojt.mgmt.dto;

import com.aojt.mgmt.entity.User;
import com.aojt.mgmt.entity.Role;

public record UserResponse(Integer id, String firstname, String lastname, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getFirstname(), user.getLastname(), user.getEmail(), user.getRole());
    }
}
