package com.example.jejugilmoa.domain.place.entity;

import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name = "category", indexes = {
        @Index(name = "idx_category_name", columnList = "name")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false, unique = true)
    private String name;  // 자연, 음식, 카페, 전통시장, 역사, 체험, 쇼핑, 포토스팟

    @Column(columnDefinition = "TEXT")
    private String description;
}
