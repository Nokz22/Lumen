package dev.lumen.domain.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.lumen.domain.user.Role;
import dev.lumen.domain.user.User;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AssessmentTest {

    private final User user =
            new User("jane@lumen.dev", "hashed-password", "Jane", "en", "PT", LocalDate.of(1990, 1, 1), Role.USER);

    @Test
    void shouldStartInScheduledStatus() {
        Assessment assessment = new Assessment(user, AssessmentType.PHQ9);

        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.SCHEDULED);
        assertThat(assessment.getStartedAt()).isNull();
    }

    @Test
    void shouldProgressThroughTheFullLifecycle() {
        Assessment assessment = new Assessment(user, AssessmentType.PHQ9);

        assessment.start();
        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.IN_PROGRESS);
        assertThat(assessment.getStartedAt()).isNotNull();

        assessment.complete();
        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.COMPLETED);
        assertThat(assessment.getCompletedAt()).isNotNull();

        assessment.score();
        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.SCORED);
    }

    @Test
    void shouldRejectCompletingBeforeStarting() {
        Assessment assessment = new Assessment(user, AssessmentType.PHQ9);

        assertThatThrownBy(assessment::complete).isInstanceOf(InvalidAssessmentStateException.class);
    }

    @Test
    void shouldRejectScoringBeforeCompleting() {
        Assessment assessment = new Assessment(user, AssessmentType.PHQ9);
        assessment.start();

        assertThatThrownBy(assessment::score).isInstanceOf(InvalidAssessmentStateException.class);
    }

    @Test
    void shouldRejectStartingTwice() {
        Assessment assessment = new Assessment(user, AssessmentType.PHQ9);
        assessment.start();

        assertThatThrownBy(assessment::start).isInstanceOf(InvalidAssessmentStateException.class);
    }
}
