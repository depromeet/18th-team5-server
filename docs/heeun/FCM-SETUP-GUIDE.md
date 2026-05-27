# FCM (Firebase Cloud Messaging) 구현 가이드

## 목차
1. [Firebase 프로젝트 생성](#1-firebase-프로젝트-생성)
2. [서비스 계정 키 발급](#2-서비스-계정-키-발급)
3. [Spring Boot 의존성 추가](#3-spring-boot-의존성-추가)
4. [FCM 설정 클래스 구현](#4-fcm-설정-클래스-구현)
5. [Topic 기반 알림 구현](#5-topic-기반-알림-구현)
6. [푸시 알림 전송 API](#6-푸시-알림-전송-api)
7. [테스트 방법](#7-테스트-방법)

---

## PeekTime FCM 구조

```
┌─────────────────────────────────────────────────────────────┐
│                        PeekTime FCM                         │
├─────────────────────────────────────────────────────────────┤
│  특징: 전체 알림만 존재 (개인 알림 없음)                       │
│  방식: Topic 구독 방식                                       │
│  토큰 저장: 서버에 저장 안함 (Firebase가 관리)                 │
└─────────────────────────────────────────────────────────────┘

iOS 앱                          Firebase                    서버
  │                                │                         │
  ├─ 알림 권한 승인                  │                         │
  ├─ 토픽 구독 ("all_users") ──────→│                         │
  │                                │ (토큰-토픽 매핑 저장)      │
  │                                │                         │
  │                                │←─────── 토픽으로 전송 ────┤
  │←─────── 알림 전달 ──────────────│                         │
  ▼
알림 수신!

서버는 토큰을 몰라도 됨!
```

---

## 1. Firebase 프로젝트 생성

### 1-1. Firebase Console 접속
1. [Firebase Console](https://console.firebase.google.com/) 접속
2. Google 계정으로 로그인

### 1-2. 새 프로젝트 생성
1. **"프로젝트 추가"** 클릭
2. 프로젝트 이름 입력: `peektime` (또는 원하는 이름)
3. Google Analytics 설정 (선택사항, 비활성화 가능)
4. **"프로젝트 만들기"** 클릭

### 1-3. iOS 앱 추가

1. 프로젝트 개요 > **iOS 아이콘** 클릭
2. iOS 번들 ID 입력: `com.team.peektime` (실제 번들 ID)
3. 앱 닉네임 입력: `PeekTime iOS`
4. **"앱 등록"** 클릭
5. `GoogleService-Info.plist` 다운로드 → iOS 프로젝트에 추가

---

## 2. 서비스 계정 키 발급

### 2-1. 서비스 계정 키 생성
1. Firebase Console > **프로젝트 설정** (톱니바퀴 아이콘)
2. **"서비스 계정"** 탭 클릭
3. **"새 비공개 키 생성"** 클릭
4. JSON 파일 다운로드 (예: `peektime-firebase-adminsdk-xxxxx.json`)

### 2-2. 키 파일 저장 위치

```
peektime-api/
└── src/main/resources/
    └── firebase/
        └── firebase-service-account.json  ← 여기에 저장
```

### 2-3. .gitignore 추가 (중요!)

```gitignore
# Firebase 서비스 계정 키 (절대 커밋 금지)
**/firebase-service-account.json
**/firebase/*.json
```

### 2-4. 환경별 설정

#### 로컬 개발
- 파일을 직접 `src/main/resources/firebase/` 에 저장

#### 운영 환경 (AWS, GCP 등)
- 환경 변수로 JSON 내용 전달:
```bash
export FIREBASE_CREDENTIALS='{"type":"service_account",...}'
```

---

## 3. Spring Boot 의존성 추가

### 3-1. build.gradle 수정

```groovy
// peektime-api/build.gradle

dependencies {
    // ... 기존 의존성

    // Firebase Admin SDK
    implementation 'com.google.firebase:firebase-admin:9.4.3'
}
```

### 3-2. Gradle Sync
```bash
./gradlew build --refresh-dependencies
```

---

## 4. FCM 설정 클래스 구현

### 4-1. FCM 설정 Properties

```java
// src/main/java/com/team/peektime_api/infra/firebase/FcmProperties.java

package com.team.peektime_api.infra.firebase;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "firebase")
public class FcmProperties {

    private String credentialsPath;  // 파일 경로 방식
    private String credentials;      // 환경변수 방식 (JSON 문자열)
}
```

### 4-2. application.yml 설정

```yaml
# application.yml
firebase:
  credentials-path: classpath:firebase/firebase-service-account.json

# application-prod.yml (운영)
firebase:
  credentials: ${FIREBASE_CREDENTIALS}  # 환경변수에서 읽기
```

### 4-3. FCM 초기화 설정

```java
// src/main/java/com/team/peektime_api/infra/firebase/FcmConfig.java

package com.team.peektime_api.infra.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FcmConfig {

    private final FcmProperties fcmProperties;
    private final ResourceLoader resourceLoader;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(getCredentials())
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase 초기화 완료");
            }
        } catch (IOException e) {
            log.error("Firebase 초기화 실패", e);
            throw new RuntimeException("Firebase 초기화 실패", e);
        }
    }

    private GoogleCredentials getCredentials() throws IOException {
        // 1. 환경변수 방식 (운영 환경)
        if (StringUtils.hasText(fcmProperties.getCredentials())) {
            InputStream stream = new ByteArrayInputStream(
                    fcmProperties.getCredentials().getBytes(StandardCharsets.UTF_8)
            );
            return GoogleCredentials.fromStream(stream);
        }

        // 2. 파일 경로 방식 (로컬 개발)
        if (StringUtils.hasText(fcmProperties.getCredentialsPath())) {
            InputStream stream = resourceLoader
                    .getResource(fcmProperties.getCredentialsPath())
                    .getInputStream();
            return GoogleCredentials.fromStream(stream);
        }

        throw new IllegalStateException("Firebase credentials 설정이 없습니다.");
    }
}
```

---

## 5. Topic 기반 알림 구현

### 5-1. Topic 구조

| Topic | 설명 | 대상 |
|-------|------|------|
| `all_users` | 전체 알림 | 알림 허용한 모든 사용자 |

### 5-2. iOS에서 Topic 구독 (클라이언트)

```swift
import FirebaseMessaging

// 알림 권한 승인 후 토픽 구독
func subscribeToTopic() {
    Messaging.messaging().subscribe(toTopic: "all_users") { error in
        if let error = error {
            print("토픽 구독 실패: \(error)")
            return
        }
        print("all_users 토픽 구독 성공")
    }
}

// 알림 권한 해제 시 토픽 구독 해제
func unsubscribeFromTopic() {
    Messaging.messaging().unsubscribe(fromTopic: "all_users") { error in
        if let error = error {
            print("토픽 구독 해제 실패: \(error)")
            return
        }
        print("all_users 토픽 구독 해제")
    }
}
```

### 5-3. 알림 요청 DTO

```java
// src/main/java/com/team/peektime_api/domain/notification/dto/PushNotificationRequest.java

package com.team.peektime_api.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Schema(description = "푸시 알림 요청")
@Getter
@Builder
public class PushNotificationRequest {

    @Schema(description = "알림 제목")
    private String title;

    @Schema(description = "알림 내용")
    private String body;

    @Schema(description = "추가 데이터")
    private Map<String, String> data;

    @Schema(description = "이미지 URL (선택)")
    private String imageUrl;
}
```

### 5-4. FCM 서비스 (Topic 전송)

```java
// src/main/java/com/team/peektime_api/domain/notification/service/FcmService.java

package com.team.peektime_api.domain.notification.service;

import com.google.firebase.messaging.*;
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
```

---

## 6. 푸시 알림 전송 API

### 6-1. 알림 컨트롤러

```java
// src/main/java/com/team/peektime_api/domain/notification/controller/NotificationController.java

package com.team.peektime_api.domain.notification.controller;

import com.team.peektime_api.domain.notification.dto.PushNotificationRequest;
import com.team.peektime_api.domain.notification.service.FcmService;
import com.team.peektime_api.global.common.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification", description = "푸시 알림 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final FcmService fcmService;

    @Operation(summary = "전체 사용자에게 알림 전송")
    @PostMapping("/all")
    public ResponseEntity<SuccessResponse<Void>> sendToAll(
            @RequestParam String title,
            @RequestParam String body) {

        PushNotificationRequest request = PushNotificationRequest.builder()
                .title(title)
                .body(body)
                .build();

        fcmService.sendToAll(request);
        return ResponseEntity.ok(SuccessResponse.of(null));
    }
}
```

### 6-2. 스케줄러에서 사용 예시

```java
// 특정 시간에 전체 알림 전송 예시
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final FcmService fcmService;

    @Scheduled(cron = "0 0 9 * * *")  // 매일 오전 9시
    public void sendDailyMissionNotification() {
        PushNotificationRequest notification = PushNotificationRequest.builder()
                .title("오늘의 미션이 도착했어요!")
                .body("지금 바로 확인해보세요")
                .data(Map.of("type", "DAILY_MISSION"))
                .build();

        fcmService.sendToAll(notification);
    }
}
```

---

## 7. 테스트 방법

### 7-1. Swagger에서 테스트

`/api/v1/notifications/all` - 전체 알림 전송

### 7-2. cURL로 테스트

```bash
# 전체 알림 전송
curl -X POST "http://localhost:8080/api/v1/notifications/all?title=테스트&body=알림입니다"
```

### 7-3. Firebase Console에서 테스트

1. Firebase Console > **Cloud Messaging** 메뉴
2. **"새 알림"** 클릭
3. 알림 제목/내용 입력
4. 타겟: **Topic** 선택 → `all_users` 입력
5. 전송

---

## 체크리스트

- [ ] Firebase 프로젝트 생성
- [ ] iOS 앱 등록
- [ ] 서비스 계정 키 발급 및 저장
- [ ] .gitignore에 키 파일 제외 추가
- [ ] Firebase Admin SDK 의존성 추가
- [ ] FcmConfig 설정 클래스 구현
- [ ] FcmService 구현 (Topic 전송)
- [ ] iOS에서 Topic 구독 구현
- [ ] 테스트 알림 전송 확인

---

## 참고 자료

- [Firebase Admin SDK 문서](https://firebase.google.com/docs/admin/setup)
- [FCM Topic 메시징](https://firebase.google.com/docs/cloud-messaging/topic-messaging)
- [FCM 서버 가이드](https://firebase.google.com/docs/cloud-messaging/server)