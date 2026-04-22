package com.digitaltherapy.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "diary_entries")
public class DiaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(columnDefinition = "TEXT")
    private String situation;

    @Column(columnDefinition = "TEXT")
    private String automaticThought;

    @ElementCollection
    @CollectionTable(name = "diary_entry_emotions")
    private List<EmotionRating> emotions;

    @ManyToMany
    @JoinTable(
            name = "diary_entry_distortions",
            joinColumns = @JoinColumn(name = "diary_entry_id"),
            inverseJoinColumns = @JoinColumn(name = "distortion_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<CognitiveDistortion> distortions;

    @Column(columnDefinition = "TEXT")
    private String alternativeThought;

    private Integer moodBefore;

    private Integer moodAfter;

    private Integer beliefRatingBefore;

    private Integer beliefRatingAfter;

    private LocalDateTime createdAt;

    @Builder.Default
    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
