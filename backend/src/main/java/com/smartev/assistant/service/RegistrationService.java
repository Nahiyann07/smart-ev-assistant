package com.smartev.assistant.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.dto.request.RegistrationRequest;
import com.smartev.assistant.dto.response.RegistrationResponse;
import com.smartev.assistant.entity.User;
import com.smartev.assistant.enums.Role;
import com.smartev.assistant.exception.ConflictException;
import com.smartev.assistant.repository.UserRepository;

@Service
public class RegistrationService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public RegistrationResponse register(RegistrationRequest request) {
		String normalizedEmail = request.email().strip().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw duplicateEmail();
		}

		User user = new User(
				request.name().strip(),
				normalizedEmail,
				passwordEncoder.encode(request.password()),
				Role.USER);

		try {
			User saved = userRepository.saveAndFlush(user);
			return new RegistrationResponse(
					saved.getId(), saved.getName(), saved.getEmail(), saved.getRole(), saved.getCreatedAt());
		} catch (DataIntegrityViolationException exception) {
			throw duplicateEmail();
		}
	}

	private ConflictException duplicateEmail() {
		return new ConflictException("EMAIL_ALREADY_REGISTERED", "An account already exists for this email address");
	}
}
