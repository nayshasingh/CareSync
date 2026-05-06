package com.cts.healthcare_appointment_system.dto;

import com.cts.healthcare_appointment_system.enums.UserRole;
import com.cts.healthcare_appointment_system.models.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Integer userId;
    private String name;
    private UserRole role;
    private String email;
    private String phone;

    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(
                user.getUserId(),
                user.getName(),
                user.getRole(),
                user.getEmail(),
                user.getPhone());
    }
}
