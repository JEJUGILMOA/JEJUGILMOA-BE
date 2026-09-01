package com.example.jejugilmoa.domain.record.entity;

import com.example.jejugilmoa.domain.record.enums.ReactionType;
import com.example.jejugilmoa.domain.user.entity.User;
import lombok.*;

import jakarta.persistence.*;


@Entity
@Table(
        name = "record_reaction",
        indexes = {
                @Index(name = "idx_reaction_record", columnList = "travel_record_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_record_reaction",
                        columnNames = {"travel_record_id", "user_id"}
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TravelRecordReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_record_id", nullable = false)
    private TravelRecord travelRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReactionType reactionType;  // LIKE, DISLIKE

}
