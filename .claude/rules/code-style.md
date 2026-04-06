---
paths:
  - "src/**/*.java"
---

# 코딩 컨벤션

## Naming
- 기본적으로 네이밍은 **누구나 알 수 있는 쉬운 단어**를 선택한다
  - 쓸데없이 어려운 고급 어휘를 피한다
- 변수/메서드: camelCase (userEmail, userCellPhone)
- 클래스: PascalCase
- 상수: UPPER_SNAKE_CASE
- 패키지: 단어가 달라지더라도 무조건 소문자 (frontend, useremail)

## Code
- 하나의 메서드 길이 15줄, 깊이(depth) 3 이내로 작성
  - 단, 가독성이 현저히 떨어진다면 코드를 좀 더 풀어쓴다
- 매개변수 타입이 메서드 시그니처에서 이미 드러나면, 메서드명에 타입을 중복해서 사용하지 않음
  - `createProjectByProjectRequest(ProjectCreateRequest request)` (X)
  - `createProject(ProjectCreateRequest request)` (O)
  - 단, 서로 다른 타입을 구분해야 할 때는 사용 (findUserByEmail, findUserByUsername)
- API 응답: ResponseEntity 사용
- 예외처리: @RestControllerAdvice 활용
- DTO: record 클래스 권장

## Entity
- id 자동 생성 전략은 IDENTITY를 사용
- @NoArgsConstructor 사용 시 access를 PROTECTED로 제한

## Service
- Service 파일이 비즈니스 로직 5개 이상으로 커지면 조회/비조회(Transactional)로 클래스 분리
- 도메인 서비스 네이밍을 피한다
  - `UserService` (X) → 수많은 책임을 떠안은 큰 클래스로 발전될 가능성 높음
  - `UserRegisterService`, `UserEmailService` (O) → 기능별로 세분화