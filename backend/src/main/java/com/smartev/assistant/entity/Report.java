package com.smartev.assistant.entity;

import java.time.Instant;

import com.smartev.assistant.enums.IssueType;
import com.smartev.assistant.enums.ReportStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity @Table(name = "reports")
public class Report {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "station_id") private Station station;
	@Enumerated(EnumType.STRING) @Column(name = "issue_type", nullable = false, length = 40) private IssueType issueType;
	@Column(nullable = false, length = 1500) private String description;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ReportStatus status;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "resolved_by_user_id") private User resolvedBy;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	@Column(name = "resolved_at") private Instant resolvedAt;
	protected Report() {}
	public Report(User user, Station station, IssueType issueType, String description) {
		this.user = user; this.station = station; this.issueType = issueType; this.description = description; status = ReportStatus.PENDING;
	}
	@PrePersist void created() { createdAt = Instant.now(); }
	public void resolve(ReportStatus resolution, User admin) { status = resolution; resolvedBy = admin; resolvedAt = Instant.now(); }
	public Long getId() { return id; } public User getUser() { return user; } public Station getStation() { return station; }
	public IssueType getIssueType() { return issueType; } public String getDescription() { return description; }
	public ReportStatus getStatus() { return status; } public User getResolvedBy() { return resolvedBy; }
	public Instant getCreatedAt() { return createdAt; } public Instant getResolvedAt() { return resolvedAt; }
}
