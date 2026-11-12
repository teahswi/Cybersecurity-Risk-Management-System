package com.cybershield.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.cybershield.enums.TaskStatus;
import org.junit.jupiter.api.Test;

/** Tests 15 and 16: valid and invalid remediation status transitions (pure state machine). */
class TaskStatusTest {

    @Test
    void validTransitionsFollowTheLifecycle() {
        assertThat(TaskStatus.OPEN.canTransitionTo(TaskStatus.ASSIGNED)).isTrue();
        assertThat(TaskStatus.ASSIGNED.canTransitionTo(TaskStatus.IN_PROGRESS)).isTrue();
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.PENDING_VERIFICATION)).isTrue();
        assertThat(TaskStatus.PENDING_VERIFICATION.canTransitionTo(TaskStatus.COMPLETED)).isTrue();
        assertThat(TaskStatus.PENDING_VERIFICATION.canTransitionTo(TaskStatus.IN_PROGRESS)).isTrue();
        assertThat(TaskStatus.COMPLETED.canTransitionTo(TaskStatus.REOPENED)).isTrue();
        assertThat(TaskStatus.REOPENED.canTransitionTo(TaskStatus.IN_PROGRESS)).isTrue();
    }

    @Test
    void invalidTransitionsAreRejected() {
        assertThat(TaskStatus.OPEN.canTransitionTo(TaskStatus.COMPLETED)).isFalse();
        assertThat(TaskStatus.OPEN.canTransitionTo(TaskStatus.IN_PROGRESS)).isFalse();
        assertThat(TaskStatus.ASSIGNED.canTransitionTo(TaskStatus.PENDING_VERIFICATION)).isFalse();
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.COMPLETED)).isFalse();
        assertThat(TaskStatus.COMPLETED.canTransitionTo(TaskStatus.IN_PROGRESS)).isFalse();
        assertThat(TaskStatus.REOPENED.canTransitionTo(TaskStatus.COMPLETED)).isFalse();
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(null)).isFalse();
    }

    @Test
    void labelsAreHumanReadable() {
        assertThat(TaskStatus.PENDING_VERIFICATION.getLabel()).isEqualTo("Pending Verification");
        assertThat(TaskStatus.PENDING_VERIFICATION.getCssKey()).isEqualTo("pending-verification");
    }
}
