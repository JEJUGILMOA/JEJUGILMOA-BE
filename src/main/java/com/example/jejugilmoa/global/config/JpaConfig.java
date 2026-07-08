package com.example.jejugilmoa.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// BaseEntity의 @CreatedDate, @LastModifiedDate 활성화
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
