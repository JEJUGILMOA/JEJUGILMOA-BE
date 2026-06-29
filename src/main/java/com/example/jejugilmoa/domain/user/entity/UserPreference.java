package com.example.jejugilmoa.domain.user.entity;

import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "user_preference",
        indexes = {
                @Index(name = "idx_user_preference_user", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_preference_category",
                        columnNames = {"user_id", "category_id"}
                )
        }
)
public class UserPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;  // 선호 카테고리 (자연, 음식, 카페, 전통시장, 역사, 체험, 쇼핑, 포토스팟 등)
}
