# 컨벤션

코드/커밋/브랜치 규칙. 아키텍처 규칙은 [architecture.md](architecture.md) 참고.

## 커밋 메시지

```
[접두사] 한국어 요약 (명사형 종결)

(선택) 본문: 왜 이 변경이 필요한지
```

| 접두사 | 용도 |
|---|---|
| `[Feat]` | 기능 추가 |
| `[Fix]` | 버그 수정 |
| `[Refactor]` | 동작 변경 없는 구조 개선 |
| `[Test]` | 테스트 추가/수정 |
| `[Docs]` | 문서 |
| `[Chore]` | 빌드, 설정, 의존성 등 |

예: `[Feat] 여행 계획 생성 API 구현`, `[Fix] 프로젝트 기본 엔티티 수정`

## 브랜치

- `main`: 항상 배포 가능한 상태 유지. 직접 푸시 금지, PR로만 병합.
- 작업 브랜치: `feat/plan-create-api`, `fix/place-geom-sync` 형식 (`접두사/케밥-요약`).

## Java 코드 스타일

- 들여쓰기 4칸, 한 줄 최대 120자 (`.editorconfig` 참고)
- **엔티티**: `@Getter @Builder @NoArgsConstructor(PROTECTED) @AllArgsConstructor(PRIVATE)`,
  setter 금지, 상태 변경은 의도가 드러나는 메서드로 (`Report.approve()` 참고)
- **DTO**: Java `record` 사용. 요청 DTO에 `jakarta.validation` 애노테이션으로 검증
- **enum 영속화**: 항상 `@Enumerated(EnumType.STRING)`
- **주석**: 한국어. "무엇"이 아니라 코드로 표현 안 되는 "왜/제약"만 기록
  (예: `// 반경 검색에 사용: ST_DWithin()`)
- **필드명 vs 컬럼명**: boolean 필드는 `published` ↔ `is_published`처럼
  자바 필드에 `is` 접두사를 빼고 `@Column(name = "is_...")`으로 매핑

## 네이밍

| 대상 | 규칙 | 예 |
|---|---|---|
| 테이블 | snake_case 단수 | `travel_plan` |
| 인덱스 | `idx_<테이블약칭>_<컬럼들>` | `idx_plan_user_status` |
| 유니크 제약 | `uk_<테이블약칭>_<컬럼들>` | `uk_course_plan_sequence` |
| 에러 코드 | `<도메인><HTTP상태>_<일련번호>` | `PLAN404_1`, `AUTH401_1` |
| DTO | `<도메인><동작>Request/Response` | `TravelPlanCreateRequest` |

## 에러 코드 추가 규칙

1. 도메인 패키지에 `<도메인>ErrorCode implements BaseCode` enum 생성 (`GeneralErrorCode` 참고)
2. `HttpStatus` + 코드 문자열 + 한국어 메시지 3요소
3. 범용 코드(`COMMON...`)에 도메인 의미를 욱여넣지 말 것 — 도메인 코드를 새로 만든다

## 인덱스

새 조회 패턴을 추가할 때 해당 엔티티의 `@Table(indexes = ...)`에 명시적으로 선언한다.
(기존 예: `TravelPlan`의 `user_id,status`, `TravelRecord`의 `user_id,visibility`)

## PR

- PR 템플릿(`.github/PULL_REQUEST_TEMPLATE.md`)의 검증/체크리스트를 채운다
- 엔티티 변경 시 [architecture.md](architecture.md)의 ERD 갱신
- 아키텍처 결정 포함 시 [ADR](adr/) 작성
