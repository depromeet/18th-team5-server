# PR 생성 및 Push

현재 브랜치의 변경사항을 분석하여 PR을 생성하고 push합니다.

## 실행 순서

1. `git branch --show-current`로 현재 브랜치 확인
2. `git log main..HEAD --oneline`로 커밋 목록 확인
3. `git diff main..HEAD --stat`로 변경된 파일 확인
4. 커밋 내용을 분석하여 PR 제목과 본문 작성
5. `gh pr create`로 PR 생성

## PR 템플릿

```markdown
## Summary
- 주요 변경사항 요약 (bullet points)

## Changes
- 변경된 파일/기능 목록

## Test plan
- [ ] 테스트 항목
```

## 규칙

- PR 제목은 커밋 메시지의 첫 번째 줄을 기반으로 작성
- Summary는 3줄 이내로 간결하게
- base 브랜치는 main
- "🤖 Generated with Claude Code" 문구는 포함하지 않음
- 이모지는 본문 내용에서만 사용 (제목에는 사용하지 않음)

## 실행

위 순서대로 PR을 생성하고 결과 URL을 반환해주세요.