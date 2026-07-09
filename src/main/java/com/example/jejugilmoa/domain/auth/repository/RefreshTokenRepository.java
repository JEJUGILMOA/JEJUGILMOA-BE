package com.example.jejugilmoa.domain.auth.repository;

import com.example.jejugilmoa.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// TODO: 만료(expiresAt 경과)되었거나 폐기(revoked=true)된 row가 로그인/재발급마다 하나씩 쌓이고
// 삭제되지 않는다. 트래픽이 늘면 스케줄러/배치로 주기적으로 정리해야 한다.
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenId(String tokenId);

    // 리프레시 토큰 재사용(탈취 의심)이 감지되면 해당 유저가 보유한 모든 유효 토큰을 강제 폐기한다.
    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.userId = :userId and r.revoked = false")
    void revokeAllByUserId(@Param("userId") Long userId);
}
