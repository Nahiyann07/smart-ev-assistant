package com.smartev.assistant.controller.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.dto.request.StationRequest;
import com.smartev.assistant.entity.Review;
import com.smartev.assistant.entity.Station;
import com.smartev.assistant.entity.User;
import com.smartev.assistant.enums.ChargerType;
import com.smartev.assistant.enums.Role;
import com.smartev.assistant.enums.StationStatus;
import com.smartev.assistant.repository.ReviewRepository;
import com.smartev.assistant.repository.StationRepository;
import com.smartev.assistant.repository.UserRepository;
import com.smartev.assistant.security.AppUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StationSearchIntegrationTests {
	@Autowired MockMvc mockMvc;
	@Autowired StationRepository stationRepository;
	@Autowired ReviewRepository reviewRepository;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;
	private AppUserPrincipal driver;
	private Station central;

	@BeforeEach
	void data() {
		User user = userRepository.saveAndFlush(new User("Search Driver", "search" + System.nanoTime() + "@example.com",
				passwordEncoder.encode("Charge123"), Role.USER));
		driver = AppUserPrincipal.from(user);
		central = stationRepository.save(station("Voltway Central", "Bengaluru", ChargerType.DC_FAST, 150, 2, 12.9716, 77.5946));
		Station airport = stationRepository.save(station("Airport Charge Hub", "Bengaluru", ChargerType.DC_FAST, 60, 0, 13.1986, 77.7066));
		stationRepository.save(station("Mysuru AC Point", "Mysuru", ChargerType.AC, 22, 3, 12.2958, 76.6394));
		stationRepository.flush();
		reviewRepository.saveAndFlush(new Review(user, central, 5, "Reliable"));
	}

	@Test
	void filtersAndReturnsStablePageMetadataWithRatings() throws Exception {
		mockMvc.perform(get("/api/stations").with(user(driver))
				.param("city", " bengaluru ").param("chargerType", "DC_FAST")
				.param("availableOnly", "true").param("minSpeedKw", "100").param("minRating", "4"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].name").value("Voltway Central"))
				.andExpect(jsonPath("$.content[0].latitude").value(12.9716))
				.andExpect(jsonPath("$.content[0].longitude").value(77.5946))
				.andExpect(jsonPath("$.content[0].averageRating").value(5.0))
				.andExpect(jsonPath("$.content[0].reviewCount").value(1))
				.andExpect(jsonPath("$.first").value(true)).andExpect(jsonPath("$.last").value(true));
	}

	@Test
	void supportsDistanceSortAndPagination() throws Exception {
		mockMvc.perform(get("/api/stations").with(user(driver)).param("sort", "distance")
				.param("latitude", "12.97").param("longitude", "77.59").param("size", "2"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].name").value("Voltway Central"))
				.andExpect(jsonPath("$.content[0].distanceKm").isNumber())
				.andExpect(jsonPath("$.size").value(2)).andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void rejectsUnsafePagingAndIncompleteCoordinates() throws Exception {
		mockMvc.perform(get("/api/stations").with(user(driver)).param("size", "51"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PAGE"));
		mockMvc.perform(get("/api/stations").with(user(driver)).param("sort", "distance").param("latitude", "12"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
	}

	private Station station(String name, String city, ChargerType type, double speed, int available, double lat, double lon) {
		return new Station(new StationRequest(name, "Test address", city, BigDecimal.valueOf(lat), BigDecimal.valueOf(lon), type,
				4, available, 0, BigDecimal.valueOf(speed), "24 hours",
				available > 0 ? StationStatus.AVAILABLE : StationStatus.OCCUPIED, "Test station"));
	}
}
