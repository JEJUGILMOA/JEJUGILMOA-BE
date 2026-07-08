# 0001. API 응답 봉투와 중앙집중 예외 처리

- 상태: Accepted
- 날짜: 2026-07-07 (기존 코드에 구현된 결정을 소급 기록)

## 배경 (Context)

프론트엔드가 성공/실패를 일관된 형태로 파싱할 수 있어야 하고, 에러 응답 형식이
컨트롤러마다 달라지는 것을 막아야 한다. 검증 실패·타입 불일치·DB 제약 위반 같은
프레임워크 예외도 같은 형식으로 내려가야 한다.

## 결정 (Decision)

- 모든 API 응답은 `ApiResponse<T>` 봉투(`{isSuccess, code, message, result}`)를 사용한다.
- 상태/코드/메시지는 `BaseCode` 인터페이스를 구현한 enum으로만 정의한다
  (`GeneralSuccessCode`, `GeneralErrorCode`, 도메인별 `<도메인>ErrorCode`).
- 비즈니스 실패는 `GeneralException(BaseCode)`을 던지고, 변환은 전부
  `GeneralExceptionAdvice`(`@RestControllerAdvice`)에서 한다. 컨트롤러 try-catch 금지.
- 에러 코드 문자열은 `<도메인><HTTP상태>_<일련번호>` 형식 (예: `AUTH401_1`, `COMMON404_1`).

## 결과 (Consequences)

- 프론트는 `code` 문자열로 분기 가능하고, HTTP 상태와 별개로 세분화된 실패 사유를 안다.
- 새 예외 유형이 생기면 advice에 핸들러를 추가해야 한다 (분산 처리 금지).
- 코드 enum이 늘어나는 비용은 감수한다 — 범용 코드 재사용으로 의미를 흐리는 것보다 낫다.

## 고려한 대안 (Alternatives)

- RFC 7807 (Problem Details) — 표준이지만 성공 응답까지 감싸는 일관 봉투가 필요했고,
  한국어 메시지 + 커스텀 코드 체계가 프론트 요구사항이었다.
- 컨트롤러별 ResponseEntity 직접 구성 — 형식 드리프트가 필연적이라 기각.
