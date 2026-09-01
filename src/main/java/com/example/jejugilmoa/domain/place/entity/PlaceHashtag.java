package com.example.jejugilmoa.domain.place.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "place_hashtag", indexes = {
        @Index(name = "idx_place_hashtag_place_id", columnList = "place_id")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlaceHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false, unique = true)
    private Place place;

    @Column(name = "mid_label", length = 100)
    private String midLabel;  // clsSystem2 중분류명 (예: 자연관광지)

    @Column(name = "sub_label", length = 100)
    private String subLabel;  // clsSystem3 소분류명 (예: 해수욕장)

    /** 기존 레코드에 null 필드만 채움 — idempotent */
    public void fillMissing(String midLabel, String subLabel) {
        if (this.midLabel == null && midLabel != null) this.midLabel = midLabel;
        if (this.subLabel == null && subLabel != null) this.subLabel = subLabel;
    }
}
