package com.cybershield.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cybershield.enums.RiskLevel;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.service.RiskService;
import org.junit.jupiter.api.Test;

/** Test 26: Risk Score = Likelihood x Impact and the level boundaries 4/5, 9/10, 16/17. */
class RiskScoreTest {

    @Test
    void scoreIsLikelihoodTimesImpact() {
        assertThat(RiskService.calculateScore(1, 1)).isEqualTo(1);
        assertThat(RiskService.calculateScore(4, 5)).isEqualTo(20);
        assertThat(RiskService.calculateScore(5, 5)).isEqualTo(25);
    }

    @Test
    void levelBoundaries() {
        assertThat(RiskLevel.fromScore(4)).isEqualTo(RiskLevel.LOW);
        assertThat(RiskLevel.fromScore(5)).isEqualTo(RiskLevel.MEDIUM);
        assertThat(RiskLevel.fromScore(9)).isEqualTo(RiskLevel.MEDIUM);
        assertThat(RiskLevel.fromScore(10)).isEqualTo(RiskLevel.HIGH);
        assertThat(RiskLevel.fromScore(16)).isEqualTo(RiskLevel.HIGH);
        assertThat(RiskLevel.fromScore(17)).isEqualTo(RiskLevel.CRITICAL);
        assertThat(RiskLevel.fromScore(25)).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void outOfRangeValuesAreRejected() {
        assertThatThrownBy(() -> RiskService.calculateScore(0, 3)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> RiskService.calculateScore(3, 6)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> RiskLevel.fromScore(26)).isInstanceOf(IllegalArgumentException.class);
    }
}
