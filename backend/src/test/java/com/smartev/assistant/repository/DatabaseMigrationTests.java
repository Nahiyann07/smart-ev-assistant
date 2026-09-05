package com.smartev.assistant.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabaseMigrationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void flywayCreatesTheCompleteSchema() {
		List<String> tables = jdbcTemplate.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
				String.class);

		assertThat(tables).contains("users", "stations", "reviews", "reports", "favourites", "flyway_schema_history");
	}

	@Test
	void databaseRejectsInvalidPortCounts() {
		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO stations (
				    name, address, city, charger_type, total_ports, available_ports,
				    out_of_service_ports, charging_speed_kw, operating_hours, status,
				    description, created_at, updated_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
				"Invalid Station", "Test Road", "Test City", "DC_FAST",
				4, 3, 2, 120.00, "24 hours", "AVAILABLE", "Invalid test data"))
				.isInstanceOf(DataAccessException.class);
	}

	@Test
	void databaseRejectsDuplicateFavouritesAndInvalidRatings() {
		jdbcTemplate.update("""
				INSERT INTO users (name, email, password_hash, role, enabled, created_at, updated_at)
				VALUES ('Test User', 'test@example.com', 'hash', 'USER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""");
		jdbcTemplate.update("""
				INSERT INTO stations (
				    name, address, city, charger_type, total_ports, available_ports,
				    out_of_service_ports, charging_speed_kw, operating_hours, status,
				    description, created_at, updated_at
				) VALUES ('Test Station', 'Test Road', 'Test City', 'AC', 4, 2, 0, 22.00,
				          '24 hours', 'AVAILABLE', 'Schema constraint test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""");

		Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = 'test@example.com'", Long.class);
		Long stationId = jdbcTemplate.queryForObject("SELECT id FROM stations WHERE name = 'Test Station'", Long.class);

		jdbcTemplate.update(
				"INSERT INTO favourites (user_id, station_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
				userId, stationId);

		assertThatThrownBy(() -> jdbcTemplate.update(
				"INSERT INTO favourites (user_id, station_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
				userId, stationId))
				.isInstanceOf(DataAccessException.class);

		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO reviews (user_id, station_id, rating, comment, created_at, updated_at)
				VALUES (?, ?, 6, 'Invalid rating', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId, stationId))
				.isInstanceOf(DataAccessException.class);
	}
}
