package com.digitaltherapy.repository;

import com.digitaltherapy.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("hashed_password_123")
                .name("Test User")
                .onboardingComplete(false)
                .streakDays(0)
                .build();
        entityManager.persistAndFlush(testUser);
    }

    @Test
    @DisplayName("findByEmail returns user when email exists")
    void findByEmail_WhenEmailExists_ReturnsUser() {
        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getName()).isEqualTo("Test User");
        assertThat(found.get().getPasswordHash()).isEqualTo("hashed_password_123");
    }

    @Test
    @DisplayName("findByEmail returns empty when email does not exist")
    void findByEmail_WhenEmailDoesNotExist_ReturnsEmpty() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail returns true when email exists")
    void existsByEmail_WhenEmailExists_ReturnsTrue() {
        boolean exists = userRepository.existsByEmail("test@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail returns false when email does not exist")
    void existsByEmail_WhenEmailDoesNotExist_ReturnsFalse() {
        boolean exists = userRepository.existsByEmail("unknown@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("save and retrieve user preserves all fields")
    void saveAndRetrieve_PreservesAllFields() {
        User newUser = User.builder()
                .email("new@example.com")
                .passwordHash("another_hash")
                .name("New User")
                .onboardingComplete(true)
                .streakDays(5)
                .build();

        User saved = userRepository.save(newUser);
        entityManager.flush();
        entityManager.clear();

        Optional<User> retrieved = userRepository.findById(saved.getId());

        assertThat(retrieved).isPresent();
        User found = retrieved.get();
        assertThat(found.getEmail()).isEqualTo("new@example.com");
        assertThat(found.getPasswordHash()).isEqualTo("another_hash");
        assertThat(found.getName()).isEqualTo("New User");
        assertThat(found.getOnboardingComplete()).isTrue();
        assertThat(found.getStreakDays()).isEqualTo(5);
        assertThat(found.getId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }
}
