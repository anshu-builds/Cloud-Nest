package com.cloudnest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * UserRegistrationDto — Data Transfer Object for user registration.
 *
 * WHY USE DTOs?
 * - DTOs separate the API/form layer from the database entity layer
 * - We never expose entity objects directly to the frontend
 * - DTOs allow us to add validation rules specific to the form
 * - Security: prevents users from setting fields they shouldn't (like "id")
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;
}
