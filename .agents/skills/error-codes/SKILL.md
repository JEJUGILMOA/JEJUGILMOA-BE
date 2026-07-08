---
name: error-codes
description: Add or modify API success/error codes in the BaseCode system. Use when a new failure mode needs a code, or when deciding between reusing a general code and creating a domain code.
---

# Error/success code system

Full rationale: `docs/adr/0001-api-response-envelope.md`.

## Decision rule

- Failure is **domain-specific** (plan not found, invalid date range, duplicate favorite…)
  → add to `domain/<x>/exception/<X>ErrorCode` (create the enum if absent).
- Failure is **framework/transport-level** (malformed JSON, type mismatch…) → already handled
  generically by `GeneralExceptionAdvice`; do not add codes for these.
- Never repurpose `GeneralErrorCode.COMMON...` entries for domain meaning.

## Format

```java
@Getter
@AllArgsConstructor
public enum PlaceErrorCode implements BaseCode {
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE404_1", "장소를 찾을 수 없습니다."),
    PLACE_NOT_PUBLISHED(HttpStatus.FORBIDDEN, "PLACE403_1", "비공개 처리된 장소입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

- Code string: `<DOMAIN><HTTP_STATUS>_<serial>` — serial increments per domain+status
  (`PLACE404_1`, `PLACE404_2`, …). Check the enum for the next free serial.
- Message: Korean, user-displayable (the frontend may render it verbatim).
- The `HttpStatus` drives the actual response status via `GeneralExceptionAdvice`.

## Usage

```java
throw new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND);
```

Success codes: reuse `GeneralSuccessCode` (`REQUEST_OK`, `CREATED`); domain-specific success
codes are rarely justified.

## Verification

Curl the failing path and assert the envelope:

```json
{"isSuccess": false, "code": "PLACE404_1", "message": "장소를 찾을 수 없습니다.", "result": null}
```

with HTTP status 404.
