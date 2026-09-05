# ADR-0011: iOS 네이티브 Apple identityToken 로그인

- 상태: Accepted
- 날짜: 2026-09-06

## 배경

iOS 앱이 identityToken과 rawNonce를 직접 전달한다. 기존 세 제공자는 인가코드 교환 방식이며,
회원 복구와 동시 가입 재시도, 자체 JWT/쿠키 정책을 유지해야 한다.

## 결정

Apple 전용 endpoint와 verifier를 두고 검증 완료된 신원을 OAuthUserInfo로 변환한다.
AuthService.loginWithVerifiedIdentity에서 기존 회원 처리를 공유한다. AppleAuthService와 공통
회원 진입점은 전체 트랜잭션으로 감싸지 않아 기존 유니크 제약 충돌 재시도를 유지한다.
Apple 공개키 검증은 자체 JwtProvider와 분리하고 기존 JJWT 라이브러리를 사용한다.
회원 식별자는 (apple, sub)이며 이메일 자동 병합이나 DB schema 변경은 하지 않는다.
응답 및 HttpOnly cookie, 자체 refresh token 회전은 기존 계약을 유지한다.

## 결과와 한계

공개키 timeout/메모리 캐시/갱신 제한을 적용한다. audience 미설정은 Apple 요청에서만 거부한다.
앱의 SHA-256(rawNonce)와 서명된 nonce를 비교한 뒤, compact identityToken 전체의 SHA-256
digest를 기존 공용 Redis에 SET NX PXAT으로 원자적으로 등록하여 exp까지 재사용을 차단한다.
만료 데이터는 Redis가 자동 삭제하며 저장소 장애 시 인증을 거부한다. 회원 처리 실패에도 소비는 유지한다.
서버 nonce challenge는 제공하지 않는다. Redis는 영속화/noeviction을 적용하고 모든 인스턴스가 공유해야 한다.
Redis 데이터 유실 및 복제 failover 시 소비 기록 유실 가능성은 운영상 한계다.
웹 로그인, Apple code/client secret/refresh/revoke는 후속 범위다.
API, 환경변수, 검증 및 후속 운영 작업은 [인증 문서](../auth.md#ios-네이티브-apple-로그인-1차)에 기록한다.
