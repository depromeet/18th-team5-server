# Enum 정의

## SpaceType (공간)
| 값 | label | 설명 |
|------|------|------|
| `INDOOR` | 실내 | 집이나 실내 공간에서 수행 |
| `OUTDOOR` | 실외 | 밖에서 수행 |

---

## IntensityType (강도)
| 값 | label | 설명 |
|------|------|------|
| `LIGHT` | 가벼운 | 이동 없음, 5분 이내 |
| `MODERATE` | 보통 | 동네 범위, 30분 이내 |
| `ACTIVE` | 적극적 | 이동 필요, 1시간 이상 |

---

## CategoryType (카테고리)
| 값 | label | 설명 |
|------|------|------|
| `FOOD` | 음식 | 제철 음식, 요리 관련 미션 |
| `NATURE` | 자연 | 자연 감상, 야외 활동 미션 |
| `RECORD` | 기록 | 사진, 일기 등 기록 미션 |
| `PLACE` | 장소 | 특정 장소 방문 미션 |
| `SENSE` | 감각 | 오감을 활용한 미션 |

---

## CompanionType (동반)
| 값 | label | 설명 |
|------|------|------|
| `SOLO` | 혼자 가능 | 혼자서도 충분히 즐길 수 있는 미션 |
| `TOGETHER` | 같이하면 더 좋음 | 함께하면 더 재미있는 미션 |

---

## EnjoyType (즐기기 타입)
| 값 | label | 설명 |
|------|------|------|
| `NATURE_OUTDOOR` | 자연/야외 활동 | 자연이나 야외에서 즐기는 활동 |
| `SEASONAL_FOOD` | 제철 음식/요리 | 제철 음식을 맛보거나 요리하는 활동 |
| `CULTURE_CONTENT` | 감성 콘텐츠/문화 | 감성적인 콘텐츠나 문화 활동 |

---

## UserType (사용자 타입)
| 값 | label | 설명 | 조건 |
|------|------|------|------|
| `EXPLORER` | 제철을 쫓는 탐험가 | 밖에서 적극적으로 활동하는 타입 | OUTDOOR + ACTIVE |
| `WALKER` | 일상 속 제철 산책가 | 밖에서 가볍게 활동하는 타입 | OUTDOOR + LIGHT |
| `LIFE_CREATOR` | 제철을 채우는 라이프 크리에이터 | 실내에서 적극적으로 활동하는 타입 | INDOOR + ACTIVE |
| `AESTHETE` | 제철을 음미하는 감상가 | 실내에서 가볍게 활동하는 타입 | INDOOR + LIGHT |

---

## MissionType (미션 타입)
| 값 | label | 설명 |
|------|------|------|
| `DAILY` | 오늘의 미션 | 모든 사용자에게 동일하게 제공되는 미션 |
| `RECOMMENDED` | 추천 미션 | 사용자 타입별로 제공되는 맞춤 미션 |
| `SELECTED` | 선택 미션 | 사용자가 직접 필터링해서 선택하는 미션 |

---

## 사용처

| Enum | 사용 엔티티 |
|------|------------|
| SpaceType | Mission, UserOnboarding |
| IntensityType | Mission, UserOnboarding |
| CategoryType | Mission |
| CompanionType | Mission |
| EnjoyType | Mission, RecommendedMissionPool |
| UserType | Mission, RecommendedMissionPool, UserOnboarding |
| MissionType | UserMissionCompletion |