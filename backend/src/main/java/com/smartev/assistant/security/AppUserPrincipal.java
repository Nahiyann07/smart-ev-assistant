package com.smartev.assistant.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.smartev.assistant.entity.User;
import com.smartev.assistant.enums.Role;

public record AppUserPrincipal(Long id, String name, String username, String password,
		Role role, boolean enabled) implements UserDetails {

	public static AppUserPrincipal from(User user) {
		return new AppUserPrincipal(user.getId(), user.getName(), user.getEmail(),
				user.getPasswordHash(), user.getRole(), user.isEnabled());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override public String getUsername() { return username; }
	@Override public String getPassword() { return password; }

	@Override public boolean isAccountNonExpired() { return true; }
	@Override public boolean isAccountNonLocked() { return true; }
	@Override public boolean isCredentialsNonExpired() { return true; }
	@Override public boolean isEnabled() { return enabled; }
}
