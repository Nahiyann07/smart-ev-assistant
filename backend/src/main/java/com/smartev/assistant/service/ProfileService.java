package com.smartev.assistant.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartev.assistant.dto.response.ProfileResponse;
import com.smartev.assistant.entity.User;
import com.smartev.assistant.exception.NotFoundException;
import com.smartev.assistant.repository.FavouriteRepository;
import com.smartev.assistant.repository.ReportRepository;
import com.smartev.assistant.repository.ReviewRepository;
import com.smartev.assistant.repository.UserRepository;

@Service
public class ProfileService {
	private final UserRepository users; private final FavouriteRepository favourites;
	private final ReviewRepository reviews; private final ReportRepository reports;
	public ProfileService(UserRepository users, FavouriteRepository favourites, ReviewRepository reviews, ReportRepository reports) {
		this.users = users; this.favourites = favourites; this.reviews = reviews; this.reports = reports;
	}
	@Transactional(readOnly = true)
	public ProfileResponse get(Long id) {
		User user = users.findById(id).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
		return new ProfileResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt(),
				favourites.countByUserId(id), reviews.countByUserId(id), reports.countByUserId(id));
	}
}
