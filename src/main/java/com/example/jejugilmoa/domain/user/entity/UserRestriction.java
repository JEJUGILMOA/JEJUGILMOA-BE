package com.example.jejugilmoa.domain.user.entity;

import com.example.jejugilmoa.domain.user.enums.RestrictionType;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_restriction", indexes = {
        @Index(name = "idx_user_restriction_user", columnList = "user_id")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserRestriction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RestrictionType restrictionType; // BAN, SUSPENSION, WARNING

    @Column(columnDefinition = "TEXT")
    private String reason;  // 제한 사유

    @Column
    private LocalDateTime restrictedUntil;  // 제한 해제 시간

}
