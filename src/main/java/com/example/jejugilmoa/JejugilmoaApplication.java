package com.example.jejugilmoa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class JejugilmoaApplication {

	public static void main(String[] args) {
		SpringApplication.run(JejugilmoaApplication.class, args);
	}

}
