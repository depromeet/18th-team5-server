# 엔티티 생성 패턴

## 규칙
엔티티 생성 시 **정적 팩토리 메서드 패턴**을 사용한다.

## 구현 방법

1. `@Builder`는 `access = AccessLevel.PRIVATE`으로 설정
2. 생성자는 `private`으로 선언
3. `public static` 정적 팩토리 메서드를 통해서만 객체 생성
4. 정적 팩토리 메서드 내부에서 `builder()`를 호출

## 예시

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Builder(access = AccessLevel.PRIVATE)
    private Mission(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public static Mission create(String title, String description) {
        return Mission.builder()
                .title(title)
                .description(description)
                .build();
    }
}
```

## 장점
- 객체 생성 로직을 한 곳에서 관리
- 의미 있는 메서드 이름 사용 가능 (`create`, `createFrom`, `of` 등)
- 생성 시 유효성 검증 로직 추가 용이
- 외부에서 builder 직접 호출 방지