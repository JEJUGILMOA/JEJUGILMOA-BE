package com.example.jejugilmoa.domain.notification;

import com.example.jejugilmoa.domain.notification.dto.DeviceTokenRegisterRequest;
import com.example.jejugilmoa.domain.notification.enums.FcmPlatform;
import com.example.jejugilmoa.domain.notification.service.DeviceTokenService;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DeviceTokenConcurrentRegistrationTest {

    @Autowired DeviceTokenService deviceTokenService;
    @Autowired UserRepository userRepository;
    @Autowired JdbcClient jdbcClient;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = userRepository.saveAndFlush(User.builder().nickname("동시성테스트유저A").build());
        user2 = userRepository.saveAndFlush(User.builder().nickname("동시성테스트유저B").build());
    }

    @AfterEach
    void tearDown() {
        jdbcClient.sql("DELETE FROM device_token WHERE user_id = :u1 OR user_id = :u2")
                .param("u1", user1.getId())
                .param("u2", user2.getId())
                .update();
        jdbcClient.sql("DELETE FROM \"user\" WHERE id = :u1 OR id = :u2")
                .param("u1", user1.getId())
                .param("u2", user2.getId())
                .update();
    }

    @Test
    @DisplayName("동일 토큰을 서로 다른 유저가 동시에 등록해도 두 요청 모두 성공하고 토큰 행은 정확히 하나만 존재한다")
    void sameToken_concurrentRegistration_bothSucceedAndOnlyOneRowPersists() throws InterruptedException {
        String sharedToken = "fcm-concurrent-" + UUID.randomUUID();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        new Thread(() -> {
            ready.countDown();
            try {
                start.await();
                deviceTokenService.registerOrUpdateToken(user1.getId(),
                        new DeviceTokenRegisterRequest(sharedToken, FcmPlatform.IOS, "device-A"));
                successCount.incrementAndGet();
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                done.countDown();
            }
        }).start();

        new Thread(() -> {
            ready.countDown();
            try {
                start.await();
                deviceTokenService.registerOrUpdateToken(user2.getId(),
                        new DeviceTokenRegisterRequest(sharedToken, FcmPlatform.ANDROID, "device-B"));
                successCount.incrementAndGet();
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                done.countDown();
            }
        }).start();

        ready.await();
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(errors).as("두 요청 모두 예외 없이 완료되어야 한다: %s", errors).isEmpty();
        assertThat(successCount.get()).isEqualTo(2);

        Long count = jdbcClient.sql("SELECT COUNT(*) FROM device_token WHERE token = :token")
                .param("token", sharedToken)
                .query(Long.class)
                .single();
        assertThat(count).isOne();
    }
}
