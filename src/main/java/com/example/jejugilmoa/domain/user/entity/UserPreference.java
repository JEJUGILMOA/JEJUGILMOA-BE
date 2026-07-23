package com.example.jejugilmoa.domain.user.entity;

import com.example.jejugilmoa.domain.user.enums.TravelStyle;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "nature", nullable = false)
    @Builder.Default
    private Boolean nature = false; // 자연

    @Column(name = "food", nullable = false)
    @Builder.Default
    private Boolean food = false; // 음식

    @Column(name = "cafe", nullable = false)
    @Builder.Default
    private Boolean cafe = false; // 카페

    @Column(name = "tradition_market", nullable = false)
    @Builder.Default
    private Boolean traditionMarket = false; // 전통시장

    @Column(name = "history", nullable = false)
    @Builder.Default
    private Boolean history = false; // 역사

    @Column(name = "experience", nullable = false)
    @Builder.Default
    private Boolean experience = false; // 체험

    @Enumerated(EnumType.STRING)
    @Column(name = "travel_style", nullable = false)
    private TravelStyle travelStyle; // 여유로운/많이둘러보기/도보중심/대중교통중심

    public static UserPreference createDefault(User user) {
        return UserPreference.builder()
                .user(user)
                .travelStyle(TravelStyle.RELAXED)
                .build();
    }

    public void updatePreference(
            Boolean nature,
            Boolean food,
            Boolean cafe,
            Boolean traditionMarket,
            Boolean history,
            Boolean experience,
            TravelStyle travelStyle
    ) {
        if (nature != null) this.nature = nature;
        if (food != null) this.food = food;
        if (cafe != null) this.cafe = cafe;
        if (traditionMarket != null) this.traditionMarket = traditionMarket;
        if (history != null) this.history = history;
        if (experience != null) this.experience = experience;
        if (travelStyle != null) this.travelStyle = travelStyle;
    }
}
