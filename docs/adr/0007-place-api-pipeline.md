# ADR-0007: Place 데이터 파이프라인 및 탐색 API 구현

**상태:** 구현 완료  
**날짜:** 2026-07-14

---

## 개요

한국관광공사 TarRlteTarService1 API에서 제주도 관광지 데이터를 로컬 DB에 동기화하고, 카테고리별 탐색·인기 장소·세부정보 API를 제공한다.

---

## 아키텍처: 2단계 분리 구조

```
[TarRlteTarService1 API]
        ↓ 새벽 3시 스케줄 / 기동 시 1회
  PlaceSyncService  →  place 테이블 (PostGIS DB)
                              ↓ 캐시 미스 시
                        Redis Cache (TTL 30분)
                              ↓
[클라이언트]  →  PlaceController  →  PlaceQueryService  →  Redis or DB
```

**적재**와 **조회**를 완전히 분리한다. 클라이언트 요청은 외부 API와 무관하게 항상 로컬 DB(또는 Redis 캐시)에서 응답한다.

이 구조를 선택한 이유:
- TarRlteTarService1은 월별 집계 API로 실시간 호출에 적합하지 않음
- 공공 API 일일 호출 한도 초과 방지
- TourAPI 장애 시에도 서비스 정상 운영

---

## 외부 API: TarRlteTarService1

**API명:** 관광지별 연관관광지 정보 서비스  
**제공기관:** 한국관광공사  
**Base URL:** `http://apis.data.go.kr/B551011/TarRlteTarService1`

### 사용 엔드포인트

| 엔드포인트 | 설명 | 주요 파라미터 |
|---|---|---|
| `/areaBasedList1` | 시군구 기반 연관관광지 목록 | `areaCd=50`, `signguCd`, `baseYm` |
| `/searchKeyword1` | 키워드 기반 연관관광지 목록 | `areaCd=50`, `keyword`, `baseYm` |

### 주요 파라미터 값

| 코드 | 값 | 설명 |
|---|---|---|
| `areaCd` | `50` | 제주특별자치도 지역 코드 |
| `signguCd` (제주시) | `50110` | 제주시 시군구 코드 |
| `signguCd` (서귀포시) | `50130` | 서귀포시 시군구 코드 |
| `baseYm` | `yyyyMM` 형식, **전월 기준** | 당월 데이터는 미제공 (월별 집계) |

### 알려진 제약

- **좌표 미제공**: 연관관광지(`rlteTatsCd`) 항목에 위·경도가 없어 시군구 중심 좌표로 대체
  - 제주시 중심: `lat=33.4996, lon=126.5312`
  - 서귀포시 중심: `lat=33.2541, lon=126.5600`
- **당월 데이터 없음**: `baseYm`을 항상 전월(`YearMonth.now().minusMonths(1)`)로 설정
- **HTTP/1.1 필수**: Spring 7의 `RestClient` 기본 HTTP 클라이언트(Java `HttpClient`)가 해당 API 서버와 502를 발생시킴 → `SimpleClientHttpRequestFactory` 적용으로 해결

---

## 구현 구조

### 패키지 레이아웃

```
domain/place/
├── controller/
│   ├── PlaceController.java
│   └── docs/PlaceControllerDocs.java
├── converter/PlaceConverter.java
├── dto/
│   ├── PlaceSummaryDto.java
│   ├── PlaceDetailDto.java        (implements Serializable — Redis 직렬화)
│   └── PopularPlaceDto.java       (implements Serializable)
├── entity/Place.java              (published 필드 포함)
├── exception/PlaceErrorCode.java
├── repository/
│   ├── PlaceRepository.java
│   ├── CategoryRepository.java
│   ├── PopularPlaceRepository.java
│   └── PlaceCongestionRepository.java
└── service/
    ├── PlaceQueryService.java
    └── PlaceSyncService.java

global/
├── external/
│   ├── config/ExternalApiProperties.java
│   ├── tourapi/
│   │   ├── TourApiClient.java
│   │   ├── dto/TourListItem.java
│   │   └── dto/TourApiResponse.java
│   └── congestion/
│       ├── CongestionClient.java
│       └── dto/CongestionItem.java
├── init/DataInitializer.java
└── scheduler/PlaceDataSyncScheduler.java
```

