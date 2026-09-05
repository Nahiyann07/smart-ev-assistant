package com.smartev.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class RecommendationScorerTests {
	private final RecommendationScorer scorer = new RecommendationScorer();

	@Test void perfectInputsScoreOneHundredWithCoordinates() {
		var result = scorer.score(4, 4, 5, 150, 0.0);
		assertThat(result.score()).isEqualTo(100.0);
		assertThat(result.breakdown().distancePoints()).isEqualTo(10.0);
		assertThat(result.reasons()).contains("Good port availability", "Highly rated by drivers", "High-speed charging", "Close to your location");
	}

	@Test void missingCoordinatesProportionallyNormalizesRemainingWeights() {
		var result = scorer.score(4, 4, 5, 150, null);
		assertThat(result.score()).isEqualTo(100.0);
		assertThat(result.breakdown().distanceScore()).isNull();
		assertThat(result.breakdown().distancePoints()).isZero();
	}

	@Test void distanceContributionReachesZeroAtTwentyFiveKilometres() {
		assertThat(scorer.score(4, 4, 5, 150, 25.0).score()).isEqualTo(90.0);
		assertThat(scorer.score(4, 4, 5, 150, 100.0).score()).isEqualTo(90.0);
	}
}
