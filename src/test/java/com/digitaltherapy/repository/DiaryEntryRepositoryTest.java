package com.digitaltherapy.repository;

import com.digitaltherapy.entity.DiaryEntry;
import com.digitaltherapy.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never"
})
class DiaryEntryRepositoryTest {

    @Autowired
    private DiaryEntryRepository diaryEntryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("diary-test@example.com")
                .passwordHash("hashed")
                .name("Diary Test User")
                .onboardingComplete(false)
                .streakDays(0)
                .build();
        entityManager.persistAndFlush(testUser);

        // Entry 1: active, mood improvement of 4 (3 -> 7)
        DiaryEntry entry1 = DiaryEntry.builder()
                .user(testUser)
                .situation("Stressful meeting at work")
                .automaticThought("I will fail the presentation")
                .alternativeThought("I have prepared well and can handle this")
                .moodBefore(3)
                .moodAfter(7)
                .deleted(false)
                .build();
        entityManager.persistAndFlush(entry1);

        // Entry 2: active, mood improvement of 2 (5 -> 7)
        DiaryEntry entry2 = DiaryEntry.builder()
                .user(testUser)
                .situation("Argument with a friend")
                .automaticThought("They must hate me")
                .alternativeThought("Disagreements are normal in friendships")
                .moodBefore(5)
                .moodAfter(7)
                .deleted(false)
                .build();
        entityManager.persistAndFlush(entry2);

        // Entry 3: active, mood improvement of 3 (4 -> 7)
        DiaryEntry entry3 = DiaryEntry.builder()
                .user(testUser)
                .situation("Missed a deadline")
                .automaticThought("I am incompetent")
                .alternativeThought("One missed deadline does not define my abilities")
                .moodBefore(4)
                .moodAfter(7)
                .deleted(false)
                .build();
        entityManager.persistAndFlush(entry3);

        // Entry 4: soft-deleted
        DiaryEntry deletedEntry = DiaryEntry.builder()
                .user(testUser)
                .situation("Old entry that was deleted")
                .automaticThought("This should not appear")
                .alternativeThought("This was removed")
                .moodBefore(2)
                .moodAfter(6)
                .deleted(true)
                .build();
        entityManager.persistAndFlush(deletedEntry);

        entityManager.clear();
    }

    @Test
    @DisplayName("findByUserIdAndDeletedFalse excludes soft-deleted entries")
    void findByUserIdAndDeletedFalseOrderByCreatedAtDesc_ExcludesDeleted() {
        Page<DiaryEntry> page = diaryEntryRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(
                        testUser.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent())
                .allMatch(entry -> !entry.getDeleted());
        assertThat(page.getContent())
                .noneMatch(entry -> "Old entry that was deleted".equals(entry.getSituation()));
    }

    @Test
    @DisplayName("findByUserIdAndDeletedFalse returns paged results")
    void findByUserIdAndDeletedFalseOrderByCreatedAtDesc_ReturnsPaged() {
        Page<DiaryEntry> firstPage = diaryEntryRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(
                        testUser.getId(), PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.isFirst()).isTrue();
        assertThat(firstPage.hasNext()).isTrue();

        Page<DiaryEntry> secondPage = diaryEntryRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(
                        testUser.getId(), PageRequest.of(1, 2));

        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.isLast()).isTrue();
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    @DisplayName("calculateAverageMoodImprovement returns correct average for non-deleted entries")
    void calculateAverageMoodImprovement_ReturnsCorrectAverage() {
        // Entry 1: 7 - 3 = 4
        // Entry 2: 7 - 5 = 2
        // Entry 3: 7 - 4 = 3
        // Deleted entry should NOT be included
        // Average = (4 + 2 + 3) / 3 = 3.0
        Double average = diaryEntryRepository
                .calculateAverageMoodImprovement(testUser.getId());

        assertThat(average).isEqualTo(3.0);
    }

    @Test
    @DisplayName("calculateAverageMoodImprovement returns null when user has no entries")
    void calculateAverageMoodImprovement_ReturnsNull_WhenNoEntries() {
        User otherUser = User.builder()
                .email("noentries@example.com")
                .passwordHash("hashed")
                .name("No Entries User")
                .onboardingComplete(false)
                .streakDays(0)
                .build();
        entityManager.persistAndFlush(otherUser);

        Double average = diaryEntryRepository
                .calculateAverageMoodImprovement(otherUser.getId());

        assertThat(average).isNull();
    }

    @Test
    @DisplayName("calculateAverageMoodImprovement excludes entries with null mood values")
    void calculateAverageMoodImprovement_ExcludesNullMoods() {
        DiaryEntry noMoodEntry = DiaryEntry.builder()
                .user(testUser)
                .situation("Entry without mood ratings")
                .automaticThought("No mood tracked")
                .alternativeThought("Still no mood")
                .moodBefore(null)
                .moodAfter(null)
                .deleted(false)
                .build();
        entityManager.persistAndFlush(noMoodEntry);
        entityManager.clear();

        Double average = diaryEntryRepository
                .calculateAverageMoodImprovement(testUser.getId());

        // Should still be 3.0 because the null-mood entry is excluded by the query
        assertThat(average).isEqualTo(3.0);
    }
}
