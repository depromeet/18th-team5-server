# 랭킹 쿼리의 선택지 지도 — 매번 집계 vs 역정규화 vs 통계 테이블, 그리고 쿼리 분해

03(커버링 인덱스), 04(역정규화 쓰기 비용)에 이어지는 정리.
"조인을 꼭 해야 했나? 따로 보내면 안 되나? 역정규화는? 별도 테이블은?"
— 전부 하나의 질문의 변형이다: **계산해야 나오는 값(mission_count)을 어디에 둘 것인가.**

---

## 0. 현재 채택안 — 조인 + 집계 + 커버링 인덱스

```sql
SELECT u.id, u.nickname,
       COUNT(c.id)        AS mission_count,
       MAX(c.created_at)  AS last_completed_at
FROM users u
JOIN user_mission_completion c ON c.user_id = u.id
WHERE u.user_type = 'EXPLORER'
  AND u.withdrawn_at IS NULL
GROUP BY u.id, u.nickname
ORDER BY mission_count DESC
LIMIT 20;
```

- 파생 데이터 없음 — 원본이 곧 진실이라 **정합성 리스크 0**
- 커버링 인덱스 설계로 2401ms → 245ms (03-results.md)
- 집계(조인 행 전체 COUNT)는 매번 수행 — 이건 어떤 인덱스로도 못 없앤다
- **어드민 통계 화면처럼 호출 빈도가 낮으면 여기서 멈추는 게 정답**

---

## 1. 문제의 본질 — mission_count는 세상 어디에도 저장돼 있지 않다

`user_mission_completion`의 행 1개 = **완료 사건 1건**이다.
"유저 7의 완료 수 = 17"이라는 값은 어떤 행에도 없고, GROUP BY로 세어봐야
비로소 존재한다. 그래서:

- **존재하지 않는 값으로는 인덱스를 만들 수 없다** → "카운트 상위 20만 읽기"가 원리적으로 불가능
- `LIMIT 20`은 집계·정렬이 **끝난 뒤에야** 적용된다 → 읽기량을 못 줄인다
- 실험에서 커버링 인덱스를 완벽히 깔고도 집계 64ms가 남은 이유가 이것

mission_count는 **유저당 1개**인 값이다. 유저당 1개인 값을 저장하려면
유저당 1행인 테이블이 필요하고, 그런 테이블은 딱 두 종류다:
이미 있는 `users`(= 역정규화), 새로 만드는 테이블(= 통계 테이블).
둘 다 싫으면 저장하지 않고 매번 계산한다(= 현재 채택안).

---

## 2. 선택지 삼각형

```
            "유저당 1개"인 mission_count를 어디에 두나?

   ① 안 둔다 (매번 집계)     ② users에 둔다 (역정규화)   ③ 전용 테이블 (통계 테이블)
   ━━━━━━━━━━━━━━━━━     ━━━━━━━━━━━━━━━━━━━━    ━━━━━━━━━━━━━━━━━━━━
   = 현재 채택안             users.mission_count        user_mission_stat
```

| | ① 매번 집계 | ② users 역정규화 | ③ 통계 테이블 |
|---|---|---|---|
| 읽기 | 245ms (집계 매번) | ~1ms (인덱스 20행) | ~1ms (인덱스 20행) |
| 쓰기 | 변화 없음 | 완료 Tx에 UPDATE 추가 | 완료 Tx에 UPDATE 추가 |
| 정합성 | 리스크 0 | 04 문서의 모든 의무 | 04 문서의 모든 의무 |
| 도메인 영향 | 없음 | **users가 쿼리 사정으로 오염** | 도메인 테이블 무변경 |
| 저장소 이동 | — | users에 묶임 | Redis ZSET 등으로 교체 용이 |
| 보정 배치 | 불필요 | 필요 | 필요 |

②와 ③의 정합성 비용은 같다(04 문서: 멱등하지 않은 증가, 쓰기 경로 전파, drift).
차이는 **그 비용을 도메인 테이블이 지느냐, 격리된 조회 전용 테이블이 지느냐**.
②가 아쉬운 이유: 랭킹 하나를 위해 모든 User 코드가 mission_count라는
"만지면 안 되는 컬럼"을 평생 들고 다닌다. ③은 통계가 틀어져도 도메인은 무사하고,
테이블째 재계산·교체·이전이 가능하다.

---

## 3. 쿼리 분해(애플리케이션 조인)는 언제 성립하는가

"조인 대신 쿼리를 2번 보내면 되지 않나?" — **첫 쿼리의 결과가 작을 때만** 성립한다.
그리고 첫 쿼리가 작으려면 정렬 기준이 **실존 컬럼**이어야 한다.

### 3-1. ✅ 성립 — ③ 통계 테이블 위에서

