package com.smartev.assistant.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartev.assistant.dto.response.AdminUserResponse;
import com.smartev.assistant.dto.response.DashboardStatisticsResponse;
import com.smartev.assistant.entity.User;
import com.smartev.assistant.enums.ReportStatus;
import com.smartev.assistant.enums.StationStatus;
import com.smartev.assistant.exception.BadRequestException;
import com.smartev.assistant.exception.NotFoundException;
import com.smartev.assistant.repository.FavouriteRepository;
import com.smartev.assistant.repository.ReportRepository;
import com.smartev.assistant.repository.ReviewRepository;
import com.smartev.assistant.repository.StationRepository;
import com.smartev.assistant.repository.UserRepository;

@Service
public class AdminService {
	private final UserRepository users; private final StationRepository stations; private final ReportRepository reports;
	private final ReviewRepository reviews; private final FavouriteRepository favourites;
	public AdminService(UserRepository users, StationRepository stations, ReportRepository reports,
			ReviewRepository reviews, FavouriteRepository favourites) {
		this.users = users; this.stations = stations; this.reports = reports; this.reviews = reviews; this.favourites = favourites;
	}
	@Transactional(readOnly = true)
	public DashboardStatisticsResponse dashboard() {
		return new DashboardStatisticsResponse(users.count(), users.countByEnabledTrue(), stations.countByDeletedAtIsNull(),
				stations.countByStatusAndDeletedAtIsNull(StationStatus.AVAILABLE),
				stations.countByStatusAndDeletedAtIsNull(StationStatus.UNDER_MAINTENANCE),
				stations.countByStatusAndDeletedAtIsNull(StationStatus.OUT_OF_SERVICE),
				reports.countByStatus(ReportStatus.PENDING), reviews.count(), favourites.count());
	}
	@Transactional(readOnly = true)
	public List<AdminUserResponse> users() { return users.findAllByOrderByCreatedAtDesc().stream().map(this::response).toList(); }
	@Transactional
	public AdminUserResponse setEnabled(Long targetId, Long currentAdminId, boolean enabled) {
		if (targetId.equals(currentAdminId) && !enabled)
			throw new BadRequestException("SELF_DISABLE_NOT_ALLOWED", "You cannot disable your own account");
		User target = users.findById(targetId).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
		target.setEnabled(enabled); return response(target);
	}
	private AdminUserResponse response(User user) { return new AdminUserResponse(user.getId(), user.getName(), user.getEmail(),
			user.getRole(), user.isEnabled(), user.getCreatedAt(), user.getUpdatedAt()); }
}
