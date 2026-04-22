# 작업 진행 상황

## 완료된 작업 (Phase 1)

### 1. Enum 구조 개선
- MissionStatus: POOL → PENDING → ASSIGNED
- IntensityType: LIGHT, MODERATE, ACTIVE (3단계)
- EnjoyType 신규 (자연/야외, 제철음식, 감성콘텐츠)
- UserType 신규 (4가지 사용자 타입)
- 모든 Enum에 label, description 추가

### 2. 엔티티 수정
- Mission: enjoyType, userType 필드 추가
- RecommendedMissionPool: userType 필드 추가

### 3. 공통 응답 클래스
- peektime-admin, peektime-api 모두에 추가
- SuccessCode, ErrorCode, SuccessResponse, ErrorResponse

---

## 다음 작업 (Phase 2)

### 결정 필요 사항 ⚠️

**peektime-api → peektime-admin HTTP 호출 방식**

1. **HTTP 클라이언트 선택**
   - `RestClient`: Spring 6.1+ 기본 제공, 동기 방식, 간단함
   - `WebClient`: 비동기/리액티브, Spring WebFlux 필요
   - `OpenFeign`: 인터페이스 선언만으로 사용, 가독성 좋음

2. **장애 대응 방식**
   - `Circuit Breaker (Resilience4j)`: admin 장애 시 빠른 실패, 복구 감지
   - `Fallback`: 캐시된 데이터 반환
   - `그냥 500 전파`: 별도 처리 없이 에러 전달

### 진행할 작업
- [ ] HTTP 클라이언트 설정
- [ ] Thymeleaf 설정 추가
- [ ] Admin 페이지 컨트롤러/뷰 구현
  - 미션풀 관리
  - 오늘의 미션 배정 (드래그앤드롭)
  - 추천 미션 관리
  - 절기 목록

---

## 커밋 히스토리

```
03dfe33 feat: peektime-api 공통 응답 클래스 추가
5da073d feat: 공통 응답 클래스 추가
460254d docs: 미션 아키텍처 설계 문서 추가
58bec8d feat: MissionType에 SELECTED 추가
8f96c4a refactor: peektime-api Enum 동기화
3b17ce7 feat: LLM 프롬프트 및 DTO에 신규 필드 반영
6ae1657 feat: Mission, RecommendedMissionPool 엔티티 필드 추가
cd2078d refactor: Enum 구조 개선 및 신규 Enum 추가
7e9cd54 chore: 자동 커밋 규칙 추가
be8127a docs: PeekTime 서비스 기획 문서 추가
```