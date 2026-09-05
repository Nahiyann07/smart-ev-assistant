package com.smartev.assistant.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.dto.request.ReviewRequest;
import com.smartev.assistant.dto.response.ReviewResponse;
import com.smartev.assistant.entity.Review;
import com.smartev.assistant.entity.Station;
import com.smartev.assistant.entity.User;
import com.smartev.assistant.exception.ConflictException;
import com.smartev.assistant.exception.ForbiddenException;
import com.smartev.assistant.exception.NotFoundException;
import com.smartev.assistant.repository.ReviewRepository;
import com.smartev.assistant.repository.StationRepository;
import com.smartev.assistant.repository.UserRepository;

@Service
public class ReviewService {
	private final ReviewRepository reviews; private final StationRepository stations; private final UserRepository users;
	public ReviewService(ReviewRepository reviews, StationRepository stations, UserRepository users) {
		this.reviews = reviews; this.stations = stations; this.users = users;
	}

	@Transactional(readOnly = true)
	public List<ReviewResponse> list(Long stationId) {
		activeStation(stationId);
		return reviews.findAllByStationIdOrderByCreatedAtDesc(stationId).stream().map(this::response).toList();
	}

	@Transactional
	public ReviewResponse create(Long stationId, Long userId, ReviewRequest request) {
		if (reviews.existsByUserIdAndStationId(userId, stationId)) throw duplicate();
		Station station = activeStation(stationId); User user = user(userId);
		try {
			return response(reviews.saveAndFlush(new Review(user, station, request.rating(), request.comment().trim())));
		} catch (DataIntegrityViolationException exception) {
			throw duplicate();
		}
	}

	@Transactional
	public ReviewResponse update(Long reviewId, Long userId, ReviewRequest request) {
		Review review = review(reviewId); requireAuthor(review, userId);
		review.update(request.rating(), request.comment().trim()); return response(review);
	}

	@Transactional
	public void delete(Long reviewId, Long userId) {
		Review review = review(reviewId); requireAuthor(review, userId); reviews.delete(review);
	}

	private ReviewResponse response(Review review) {
		return new ReviewResponse(review.getId(), review.getUser().getId(), review.getUser().getName(),
				review.getStation().getId(), review.getRating(), review.getComment(), review.getCreatedAt(), review.getUpdatedAt());
	}
	private Station activeStation(Long id) { return stations.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new NotFoundException("STATION_NOT_FOUND", "Station not found")); }
	private User user(Long id) { return users.findById(id).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found")); }
	private Review review(Long id) { return reviews.findById(id).orElseThrow(() -> new NotFoundException("REVIEW_NOT_FOUND", "Review not found")); }
	private void requireAuthor(Review review, Long userId) { if (!review.getUser().getId().equals(userId)) throw new ForbiddenException("REVIEW_NOT_OWNED", "Only the review author can change this review"); }
	private ConflictException duplicate() { return new ConflictException("REVIEW_ALREADY_EXISTS", "You have already reviewed this station"); }
}
