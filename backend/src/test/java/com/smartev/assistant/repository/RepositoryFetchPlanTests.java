package com.smartev.assistant.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

class RepositoryFetchPlanTests {
	@Test
	void collectionQueriesDeclareTheAssociationsNeededByResponseMapping() throws Exception {
		assertPaths(ReviewRepository.class.getMethod("findAllByStationIdOrderByCreatedAtDesc", Long.class), "user", "station");
		assertPaths(ReportRepository.class.getMethod("findAllByOrderByCreatedAtDesc"), "user", "station", "resolvedBy");
		assertPaths(FavouriteRepository.class.getMethod("findAllByUserIdOrderByCreatedAtDesc", Long.class), "station");
	}

	private void assertPaths(java.lang.reflect.Method method, String... paths) {
		EntityGraph graph = method.getAnnotation(EntityGraph.class);
		assertThat(graph).isNotNull();
		assertThat(Arrays.asList(graph.attributePaths())).containsExactlyInAnyOrder(paths);
	}
}
