package com.example.jejugilmoa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// JPA Auditing 활성화는 JpaConfig에서 담당 (중복 선언 시 jpaAuditingHandler 빈 충돌)
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class JejugilmoaApplication {

	public static void main(String[] args) {
		SpringApplication.run(JejugilmoaApplication.class, args);
	}

}
