package com.smartev.assistant.security;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.repository.UserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public DatabaseUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
		return userRepository.findByEmail(normalizedEmail)
				.map(AppUserPrincipal::from)
				.orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
	}
}