### 데이터 흐름 (적재)

```
PlaceDataSyncScheduler.syncAll()
  → PlaceSyncService.syncAllCategories()
      → [제주시, 서귀포시] 순서로 순회
      → TourApiClient.getAreaBased(signguCd, 1, 100)
          → GET /areaBasedList1?areaCd=50&signguCd={코드}&baseYm={전월}
      → rlteTatsCd 기준 중복 체크 (existsByExternalId)
      → 신규 항목만 Place 저장 + PopularPlace 생성 (rlteRank → visitCount 변환)
```

**rlteRank → visitCount 변환 공식:** `Math.max(1, 51 - rlteRank) * 10`  
→ rank 1 = 500점, rank 50 = 10점

### 데이터 흐름 (조회)

```
GET /api/places/popular?limit=N
  → PlaceQueryService.getPopular(N)
      → [Redis 캐시 히트] popularPlaces::{N} → 즉시 반환
      → [캐시 미스] PopularPlaceRepository.findAllByOrderByVisitCountDesc(PageRequest.of(0, N))
                   → Redis에 저장 (TTL 30분)

GET /api/places/{id}
  → PlaceQueryService.getDetail(id)
      → [Redis 캐시 히트] placeDetail::{id} → 즉시 반환
      → [캐시 미스] PlaceRepository.findById(id) → Redis에 저장 (TTL 30분)

GET /api/places?categoryName=자연&page=0&size=20
  → PlaceQueryService.browse(categoryName, pageable)
      → categoryName 없음: PlaceRepository.findAll(pageable)
      → categoryName 있음: PlaceRepository.findByCategoryNameAndPublishedTrue(categoryName, pageable)
```

---

## API 명세

**Base path:** `/api/places`  
**공통 응답 형식:** `{"isSuccess": true, "code": "FOUND200", "message": "...", "result": ...}`

| 메서드 | 경로 | 설명 | 주요 파라미터 |
|---|---|---|---|
| GET | `/api/places` | 카테고리별 장소 목록 (페이지네이션) | `categoryName` (선택), `page=0`, `size=20` |
| GET | `/api/places/popular` | 인기 장소 목록 | `limit=20` (홈 화면: 3) |
| GET | `/api/places/{id}` | 장소 세부정보 | `id` (Path Variable) |

**에러 코드:**

| 상황 | HTTP | code |
|---|---|---|
| 존재하지 않는 장소 ID | 404 | `PLACE404_1` |

**현재 미제공 필드 (TarRlteTarService1 한계):**
- `PlaceDetailDto.homepage` → 항상 `null`
- `PlaceDetailDto.tel` → 항상 `null`
- `PlaceDetailDto.images` → 항상 빈 배열 (추후 PlaceImage 테이블로 확장 예정)

---

## 카테고리 초기화

앱 기동 시 `DataInitializer`가 아래 8개 카테고리를 DB에 삽입한다 (이미 있으면 건너뜀).

| 카테고리명 | 설명 |
|---|---|
| 자연 | 자연 관광지 (산, 해변, 오름 등) |
| 음식 | 제주 맛집 및 음식점 |
| 카페 | 카페 및 디저트 |
| 전통시장 | 전통시장 및 로컬 마켓 |
| 역사 | 역사·문화시설 (박물관, 유적지 등) |
| 체험 | 레저·체험 활동 |
| 쇼핑 | 쇼핑 명소 |
| 사진명소 | 포토스팟 및 뷰포인트 |

**TourAPI 카테고리 매핑:** `rlteCtgryLclsNm` 기준으로 내부 카테고리로 변환

| TourAPI 값 | 내부 카테고리 |
|---|---|
| `관광지` | 자연 |
| `음식` | 음식 |
| 그 외 | 자연 (기본값) |

---

## 동기화 스케줄러

```java
// 매일 새벽 3시 자동 실행
@Scheduled(cron = "0 0 3 * * *")

// 앱 기동 시 즉시 실행 (application-dev.yml에서 제어)
@Value("${app.sync.run-on-startup:false}")
```

**기동 시 동기화 활성화:**
```yaml
# application-dev.yml
app:
  sync:
    run-on-startup: true
```

