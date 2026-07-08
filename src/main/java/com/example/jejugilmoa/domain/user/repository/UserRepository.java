package com.example.jejugilmoa.domain.user.repository;

import com.example.jejugilmoa.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByExternalProviderAndExternalIdAndDeletedAtIsNull(String externalProvider, String externalId);
}
