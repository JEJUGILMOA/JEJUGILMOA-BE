package com.example.jejugilmoa.domain.record.entity;

import lombok.*;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shared_record", indexes = {
        @Index(name = "idx_shared_token", columnList = "share_token")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TravelSharedRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "travel_record_id",
            nullable = false,
            unique = true
    )
    private TravelRecord travelRecord;

    @Column(nullable = false, unique = true, length = 255)
    private String shareToken;  // 공유 토큰 (외부 링크용)

    private LocalDateTime expiredAt; // 공유 링크 만료 시간

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true; // 공유 링크 활성화 상태

}
