package com.smartev.assistant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
		@NotBlank(message = "Name is required")
		@Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
		String name,

		@NotBlank(message = "Email is required")
		@Email(message = "Enter a valid email address")
		@Size(max = 255, message = "Email must not exceed 255 characters")
		String email,

		@NotBlank(message = "Password is required")
		@Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one number")
		String password) {

	public RegistrationRequest {
		name = name == null ? null : name.trim();
		email = email == null ? null : email.trim();
	}
}
