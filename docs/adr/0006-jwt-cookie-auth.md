# 0006. JWT 기반 쿠키 인증과 리프레시 토큰 회전

- 상태: Accepted
- 날짜: 2026-07-08

## 배경 (Context)

소셜 로그인(OAuth)은 구현되어 있었지만 로그인 이후 앱 자체 인증 수단이 없었다 —
`AuthService.login`은 사용자 식별 결과만 반환할 뿐 세션/토큰을 발급하지 않았고,
`SecurityConfig`는 `anyRequest().permitAll()`로 완전히 열려 있었다. 로그아웃, 토큰
재발급(reissue), 토큰 회전을 구현하려면 먼저 액세스/리프레시 토큰 발급·저장·검증
체계를 세워야 했다.

## 결정 (Decision)

- 로그인 성공 시 액세스 토큰(단명, 30분)과 리프레시 토큰(장기, 14일)을 각각
  JWT로 발급하고, 둘 다 **HttpOnly 쿠키**(`ACCESS_TOKEN`, `REFRESH_TOKEN`)로 내려준다.
  응답 바디에는 토큰을 포함하지 않는다 — XSS로 인한 토큰 탈취 표면을 줄이기 위함.
- 리프레시 토큰은 **DB(JPA `RefreshToken` 엔티티)에 저장**한다. 이 시점에 Redis는
  `RedisConfig`가 빈 스텁이고 의존성도 없는 미구성 상태([관련 CLAUDE.md 기록](../../CLAUDE.md))라,
  TTL 자동 만료의 이점보다 새 인프라를 끌어오는 비용이 더 크다고 판단했다.
- 리프레시 토큰은 **회전(rotation)** 방식으로 운용한다: `/api/v1/auth/reissue` 호출마다
  제시된 토큰을 즉시 `revoked=true`로 폐기하고 새 access/refresh 쌍을 발급한다.
  이미 폐기된 토큰이 다시 제시되면(재사용) 탈취로 간주해 해당 유저의 모든 리프레시
  토큰을 무효화하고 401을 반환한다 — 사용자는 재로그인해야 한다.
- `RefreshToken` 조회 키는 JWT의 `jti` 클레임(`tokenId`, UUID)이다. 토큰 원문이 아니라
  jti만 저장/조회하므로 DB에 서명된 JWT 전체를 들고 있을 필요가 없다.
- `JwtAuthenticationFilter`는 `ACCESS_TOKEN` 쿠키가 유효하면 `SecurityContext`를
  채우지만, 인가 정책(`anyRequest().permitAll()`) 자체는 이번 결정 범위 밖이라
  건드리지 않았다 — 현재 도메인 컨트롤러가 `HealthController`/`AuthController`뿐이라
  잠글 대상이 아직 없다.

## 결과 (Consequences)

- 새 의존성: `io.jsonwebtoken:jjwt-*` (JWT 서명/검증). Redis는 여전히 미도입.
- `jwt.secret`은 `application-dev.yml`에만 기본값이 있고 `application-prod.yml`은
  필수 env 값이다 — datasource와 동일한 패턴([ADR-0005](0005-schema-management.md) 참고).
- 프론트엔드는 `credentials: 'include'`로 요청해야 쿠키가 오간다. CORS는 이미
  `allowCredentials(true)` + 명시적 origin 목록으로 구성되어 있어 추가 변경이 없었다.
- `RefreshToken` row는 회전/로그아웃 시 물리 삭제하지 않고 `revoked` 플래그만 세운다.
  재사용 탐지 로그·감사 목적상 남겨두며, 만료된 row 정리(배치/스케줄러)는 아직 없다 —
  필요해지면 별도로 추가한다.
- 액세스 토큰이 만료된 상태로 `/reissue`를 호출해도 리프레시 토큰만 유효하면 재발급된다
  (의도된 동작). 리프레시 토큰까지 만료/폐기되면 재로그인이 필요하다.

## 고려한 대안 (Alternatives)

- Redis에 리프레시 토큰 저장 — TTL 자동 만료가 편하지만, 이 저장소 목적을 위해서만
  새 인프라 의존성과 로컬 Redis 실행 환경을 추가하는 건 현재 단계에 과했다. 기각(추후
  트래픽/만료 정리 요구가 커지면 재검토).
- 액세스 토큰을 응답 바디로, 리프레시 토큰만 쿠키로 — 프론트가 헤더 기반 인증 로직을
  이미 갖췄다면 유리하지만, 이번 요청은 "JWT 기반 쿠키 인증"을 명시적으로 요구했고
  프론트엔드 쪽 기존 구현이 확인되지 않아 기각.
- 리프레시 토큰을 회전 없이 만료까지 재사용 — 구현은 단순하지만 탈취 시 만료 전까지
  무제한 재사용이 가능해 회전 대비 보안이 약함. 기각.
