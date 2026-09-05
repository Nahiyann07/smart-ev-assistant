package com.smartev.assistant.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(name = "uk_reviews_user_station", columnNames = {"user_id", "station_id"}))
public class Review {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "station_id") private Station station;
	@Column(nullable = false) private byte rating;
	@Column(nullable = false, length = 1000) private String comment;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	@Column(name = "updated_at", nullable = false) private Instant updatedAt;
	protected Review() {}
	public Review(User user, Station station, int rating, String comment) { this.user = user; this.station = station; this.rating = (byte) rating; this.comment = comment; }
	@PrePersist void created() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
	@PreUpdate void updated() { updatedAt = Instant.now(); }
	public Long getId() { return id; } public User getUser() { return user; } public Station getStation() { return station; }
	public int getRating() { return rating; } public String getComment() { return comment; }
	public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
	public void update(int newRating, String newComment) { rating = (byte) newRating; comment = newComment; }
}