```sql
-- 쿼리 1: 랭킹 몸통. 조인도 집계도 없음
SELECT user_id, mission_count, last_completed_at
FROM user_mission_stat
WHERE user_type = 'EXPLORER'
ORDER BY mission_count DESC
LIMIT 20;
-- (user_type, mission_count DESC) 인덱스 → 리프 20개만 읽음, filesort 없음

-- 쿼리 2: 닉네임 붙이기. PK 룩업 20번
SELECT id, nickname
FROM users
WHERE id IN (7012, 583, 44109, /* ... 쿼리 1의 20개 */ 9921);
```

```java
// 애플리케이션 조립 — 조인의 ON 절을 Map 룩업이 대신한다
Map<Long, String> nicknames = userRepository.findAllById(ids).stream()
        .collect(toMap(User::getId, User::getNickname));
top20.stream().map(s -> response(rank++, s, nicknames.get(s.getUserId())));
```

얻는 것:
- 쿼리가 둘 다 자명해져서 **옵티마이저가 틀릴 추정 자체가 없다**
  (2401ms 사태의 뿌리였던 조인 rec_per_key 추정 괴리가 소멸)
- 두 저장소가 쿼리로 묶이지 않음 → 통계를 Redis ZSET·별도 DB로 옮겨도
  "쿼리 1의 구현체"만 바뀐다. MSA로 DB가 쪼개지면 어차피 이 패턴으로 수렴

떠안는 것 (조인이 공짜로 해주던 일):
- **필터**: `withdrawn_at IS NULL`을 SQL이 못 걸어준다 → 탈퇴 시 stat 행 삭제를
  탈퇴 경로가 기억해야 함 (04 문서 문제 ③이 하나 추가되는 것)
- **스냅샷**: 쿼리 1과 2는 다른 시점의 읽기 — 사이에 탈퇴/닉변이 끼면 미세하게 어긋남

### 3-2. ❌ 불성립 — 원본 스키마 위에서

```sql
-- 쿼리 1: 카운트가 없으니 누가 상위인지 모른다 → LIMIT 20을 붙일 곳이 없다
SELECT id FROM users WHERE user_type = 'NATURE_OUTDOOR';
-- 결과: 50,000행 전부

-- 쿼리 2: 5만 개를 IN으로 되돌려보내며 집계는 그대로 다 한다
SELECT user_id, COUNT(*) AS mission_count, MAX(created_at)
FROM user_mission_log
WHERE user_id IN (1, 51, 101, /* ... 5만 개 */ 2499951)
GROUP BY user_id
ORDER BY mission_count DESC
LIMIT 20;
```

조인이 하던 일(50만 행 집계)은 그대로인데, 5만 건 왕복 + 수 MB SQL 파싱 +
`max_allowed_packet` 리스크만 얹힌다. **DB 조인은 바로 이 왕복을 없애려고
존재한다** — 그래서 원본 스키마에서는 조인이 분해보다 싸다.

### 두 버전의 쿼리 1만 나란히 보면

```sql
ORDER BY mission_count DESC LIMIT 20   -- ③: 정렬 기준이 실존 컬럼 → 20행만 읽음
SELECT id ... WHERE user_type = ...    -- 원본: 정렬 기준이 아직 없음 → 전부 읽음
```

**LIMIT이 읽기량을 자르려면 정렬 기준이 저장돼 있어야 한다.**
쿼리 분해는 독립된 기법이 아니라, 사전 집계(③)가 끝난 뒤에만 열리는 보상이다.

---

## 4. 전환 판단과 순서

```
① 조인 + 커버링 인덱스          ← 지금 여기 (어드민 용도, 245ms면 충분)
        │
        │  유저 대면 핫패스가 되거나, 호출량 증가로 p95 기준 초과
        ▼
③ 통계 테이블 (+ 조인 유지)      ← 완료 Tx에서 함께 갱신, 읽기 인덱스 20행
        │
        │  저장소 분리·캐시 도입·더 높은 QPS
        ▼
③ + 쿼리 분해 (애플리케이션 조인) ← 조인 소멸, Redis ZSET 등 교체 여지
```

- 오른쪽으로 갈수록: 읽기는 싸지고, DB가 해주던 일(원자성·필터·스냅샷)이
  애플리케이션 책임으로 넘어온다
- ②(users 역정규화)는 ③과 비용이 같으면서 도메인만 오염시키므로 건너뛴다
- 전환 비용의 실체는 "INSERT 하나 추가"가 아니라 **쓰기 경로의 정합성 유지 의무**
  (멱등성·전파·drift 보정) — 상세는 04-denormalization-tradeoffs.md

**한 줄 요약: 집계 결과를 미리 저장해 "계산해야 나오는 값"을 "정렬 가능한
실존 컬럼"으로 바꿔두는 것이 통계 테이블의 본질이고, 쿼리 분해·저장소 교체는
전부 그 다음에 열리는 선택지다.**
