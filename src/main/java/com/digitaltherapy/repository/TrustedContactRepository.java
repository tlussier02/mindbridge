package com.digitaltherapy.repository;

import com.digitaltherapy.entity.TrustedContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrustedContactRepository extends JpaRepository<TrustedContact, UUID> {
    List<TrustedContact> findByUserId(UUID userId);
}
