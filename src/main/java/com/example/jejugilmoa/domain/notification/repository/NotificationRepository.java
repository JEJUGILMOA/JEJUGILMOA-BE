package com.example.jejugilmoa.domain.notification.repository;

import com.example.jejugilmoa.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {}
