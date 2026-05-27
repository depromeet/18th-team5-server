package com.team.peektime_api.domain.notification.service;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.team.peektime_api.domain.notification.dto.PushNotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Service
public class FcmService {

    private static final String TOPIC_ALL_USERS = "all_users";

    /**
     * 전체 사용자에게 알림 전송 (Topic 방식)
     */
    public void sendToAll(PushNotificationRequest request) {
        sendToTopic(TOPIC_ALL_USERS, request);
    }

    /**
     * 특정 Topic으로 알림 전송
     */
    public void sendToTopic(String topic, PushNotificationRequest request) {
        try {
            Message message = buildTopicMessage(topic, request);
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM Topic 전송 성공: topic={}, response={}", topic, response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM Topic 전송 실패: topic={}", topic, e);
            throw new RuntimeException("푸시 알림 전송 실패", e);
        }
    }

    private Message buildTopicMessage(String topic, PushNotificationRequest request) {
        Message.Builder builder = Message.builder()
                .setTopic(topic)
                .setNotification(buildNotification(request))
                // iOS 설정
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setSound("default")
                                .build())
                        .build());

        if (request.getData() != null) {
            builder.putAllData(request.getData());
        }

        return builder.build();
    }

    private Notification buildNotification(PushNotificationRequest request) {
        Notification.Builder builder = Notification.builder()
                .setTitle(request.getTitle())
                .setBody(request.getBody());

        if (StringUtils.hasText(request.getImageUrl())) {
            builder.setImage(request.getImageUrl());
        }

        return builder.build();
    }
}