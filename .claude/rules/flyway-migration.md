# Flyway 마이그레이션 규칙

## 적용 대상
- peektime-admin 모듈

## 규칙
DB 스키마 변경 시 **반드시 Flyway 마이그레이션 스크립트를 추가**한다.

## 마이그레이션 파일 위치
```
peektime-admin/src/main/resources/db/migration/
```

## 파일 명명 규칙
```
V{버전}__{설명}.sql
```

예시:
- `V1__create_user_table.sql`
- `V2__add_email_column.sql`
- `V3__recreate_mission_log.sql`

## 버전 규칙
- 기존 파일의 가장 높은 버전 + 1
- 버전은 정수 (V1, V2, V3...)

## 스크립트 작성 시 주의사항

### 테이블 생성
```sql
CREATE TABLE table_name (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    column_name VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);
```

### 테이블 삭제 후 재생성
```sql
DROP TABLE IF EXISTS table_name;
CREATE TABLE table_name (...);
```

### 컬럼 추가
```sql
ALTER TABLE table_name ADD COLUMN column_name VARCHAR(255);
```

### 컬럼 삭제
```sql
ALTER TABLE table_name DROP COLUMN column_name;
```

### 인덱스 추가
```sql
CREATE INDEX idx_column ON table_name(column_name);
```

## 엔티티 변경 시 체크리스트
1. [ ] 엔티티 필드 변경
2. [ ] 마이그레이션 SQL 작성
3. [ ] 버전 번호 확인 (중복 없는지)
4. [ ] 로컬에서 테스트