**동기화 확인 로그:**
```
앱 기동 시 장소 데이터 동기화 실행
TourAPI 동기화: signguCd=50110, 결과수=100
TourAPI 동기화: signguCd=50130, 결과수=100
장소 데이터 동기화 완료
```

---

## 테스트 구조

### 단위 테스트 (Mockito)

| 파일 | 검증 내용 |
|---|---|
| `PlaceSyncServiceTest` | 신규 장소 저장, 중복 `externalId` 스킵 |
| `PlaceQueryServiceTest` | `getPopular` 결과 반환, `getDetail` 404 예외, `getDetail` 정상 조회 |
| `PlaceDataSyncSchedulerTest` | `syncAll()` 호출 시 `PlaceSyncService.syncAllCategories()` 위임 |
| `CongestionClientTest` | MockRestServiceServer로 HTTP 응답 파싱 검증 |

### 슬라이스 테스트

| 파일 | 검증 내용 |
|---|---|
| `PlaceRepositoryTest` | `findByExternalId`, `findByCategoryNameAndPublishedTrue` (published=true 필터) |
| `PlaceControllerTest` | `@WebMvcTest` — HTTP 응답 상태코드 및 JSON 엔벨로프 형식 |

### 테스트 실행

```bash
# 전체 빌드 + 테스트 (PostGIS Docker 필요)
docker compose up -d
./gradlew build

# 개별 테스트
./gradlew test --tests "*.PlaceSyncServiceTest"
./gradlew test --tests "*.PlaceQueryServiceTest"
./gradlew test --tests "*.PlaceRepositoryTest"
./gradlew test --tests "*.PlaceControllerTest"
```

**주의:** `PlaceRepositoryTest`는 실제 PostGIS DB를 사용한다. `docker compose up -d` 없이 실행하면 실패한다.

### 로컬 통합 검증 (curl)

```bash
# 헬스체크
curl http://localhost:8080/health
# → ok

# 인기 장소 (동기화 전: 빈 배열)
curl "http://localhost:8080/api/places/popular?limit=5"
# → {"isSuccess":true,"code":"FOUND200","result":[...]}

# 카테고리 목록 (한글은 URL 인코딩 필요)
curl "http://localhost:8080/api/places?categoryName=%EC%9E%90%EC%97%B0&page=0&size=5"
# → {"isSuccess":true,...,"result":{"content":[...],"totalElements":N}}

# 장소 세부정보
curl "http://localhost:8080/api/places/1"
# → {"isSuccess":true,...,"result":{"id":1,"name":"...",...}}

# 존재하지 않는 장소 (에러 확인)
curl "http://localhost:8080/api/places/999"
# → {"isSuccess":false,"code":"PLACE404_1","message":"존재하지 않는 관광지입니다."}

# Swagger 경로 확인
curl http://localhost:8080/v3/api-docs | grep "/api/places"
```

### DB 직접 확인

```bash
# 적재된 장소 수 확인
docker compose exec db psql -U postgres -d jejugilmoa \
  -c "SELECT COUNT(*) FROM place;"

# 샘플 데이터 확인
docker compose exec db psql -U postgres -d jejugilmoa \
  -c "SELECT name, address, category_id FROM place LIMIT 5;"

# 인기 장소 랭킹 확인
docker compose exec db psql -U postgres -d jejugilmoa \
  -c "SELECT p.name, pp.visit_count
      FROM popular_place pp JOIN place p ON p.id = pp.place_id
      ORDER BY pp.visit_count DESC LIMIT 5;"

# Redis 캐시 키 확인 (동기화 + 조회 후)
docker compose exec redis redis-cli KEYS "*"
```

---

## Plan B (다음 단계)

`place` 테이블 데이터가 채워진 후 구현 가능한 기능:

| 기능 | API | 설명 |
|---|---|---|
| 연관 관광지 | `GET /api/places/{id}/related` | `tAtsCd` 기준 연관 목록 |
| 추천 여행지 | `GET /api/recommendations/places` | 비회원: 인기순, 회원: 선호도 기반 |
| 추천 코스 | `GET /api/recommendations/courses` | 두루누비 API 연동 |
| 거리 계산 | `POST /api/distance` | PostGIS `ST_Distance` |
