package com.example.jejugilmoa.domain.user.repository;

import com.example.jejugilmoa.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByExternalProviderAndExternalIdAndDeletedAtIsNull(String externalProvider, String externalId);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT COUNT(ub) FROM UserBadge ub WHERE ub.user.id = :userId")
    long countBadgesByUserId(@Param("userId") Long userId);
}
