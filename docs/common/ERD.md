# ERD (Entity Relationship Diagram)

## 엔티티 구조

### User (사용자)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | Long (PK, Auto) | 사용자 ID |
| provider | String | OAuth 제공자 (KAKAO, APPLE) |
| provider_id | String | OAuth 제공자 ID |
| nickname | String | 닉네임 |
| profile_image | String | 프로필 이미지 URL |
| created_at | LocalDateTime | 생성일시 |
| updated_at | LocalDateTime | 수정일시 |

### UserOnboarding (사용자 온보딩)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | Long (PK, Auto) | 온보딩 ID |
| user_id | Long (FK) | 사용자 ID |
| space_type | SpaceType | 공간 선호 (INDOOR/OUTDOOR) |
| intensity_type | IntensityType | 강도 선호 |
| enjoy_priorities | String | 즐기기 우선순위 (JSON) |
| user_type | UserType | 계산된 사용자 타입 |

### SolarTerm (절기)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | 절기 ID (Admin에서 동기화) |
| name | String | 절기 이름 (입춘, 우수 등) |
| year | Integer | 연도 |
| start_date | LocalDate | 시작일 |
| end_date | LocalDate | 종료일 |
| description | String | 절기 설명 |

### Mission (미션)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | 미션 ID (Admin에서 동기화) |
| title | String | 미션 제목 |
| description | Text | 미션 설명 |
| space_type | SpaceType | 공간 (INDOOR/OUTDOOR) |
| intensity_type | IntensityType | 강도 (LIGHT/MODERATE/ACTIVE) |
| category_type | CategoryType | 카테고리 |
| companion_type | CompanionType | 동반 (SOLO/TOGETHER) |
| enjoy_type | EnjoyType | 즐기기 타입 (nullable) |
| user_type | UserType | 추천 사용자 타입 (nullable) |
| deleted | Boolean | 삭제 여부 |

### DailyMission (오늘의 미션)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | 오늘의 미션 ID (Admin에서 동기화) |
| mission_id | Long (FK) | 미션 ID |
| solar_term_id | Long (FK) | 절기 ID |
| mission_date | LocalDate | 미션 날짜 (nullable = 배정대기) |
| participant_count | Integer | 참여자 수 |

### RecommendedMissionPool (추천 미션)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | 추천 미션 ID (Admin에서 동기화) |
| mission_id | Long (FK) | 미션 ID |
| solar_term_id | Long (FK) | 절기 ID |
| user_type | UserType | 대상 사용자 타입 |
| display_order | Integer | 표시 순서 |

### UserMissionCompletion (미션 완료 기록)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | Long (PK, Auto) | 완료 기록 ID |
| user_id | Long (FK) | 사용자 ID |
| mission_id | Long (FK) | 미션 ID |
| mission_type | MissionType | 미션 타입 (DAILY/RECOMMENDED/SELECTED) |
| object_key | String | S3 이미지 키 |
| memo | String | 메모 (200자) |
| completed_at | LocalDateTime | 완료 시간 |

---

## 관계도

```
User (1) ──────── (1) UserOnboarding
  │
  │ (1:N)
  ▼
UserMissionCompletion (N) ──── (1) Mission
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
                    ▼               ▼               ▼
              DailyMission   RecommendedMissionPool
                    │               │
                    └───────┬───────┘
                            ▼
                        SolarTerm
```

---

## 동기화 구조

```
[Admin DB]                    [API DB]
    │                             │
    │   POST /internal/sync/*    │
    │ ─────────────────────────► │
    │                             │
  Mission ──────────────────► Mission
  DailyMission ─────────────► DailyMission
  RecommendedMissionPool ───► RecommendedMissionPool
  SolarTerm ────────────────► SolarTerm
```

- Admin에서 생성된 데이터가 API로 동기화
- ID는 Admin ID를 그대로 사용 (PK에 @GeneratedValue 없음)
- User, UserOnboarding, UserMissionCompletion은 API에서만 생성