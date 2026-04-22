package com.digitaltherapy.repository;

import com.digitaltherapy.entity.CbtSession;
import com.digitaltherapy.entity.SessionModule;
import com.digitaltherapy.entity.SessionStatus;
import com.digitaltherapy.entity.User;
import com.digitaltherapy.entity.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never"
})
class UserSessionRepositoryTest {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private CbtSession testCbtSession;
    private UserSession completedSession1;
    private UserSession completedSession2;
    private UserSession inProgressSession;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("session-test@example.com")
                .passwordHash("hashed")
                .name("Session Test User")
                .onboardingComplete(false)
                .streakDays(0)
                .build();
        entityManager.persistAndFlush(testUser);

        SessionModule module = SessionModule.builder()
                .name("Anxiety Management")
                .description("Learn to manage anxiety")
                .category("CBT")
                .orderIndex(1)
                .build();
        entityManager.persistAndFlush(module);

        testCbtSession = CbtSession.builder()
                .module(module)
                .title("Understanding Anxiety")
                .description("First session on anxiety")
                .durationMinutes(30)
                .orderIndex(1)
                .build();
        entityManager.persistAndFlush(testCbtSession);

        // Create completed session 1 (oldest)
        completedSession1 = UserSession.builder()
                .user(testUser)
                .cbtSession(testCbtSession)
                .status(SessionStatus.COMPLETED)
                .startedAt(LocalDateTime.of(2025, 1, 10, 10, 0))
                .endedAt(LocalDateTime.of(2025, 1, 10, 10, 30))
                .moodBefore(3)
                .moodAfter(7)
                .build();
        entityManager.persistAndFlush(completedSession1);

        // Create completed session 2 (middle)
        completedSession2 = UserSession.builder()
                .user(testUser)
                .cbtSession(testCbtSession)
                .status(SessionStatus.COMPLETED)
                .startedAt(LocalDateTime.of(2025, 2, 15, 14, 0))
                .endedAt(LocalDateTime.of(2025, 2, 15, 14, 45))
                .moodBefore(4)
                .moodAfter(8)
                .build();
        entityManager.persistAndFlush(completedSession2);

        // Create in-progress session (newest)
        inProgressSession = UserSession.builder()
                .user(testUser)
                .cbtSession(testCbtSession)
                .status(SessionStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.of(2025, 3, 20, 9, 0))
                .moodBefore(5)
                .build();
        entityManager.persistAndFlush(inProgressSession);

        entityManager.clear();
    }

    @Test
    @DisplayName("findByUserIdOrderByStartedAtDesc returns sessions sorted by startedAt descending")
    void findByUserIdOrderByStartedAtDesc_ReturnsSortedSessions() {
        List<UserSession> sessions = userSessionRepository
                .findByUserIdOrderByStartedAtDesc(testUser.getId());

        assertThat(sessions).hasSize(3);
        assertThat(sessions.get(0).getStartedAt()).isEqualTo(LocalDateTime.of(2025, 3, 20, 9, 0));
        assertThat(sessions.get(1).getStartedAt()).isEqualTo(LocalDateTime.of(2025, 2, 15, 14, 0));
        assertThat(sessions.get(2).getStartedAt()).isEqualTo(LocalDateTime.of(2025, 1, 10, 10, 0));
    }

    @Test
    @DisplayName("findByUserIdAndDateRange returns only sessions within the date range")
    void findByUserIdAndDateRange_ReturnsSessionsInRange() {
        LocalDateTime startDate = LocalDateTime.of(2025, 2, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 3, 1, 0, 0);

        List<UserSession> sessions = userSessionRepository
                .findByUserIdAndDateRange(testUser.getId(), startDate, endDate);

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getStartedAt()).isEqualTo(LocalDateTime.of(2025, 2, 15, 14, 0));
        assertThat(sessions.get(0).getStatus()).isEqualTo(SessionStatus.COMPLETED);
    }

    @Test
    @DisplayName("countCompletedByUserId returns correct count of completed sessions")
    void countCompletedByUserId_ReturnsCorrectCount() {
        Long count = userSessionRepository.countCompletedByUserId(testUser.getId());

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("findFirstByUserIdAndStatusOrderByStartedAtDesc returns the latest session with given status")
    void findFirstByUserIdAndStatusOrderByStartedAtDesc_ReturnsLatest() {
        Optional<UserSession> latest = userSessionRepository
                .findFirstByUserIdAndStatusOrderByStartedAtDesc(
                        testUser.getId(), SessionStatus.COMPLETED);

        assertThat(latest).isPresent();
        assertThat(latest.get().getStartedAt()).isEqualTo(LocalDateTime.of(2025, 2, 15, 14, 0));
        assertThat(latest.get().getStatus()).isEqualTo(SessionStatus.COMPLETED);
    }

    @Test
    @DisplayName("findFirstByUserIdAndStatusOrderByStartedAtDesc returns empty when no session matches status")
    void findFirstByUserIdAndStatusOrderByStartedAtDesc_ReturnsEmpty_WhenNoMatch() {
        Optional<UserSession> latest = userSessionRepository
                .findFirstByUserIdAndStatusOrderByStartedAtDesc(
                        testUser.getId(), SessionStatus.EARLY_EXIT);

        assertThat(latest).isEmpty();
    }

    @Test
    @DisplayName("findByUserIdAndDateRange returns empty list when no sessions in range")
    void findByUserIdAndDateRange_ReturnsEmpty_WhenNoSessionsInRange() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59);

        List<UserSession> sessions = userSessionRepository
                .findByUserIdAndDateRange(testUser.getId(), startDate, endDate);

        assertThat(sessions).isEmpty();
    }
}
