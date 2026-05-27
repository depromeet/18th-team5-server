# FCM (Firebase Cloud Messaging) 구현 가이드

## 목차
1. [Firebase 프로젝트 생성](#1-firebase-프로젝트-생성)
2. [서비스 계정 키 발급](#2-서비스-계정-키-발급)
3. [Spring Boot 의존성 추가](#3-spring-boot-의존성-추가)
4. [FCM 설정 클래스 구현](#4-fcm-설정-클래스-구현)
5. [디바이스 토큰 관리](#5-디바이스-토큰-관리)
6. [FCM 서비스 구현](#6-fcm-서비스-구현)
7. [푸시 알림 전송 API](#7-푸시-알림-전송-api)
8. [테스트 방법](#8-테스트-방법)

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

## 5. 디바이스 토큰 관리

### 5-1. 토큰 저장 방식 결정

| 방식 | 장점 | 단점 |
|------|------|------|
| User 엔티티에 필드 추가 | 간단함 | 멀티 디바이스 미지원 |
| 별도 테이블 (DeviceToken) | 멀티 디바이스 지원 | 테이블 추가 필요 |
| Redis | 빠름, TTL 관리 용이 | 영속성 이슈 |

**권장: 별도 테이블 (DeviceToken)**

### 5-2. DeviceToken 엔티티

```java
// src/main/java/com/team/peektime_api/domain/notification/entity/DeviceToken.java

package com.team.peektime_api.domain.notification.entity;

import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "device_token",
       indexes = @Index(name = "idx_device_token_user", columnList = "user_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType deviceType;  // IOS, ANDROID

    @Column
    private String deviceId;  // 디바이스 고유 식별자 (선택)

    @Builder(access = AccessLevel.PRIVATE)
    private DeviceToken(User user, String token, DeviceType deviceType, String deviceId) {
        this.user = user;
        this.token = token;
        this.deviceType = deviceType;
        this.deviceId = deviceId;
    }

    public static DeviceToken create(User user, String token, DeviceType deviceType, String deviceId) {
        return DeviceToken.builder()
                .user(user)
                .token(token)
                .deviceType(deviceType)
                .deviceId(deviceId)
                .build();
    }

    public void updateToken(String newToken) {
        this.token = newToken;
    }
}
```

### 5-3. DeviceType Enum

```java
// src/main/java/com/team/peektime_api/domain/notification/entity/DeviceType.java

package com.team.peektime_api.domain.notification.entity;

public enum DeviceType {
    IOS
}
```

### 5-4. Repository

```java
// src/main/java/com/team/peektime_api/domain/notification/repository/DeviceTokenRepository.java

package com.team.peektime_api.domain.notification.repository;

import com.team.peektime_api.domain.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findByUserId(Long userId);

    Optional<DeviceToken> findByToken(String token);

    Optional<DeviceToken> findByUserIdAndDeviceId(Long userId, String deviceId);

    @Modifying
    @Query("DELETE FROM DeviceToken dt WHERE dt.token = :token")
    void deleteByToken(String token);

    @Modifying
    @Query("DELETE FROM DeviceToken dt WHERE dt.user.id = :userId")
    void deleteAllByUserId(Long userId);
}
```

### 5-5. 토큰 등록 API

```java
// src/main/java/com/team/peektime_api/domain/notification/dto/DeviceTokenRequest.java

package com.team.peektime_api.domain.notification.dto;

import com.team.peektime_api.domain.notification.entity.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "디바이스 토큰 등록 요청")
@Getter
@Setter
public class DeviceTokenRequest {

    @Schema(description = "FCM 토큰", example = "dGVzdF90b2tlbi4uLg==")
    @NotBlank(message = "토큰은 필수입니다")
    private String token;

    @Schema(description = "디바이스 타입", example = "IOS")
    @NotNull(message = "디바이스 타입은 필수입니다")
    private DeviceType deviceType;

    @Schema(description = "디바이스 ID (선택)", example = "device-uuid-1234")
    private String deviceId;
}
```

```java
// src/main/java/com/team/peektime_api/domain/notification/controller/DeviceTokenController.java

package com.team.peektime_api.domain.notification.controller;

import com.team.peektime_api.domain.notification.dto.DeviceTokenRequest;
import com.team.peektime_api.domain.notification.service.DeviceTokenService;
import com.team.peektime_api.global.common.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Device Token", description = "FCM 디바이스 토큰 관리 API")
@RestController
@RequestMapping("/api/v1/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @Operation(summary = "디바이스 토큰 등록/갱신")
    @PostMapping
    public ResponseEntity<SuccessResponse<Void>> registerToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody DeviceTokenRequest request) {

        deviceTokenService.registerToken(userId, request);
        return ResponseEntity.ok(SuccessResponse.of(null));
    }

    @Operation(summary = "디바이스 토큰 삭제 (로그아웃 시)")
    @DeleteMapping
    public ResponseEntity<SuccessResponse<Void>> deleteToken(
            @AuthenticationPrincipal Long userId,
            @RequestParam String token) {

        deviceTokenService.deleteToken(userId, token);
        return ResponseEntity.ok(SuccessResponse.of(null));
    }
}
```

### 5-6. 토큰 서비스

```java
// src/main/java/com/team/peektime_api/domain/notification/service/DeviceTokenService.java

package com.team.peektime_api.domain.notification.service;

import com.team.peektime_api.domain.notification.dto.DeviceTokenRequest;
import com.team.peektime_api.domain.notification.entity.DeviceToken;
import com.team.peektime_api.domain.notification.repository.DeviceTokenRepository;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerToken(Long userId, DeviceTokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 기존 토큰이 있으면 업데이트, 없으면 생성
        if (StringUtils.hasText(request.getDeviceId())) {
            // deviceId로 기존 토큰 찾기
            deviceTokenRepository.findByUserIdAndDeviceId(userId, request.getDeviceId())
                    .ifPresentOrElse(
                            existing -> existing.updateToken(request.getToken()),
                            () -> saveNewToken(user, request)
                    );
        } else {
            // deviceId 없으면 토큰으로 찾기
            deviceTokenRepository.findByToken(request.getToken())
                    .ifPresentOrElse(
                            existing -> log.debug("이미 등록된 토큰: {}", request.getToken()),
                            () -> saveNewToken(user, request)
                    );
        }
    }

    private void saveNewToken(User user, DeviceTokenRequest request) {
        DeviceToken deviceToken = DeviceToken.create(
                user,
                request.getToken(),
                request.getDeviceType(),
                request.getDeviceId()
        );
        deviceTokenRepository.save(deviceToken);
        log.info("새 디바이스 토큰 등록: userId={}, deviceType={}", user.getId(), request.getDeviceType());
    }

    @Transactional
    public void deleteToken(Long userId, String token) {
        deviceTokenRepository.deleteByToken(token);
        log.info("디바이스 토큰 삭제: userId={}", userId);
    }

    @Transactional(readOnly = true)
    public List<String> getTokensByUserId(Long userId) {
        return deviceTokenRepository.findByUserId(userId)
                .stream()
                .map(DeviceToken::getToken)
                .toList();
    }
}
```

---

## 6. FCM 서비스 구현

### 6-1. 알림 요청 DTO

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

### 6-2. FCM 서비스

```java
// src/main/java/com/team/peektime_api/domain/notification/service/FcmService.java

package com.team.peektime_api.domain.notification.service;

import com.google.firebase.messaging.*;
import com.team.peektime_api.domain.notification.dto.PushNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final DeviceTokenService deviceTokenService;

    /**
     * 단일 사용자에게 알림 전송
     */
    public void sendToUser(Long userId, PushNotificationRequest request) {
        List<String> tokens = deviceTokenService.getTokensByUserId(userId);

        if (tokens.isEmpty()) {
            log.warn("등록된 디바이스 토큰 없음: userId={}", userId);
            return;
        }

        sendMulticast(tokens, request);
    }

    /**
     * 여러 사용자에게 알림 전송
     */
    public void sendToUsers(List<Long> userIds, PushNotificationRequest request) {
        List<String> tokens = userIds.stream()
                .flatMap(userId -> deviceTokenService.getTokensByUserId(userId).stream())
                .toList();

        if (tokens.isEmpty()) {
            log.warn("등록된 디바이스 토큰 없음");
            return;
        }

        sendMulticast(tokens, request);
    }

    /**
     * 단일 토큰으로 전송 (테스트용)
     */
    public void sendToToken(String token, PushNotificationRequest request) {
        try {
            Message message = buildMessage(token, request);
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 전송 성공: {}", response);
        } catch (FirebaseMessagingException e) {
            handleFcmException(token, e);
        }
    }

    /**
     * 멀티캐스트 전송 (최대 500개 토큰)
     */
    private void sendMulticast(List<String> tokens, PushNotificationRequest request) {
        // FCM은 한 번에 최대 500개 토큰만 지원
        int batchSize = 500;

        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> batch = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            sendBatch(batch, request);
        }
    }

    private void sendBatch(List<String> tokens, PushNotificationRequest request) {
        MulticastMessage message = MulticastMessage.builder()
                .setNotification(buildNotification(request))
                .putAllData(request.getData() != null ? request.getData() : Map.of())
                .addAllTokens(tokens)
                // iOS 설정
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setSound("default")
                                .build())
                        .build())
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("FCM 멀티캐스트 전송: 성공={}, 실패={}",
                    response.getSuccessCount(), response.getFailureCount());

            // 실패한 토큰 처리
            handleFailedTokens(tokens, response);

        } catch (FirebaseMessagingException e) {
            log.error("FCM 멀티캐스트 전송 실패", e);
        }
    }

    private Message buildMessage(String token, PushNotificationRequest request) {
        Message.Builder builder = Message.builder()
                .setToken(token)
                .setNotification(buildNotification(request));

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

    private void handleFailedTokens(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();

        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);

            if (!sendResponse.isSuccessful()) {
                String failedToken = tokens.get(i);
                FirebaseMessagingException exception = sendResponse.getException();

                if (exception != null) {
                    handleFcmException(failedToken, exception);
                }
            }
        }
    }

    private void handleFcmException(String token, FirebaseMessagingException e) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();

        // 유효하지 않은 토큰 삭제
        if (errorCode == MessagingErrorCode.UNREGISTERED ||
            errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            log.warn("유효하지 않은 FCM 토큰 삭제: {}", token);
            // deviceTokenRepository.deleteByToken(token);  // 주석 해제하여 사용
        }

        log.error("FCM 전송 실패: errorCode={}, token={}", errorCode, token, e);
    }
}
```

---

## 7. 푸시 알림 전송 API

### 7-1. 알림 컨트롤러 (테스트/관리용)

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

    @Operation(summary = "테스트 알림 전송 (토큰 직접 지정)")
    @PostMapping("/test")
    public ResponseEntity<SuccessResponse<Void>> sendTestNotification(
            @RequestParam String token,
            @RequestParam String title,
            @RequestParam String body) {

        PushNotificationRequest request = PushNotificationRequest.builder()
                .title(title)
                .body(body)
                .build();

        fcmService.sendToToken(token, request);
        return ResponseEntity.ok(SuccessResponse.of(null));
    }
}
```

### 7-2. 비즈니스 로직에서 사용 예시

```java
// 미션 완료 시 알림 전송 예시
@Service
@RequiredArgsConstructor
public class MissionService {

    private final FcmService fcmService;

    public void completeMission(Long userId, Long missionId) {
        // ... 미션 완료 로직

        // 푸시 알림 전송
        PushNotificationRequest notification = PushNotificationRequest.builder()
                .title("미션 완료!")
                .body("오늘의 미션을 완료했습니다. 수고했어요!")
                .data(Map.of(
                        "type", "MISSION_COMPLETE",
                        "missionId", String.valueOf(missionId)
                ))
                .build();

        fcmService.sendToUser(userId, notification);
    }
}
```

---

## 8. 테스트 방법

### 8-1. FCM 토큰 얻기 (iOS)

```swift
import FirebaseMessaging

Messaging.messaging().token { token, error in
    if let token = token {
        print("FCM Token: \(token)")
        // 서버에 토큰 등록 API 호출
    }
}
```

### 8-2. Swagger에서 테스트

1. `/api/v1/device-tokens` - 토큰 등록
2. `/api/v1/notifications/test` - 테스트 알림 전송

### 8-3. cURL로 테스트

```bash
# 토큰 등록
curl -X POST http://localhost:8080/api/v1/device-tokens \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "FCM_DEVICE_TOKEN",
    "deviceType": "IOS"
  }'

# 테스트 알림 전송
curl -X POST "http://localhost:8080/api/v1/notifications/test?token=FCM_DEVICE_TOKEN&title=테스트&body=알림입니다"
```

### 8-4. Firebase Console에서 테스트

1. Firebase Console > **Cloud Messaging** 메뉴
2. **"새 알림"** 클릭
3. 알림 제목/내용 입력
4. 타겟: 단일 기기 선택 후 FCM 토큰 입력
5. 전송

---

## 체크리스트

- [ ] Firebase 프로젝트 생성
- [ ] iOS 앱 등록
- [ ] 서비스 계정 키 발급 및 저장
- [ ] .gitignore에 키 파일 제외 추가
- [ ] Firebase Admin SDK 의존성 추가
- [ ] FcmConfig 설정 클래스 구현
- [ ] DeviceToken 엔티티 및 Repository 구현
- [ ] 토큰 등록/삭제 API 구현
- [ ] FcmService 구현
- [ ] 테스트 알림 전송 확인

---

## 참고 자료

- [Firebase Admin SDK 문서](https://firebase.google.com/docs/admin/setup)
- [FCM 서버 가이드](https://firebase.google.com/docs/cloud-messaging/server)
- [FCM HTTP v1 API](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages)
