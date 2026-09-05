package com.smartev.assistant.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.smartev.assistant.dto.request.StationRequest;
import com.smartev.assistant.enums.ChargerType;
import com.smartev.assistant.enums.StationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "stations")
public class Station {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@Column(nullable = false, length = 150) private String name;
	@Column(nullable = false, length = 255) private String address;
	@Column(nullable = false, length = 100) private String city;
	@Column(precision = 9, scale = 6) private BigDecimal latitude;
	@Column(precision = 9, scale = 6) private BigDecimal longitude;
	@Enumerated(EnumType.STRING) @Column(name = "charger_type", nullable = false, length = 20) private ChargerType chargerType;
	@Column(name = "total_ports", nullable = false) private int totalPorts;
	@Column(name = "available_ports", nullable = false) private int availablePorts;
	@Column(name = "out_of_service_ports", nullable = false) private int outOfServicePorts;
	@Column(name = "charging_speed_kw", nullable = false, precision = 6, scale = 2) private BigDecimal chargingSpeedKw;
	@Column(name = "operating_hours", nullable = false, length = 100) private String operatingHours;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private StationStatus status;
	@Column(nullable = false, columnDefinition = "TEXT") private String description;
	@Column(name = "deleted_at") private Instant deletedAt;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	@Column(name = "updated_at", nullable = false) private Instant updatedAt;

	protected Station() {}

	public Station(StationRequest request) { apply(request); }

	public void apply(StationRequest request) {
		name = request.name().trim(); address = request.address().trim(); city = request.city().trim();
		latitude = request.latitude(); longitude = request.longitude(); chargerType = request.chargerType();
		totalPorts = request.totalPorts(); availablePorts = request.availablePorts();
		outOfServicePorts = request.outOfServicePorts(); chargingSpeedKw = request.chargingSpeedKw();
		operatingHours = request.operatingHours().trim(); status = request.status(); description = request.description().trim();
	}

	public void softDelete() { deletedAt = Instant.now(); }
	public void updateAvailability(StationStatus newStatus, int newAvailablePorts, int newOutOfServicePorts) {
		status = newStatus; availablePorts = newAvailablePorts; outOfServicePorts = newOutOfServicePorts;
	}
	@PrePersist void created() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
	@PreUpdate void updated() { updatedAt = Instant.now(); }

	public Long getId() { return id; }
	public String getName() { return name; }
	public String getAddress() { return address; }
	public String getCity() { return city; }
	public BigDecimal getLatitude() { return latitude; }
	public BigDecimal getLongitude() { return longitude; }
	public ChargerType getChargerType() { return chargerType; }
	public int getTotalPorts() { return totalPorts; }
	public int getAvailablePorts() { return availablePorts; }
	public int getOutOfServicePorts() { return outOfServicePorts; }
	public int getOccupiedPorts() { return totalPorts - availablePorts - outOfServicePorts; }
	public BigDecimal getChargingSpeedKw() { return chargingSpeedKw; }
	public String getOperatingHours() { return operatingHours; }
	public StationStatus getStatus() { return status; }
	public String getDescription() { return description; }
	public Instant getDeletedAt() { return deletedAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
