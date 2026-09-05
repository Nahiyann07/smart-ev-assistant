package com.smartev.assistant.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartev.assistant.entity.User;
import com.smartev.assistant.enums.Role;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);
	long countByEnabledTrue();
	List<User> findAllByOrderByCreatedAtDesc();

	@Query("select u.enabled as enabled, u.role as role from User u where u.id = :id")
	Optional<SecurityState> findSecurityStateById(@Param("id") Long id);

	interface SecurityState {
		boolean getEnabled();
		Role getRole();
	}
}
