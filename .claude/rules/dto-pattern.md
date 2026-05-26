# DTO 패턴

## 규칙
DTO는 **record가 아닌 class**로 작성한다.

## 구현 방법

1. `@Getter` 사용
2. Request DTO: `@Setter` 추가 (Spring 바인딩용)
3. Response DTO: `@Builder` 사용, 정적 팩토리 메서드로 변환

## Request DTO 예시

```java
@Schema(description = "선택 미션 필터 요청")
@Getter
@Setter
public class SelectedMissionRequest {

    @Schema(description = "공간 필터", example = "INDOOR")
    private SpaceType spaceType;

    @Schema(description = "카테고리 필터", example = "FOOD")
    private CategoryType categoryType;
}
```

## Response DTO 예시

```java
@Schema(description = "미션 응답")
@Getter
@Builder
public class MissionResponse {

    @Schema(description = "미션 ID")
    private Long id;

    @Schema(description = "미션 제목")
    private String title;

    public static MissionResponse from(Mission mission) {
        return MissionResponse.builder()
                .id(mission.getId())
                .title(mission.getTitle())
                .build();
    }
}
```

## 장점
- Spring 바인딩 호환성
- 일관된 코드 스타일
- 명시적인 변환 로직