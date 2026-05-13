package com.digitaltherapy.repository;

import com.digitaltherapy.entity.SessionStatus;
import com.digitaltherapy.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    List<UserSession> findByUserIdOrderByStartedAtDesc(UUID userId);

    @Query("SELECT us FROM UserSession us WHERE us.user.id = :userId AND us.startedAt >= :startDate AND us.startedAt < :endDate")
    List<UserSession> findByUserIdAndDateRange(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.user.id = :userId AND us.status = 'COMPLETED'")
    Long countCompletedByUserId(@Param("userId") UUID userId);

    Optional<UserSession> findFirstByUserIdAndStatusOrderByStartedAtDesc(UUID userId, SessionStatus status);
}
