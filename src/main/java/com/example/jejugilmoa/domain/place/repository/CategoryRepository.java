package com.example.jejugilmoa.domain.place.repository;

import com.example.jejugilmoa.domain.place.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
}
