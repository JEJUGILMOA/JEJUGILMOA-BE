package com.example.jejugilmoa.domain.user.repository;

import com.example.jejugilmoa.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByExternalProviderAndExternalIdAndDeletedAtIsNull(String externalProvider, String externalId);

    Optional<User> findByExternalProviderAndExternalId(String externalProvider, String externalId);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NOT NULL")
    Optional<User> findByIdAndDeletedAtIsNotNullForUpdate(@Param("id") Long id);

    @Query("SELECT COUNT(ub) FROM UserBadge ub WHERE ub.user.id = :userId")
    long countBadgesByUserId(@Param("userId") Long userId);

    List<User> findByDeletedAtBeforeAndAnonymizedAtIsNull(LocalDateTime cutoff);
}
