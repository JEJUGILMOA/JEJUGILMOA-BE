# 한국관광공사 TourAPI — 관광지별 연관관광지 정보 서비스 매뉴얼

> **문서 버전:** v4.1  
> **최종 수정:** 2025-05-23

---

## 개정 이력

| 버전 | 변경일 | 변경 내용 |
|------|--------|-----------|
| 4.0 | 2024-09-03 | 최초 작성 |
| 4.1 | 2025-05-23 | 목록 오퍼레이션(지역기반, 키워드) 응답항목 변경 — 연관관광지기본주소 삭제, 관광지코드/연관관광지코드 추가 |

---

## I. 서비스 개요

### TourAPI 소개

한국관광공사는 국가정보자원의 개방 및 공유 정책에 따라 다양한 OpenAPI 서비스를 제공합니다.

- 국문 및 다국어(영문, 일문, 중문간체, 중문번체, 독문, 불문, 서문, 노문) 서비스 (9종)
- 국문 무장애 여행 정보 서비스
- 생태관광정보 서비스
- 관광사진갤러리 서비스
- 고캠핑정보 서비스
- 관광지 오디오 가이드정보 서비스
- 관광 빅데이터 정보 서비스
- 두루누비 정보 서비스
- 외래객 관광 정보 서비스
- 관광 채용 정보 서비스
- 관광지 집중률 방문자 추이 예측 정보
- 기초지자체 중심 관광지 정보
- **관광지별 연관 관광지 정보** ← 본 서비스
- 반려동물 동반여행 서비스

### 활용 홈페이지

- **TourAPI4.0 활용 사이트:** https://data.go.kr/

---

## II. 인증키 활용 및 API 호출 방법

- 개발계정은 일 **1,000건** 트래픽 제공
- 개발계정은 자동승인으로, 활용 신청 후 약 **30분** 후 사용 가능 (공공데이터포털 ↔ 한국관광공사 동기화)

### REST 방식 URL 요청 예시

응답 표준은 **XML**이며, JSON 응답을 원할 경우 `&_type=json`을 추가합니다.

```
http://apis.data.go.kr/B551011/TarRlteTarService1/areaBasedList1?serviceKey=ServiceKey&numOfRows=10&pageNo=1&MobileOS=ETC&MobileApp=TestApp&_type=json
```

### 서비스키(인증키) 인코딩 방법

- **2015년 1월 이전** 발급 인증키: URL 인코딩 필요
  ```java
  String myKey = "발급 받은 인증키";
  String serviceKey = URLEncoder.encode(myKey, "UTF-8");
  ```
  > TourAPI의 모든 Character Set은 UTF-8

- **2015년 1월 이후** 발급 인증키: 인코딩 불필요

### 요청 파라미터에 서비스명 기재

`MobileApp` 파라미터는 서비스(웹/앱 등)별 활용 통계 산출을 위한 필수 항목입니다. URL 요청 시 반드시 기재하세요.

```
http://apis.data.go.kr/B551011/TarRlteTarService1/areaBasedList1?serviceKey=ServiceKey&numOfRows=10&pageNo=1&MobileOS=AND&MobileApp=appName
```

---

## III. API 서비스 명세

### 서비스 개요

| 항목 | 내용 |
|------|------|
| 서비스 ID | TarRlteTarService1 |
| 서비스명 | 한국관광공사 관광지별 연관 관광지 정보 서비스 |
| 서비스 설명 | 선택한 관광지와 높은 연결성을 가지는 연관관광지를 전체·관광지·음식·숙박 유형별 최대 각 50위까지 목록 조회 |
| 인터페이스 | REST (GET) |
| 응답 형식 | XML, JSON |
| 보안 | 서비스 Key 인증, HTTPS/HTTP |
| 서비스 URL | `http://apis.data.go.kr/B551011/TarRlteTarService1` |
| 데이터 갱신 주기 | 월 1회 (매월 8일) |
| 서비스 제공자 | 개방데이터운영팀 / 디지털콘텐츠팀 `tourapi@knto.or.kr` / 070-4287-3219 |

### 오퍼레이션 목록

| 번호 | 오퍼레이션 | 설명 |
|------|-----------|------|
| 1 | `areaBasedList1` | 지역기반 관광지별 연관관광지 정보 조회 |
| 2 | `searchKeyword1` | 키워드검색 관광지별 연관관광지 정보 조회 |

---

### 에러 코드

#### 공공데이터포털 에러코드

```xml
<OpenAPI_ServiceResponse>
  <cmmMsgHeader>
    <errMsg>SERVICE ERROR</errMsg>
    <returnAuthMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</returnAuthMsg>
    <returnReasonCode>30</returnReasonCode>
  </cmmMsgHeader>
</OpenAPI_ServiceResponse>
```

| 에러코드 | 에러메시지 | 설명 |
|---------|-----------|------|
| 01 | APPLICATION_ERROR | 어플리케이션 에러 |
| 04 | HTTP_ERROR | HTTP 에러 |
| 12 | NO_OPENAPI_SERVICE_ERROR | 해당 오픈API 서비스가 없거나 폐기됨 |
| 20 | SERVICE_ACCESS_DENIED_ERROR | 서비스 접근 거부 |
| 22 | LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR | 서비스 요청 제한 횟수 초과 |
| 30 | SERVICE_KEY_IS_NOT_REGISTERED_ERROR | 등록되지 않은 서비스 키 |
| 31 | DEADLINE_HAS_EXPIRED_ERROR | 활용기간 만료 |
| 32 | UNREGISTERED_IP_ERROR | 등록되지 않은 IP |
| 99 | UNKNOWN_ERROR | 기타 에러 |

#### 제공기관 에러코드

| 에러코드 | 에러메시지 | 설명 |
|---------|-----------|------|
| 00 | NORMAL_CODE | 정상 |
| 01 | APPLICATION_ERROR | 어플리케이션 에러 |
| 02 | DB_ERROR | 데이터베이스 에러 |
| 03 | NODATA_ERROR | 데이터 없음 |
| 04 | HTTP_ERROR | HTTP 에러 |
| 05 | SERVICETIMEOUT_ERROR | 서비스 연결 실패 |
| 10 | INVALID_REQUEST_PARAMETER_ERROR | 잘못된 요청 파라미터 |
| 11 | NO_MANDATORY_REQUEST_PARAMETERS_ERROR | 필수 요청 파라미터 없음 |
| 12 | NO_OPENAPI_SERVICE_ERROR | 해당 오픈API 서비스가 없거나 폐기됨 |
| 20 | SERVICE_ACCESS_DENIED_ERROR | 서비스 접근 거부 |
| 21 | TEMPORARILY_DISABLE_THE_SERVICEKEY_ERROR | 일시적으로 사용할 수 없는 서비스 키 |
| 22 | LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR | 서비스 요청 제한 횟수 초과 |
| 30 | SERVICE_KEY_IS_NOT_REGISTERED_ERROR | 등록되지 않은 서비스 키 |
| 31 | DEADLINE_HAS_EXPIRED_ERROR | 활용기간 만료 |
| 32 | UNREGISTERED_IP_ERROR | 등록되지 않은 IP |
| 33 | UNSIGNED_CALL_ERROR | 서명되지 않은 호출 |
| 99 | UNKNOWN_ERROR | 기타 에러 |

---

### 1) 지역기반 관광지별 연관관광지 정보 조회 (`areaBasedList1`)

시군구 코드를 기반으로 관광지별 연관 관광지 정보 목록을 조회합니다.

**Call Back URL**
```
http://apis.data.go.kr/B551011/TarRlteTarService1/areaBasedList1
```

#### 요청 파라미터

> 항목구분: 필수(1), 옵션(0)

| 파라미터명 | 국문명 | 필수 | 샘플 | 설명 |
|-----------|--------|:----:|------|------|
| `serviceKey` | 인증키 | 1 | 인증키(URL-Encode) | 공공데이터포털에서 발급받은 인증키 |
| `MobileOS` | OS 구분 | 1 | ETC | IOS, AND, WEB, ETC |
| `MobileApp` | 서비스명 | 1 | AppTest | 서비스명=어플명 |
| `baseYm` | 기준연월 | 1 | 202504 | 조회 기준 연월 (형식: YYYYMM) |
| `areaCd` | 지역코드 | 1 | 51 | 시도 지역코드 (코드표 참고) |
| `signguCd` | 시군구코드 | 1 | 51130 | 시군구 코드 (코드표 참고) |
| `numOfRows` | 한 페이지 결과 수 | 0 | 10 | 한 페이지 결과 수 |
| `pageNo` | 페이지 번호 | 0 | 1 | 현재 페이지 번호 |
| `_type` | 응답 형식 | 0 | json | json 지정 시 JSON 응답 (기본값: XML) |

#### 응답 항목

> item 노드 이하는 알파벳 순서로 정렬됨

| 항목명 | 국문명 | 필수 | 샘플 | 설명 |
|--------|--------|:----:|------|------|
| `resultCode` | 결과코드 | 1 | 0000 | 응답 결과코드 |
| `resultMsg` | 결과메시지 | 1 | OK | 응답 결과메시지 |
| `numOfRows` | 한 페이지 결과 수 | 1 | 10 | - |
| `pageNo` | 페이지 번호 | 1 | 1 | - |
| `totalCount` | 전체 결과 수 | 1 | 800 | - |
| `baseYm` | 기준연월 | 1 | 202504 | - |
| `tAtsNm` | 관광지명 | 1 | 간현관광지 | - |
| `areaCd` | 관광지 지역코드 | 1 | 51 | - |
| `areaNm` | 관광지 지역명 | 0 | 강원특별자치도 | - |
| `signguCd` | 관광지 시군구코드 | 1 | 51130 | - |
| `signguNm` | 관광지 시군구명 | 0 | 원주시 | - |
| `rlteTatsCd` | 연관관광지코드 | 1 | 0bfeca2105aa7bf8... | - |
| `rlteTatsNm` | 연관관광지명 | 1 | 뮤지엄산 | - |
| `rlteRegnCd` | 연관관광지 지역코드 | 1 | 51 | 시도 코드 |
| `rlteRegnNm` | 연관관광지 지역명 | 0 | 강원특별자치도 | - |
| `rlteSignguCd` | 연관관광지 시군구코드 | 1 | 51130 | - |
| `rlteSignguNm` | 연관관광지 시군구명 | 0 | 원주시 | - |
| `rlteCtgryLclsNm` | 연관 카테고리 대분류 | 0 | 관광지 | - |
| `rlteCtgryMclsNm` | 연관 카테고리 중분류 | 0 | 문화관광 | - |
| `rlteCtgrySclsNm` | 연관 카테고리 소분류 | 0 | 전시시설 | - |
| `rlteRank` | 연관순위 | 1 | 1 | - |

#### 요청/응답 예제

**요청 URL**
```
http://apis.data.go.kr/B551011/TarRlteTarService1/areaBasedList1?serviceKey=서비스인증키&numOfRows=10&pageNo=1&MobileOS=ETC&MobileApp=AppTest&baseYm=202504&areaCd=51&signguCd=51130
```

**응답 (XML)**
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<response>
  <header>
    <resultCode>0000</resultCode>
    <resultMsg>OK</resultMsg>
  </header>
  <body>
    <items>
      <item>
        <baseYm>202504</baseYm>
        <tAtsCd>3dbadaccd57c18ae536e552040025fa8</tAtsCd>
        <tAtsNm>간현관광지</tAtsNm>
        <areaCd>51</areaCd>
        <areaNm>강원특별자치도</areaNm>
        <signguCd>51130</signguCd>
        <signguNm>원주시</signguNm>
        <rlteTatsCd>0bfeca2105aa7bf8d83e4622e5da19ec</rlteTatsCd>
        <rlteTatsNm>뮤지엄산</rlteTatsNm>
        <rlteRegnCd>51</rlteRegnCd>
        <rlteRegnNm>강원특별자치도</rlteRegnNm>
        <rlteSignguCd>51130</rlteSignguCd>
        <rlteSignguNm>원주시</rlteSignguNm>
        <rlteCtgryLclsNm>관광지</rlteCtgryLclsNm>
        <rlteCtgryMclsNm>문화관광</rlteCtgryMclsNm>
        <rlteCtgrySclsNm>전시시설</rlteCtgrySclsNm>
        <rlteRank>1</rlteRank>
      </item>
      <!-- ... -->
    </items>
    <numOfRows>10</numOfRows>
    <pageNo>1</pageNo>
    <totalCount>800</totalCount>
  </body>
</response>
```

---

### 2) 키워드검색 관광지별 연관관광지 정보 조회 (`searchKeyword1`)

키워드로 관광지를 검색하여 연관 관광지 정보를 조회합니다.

**Call Back URL**
```
http://apis.data.go.kr/B551011/TarRlteTarService1/searchKeyword1
```

#### 요청 파라미터

> 항목구분: 필수(1), 옵션(0)

| 파라미터명 | 국문명 | 필수 | 샘플 | 설명 |
|-----------|--------|:----:|------|------|
| `serviceKey` | 인증키 | 1 | 인증키(URL-Encode) | 공공데이터포털에서 발급받은 인증키 |
| `MobileOS` | OS 구분 | 1 | ETC | IOS, AND, WEB, ETC |
| `MobileApp` | 서비스명 | 1 | AppTest | 서비스명=어플명 |
| `baseYm` | 기준연월 | 1 | 202504 | 조회 기준 연월 (형식: YYYYMM) |
| `areaCd` | 지역코드 | 1 | 51 | 시도 지역코드 (코드표 참고) |
| `signguCd` | 시군구코드 | 1 | 51130 | 시군구 코드 (코드표 참고) |
| `keyword` | 요청 키워드 | 1 | 뮤지엄산 | 검색 요청할 관광지 명칭 |
| `numOfRows` | 한 페이지 결과 수 | 0 | 10 | - |
| `pageNo` | 페이지 번호 | 0 | 1 | - |
| `_type` | 응답 형식 | 0 | json | json 지정 시 JSON 응답 (기본값: XML) |

#### 응답 항목

> item 노드 이하는 알파벳 순서로 정렬됨

| 항목명 | 국문명 | 필수 | 샘플 | 설명 |
|--------|--------|:----:|------|------|
| `resultCode` | 결과코드 | 1 | 0000 | - |
| `resultMsg` | 결과메시지 | 1 | OK | - |
| `numOfRows` | 한 페이지 결과 수 | 1 | 10 | - |
| `pageNo` | 페이지 번호 | 1 | 1 | - |
| `totalCount` | 전체 결과 수 | 1 | 50 | - |
| `baseYm` | 기준연월 | 1 | 202504 | - |
| `tAtsCd` | 관광지코드 | 1 | 0bfeca2105aa7bf8... | - |
| `tAtsNm` | 관광지명 | 1 | 뮤지엄산 | - |
| `areaCd` | 관광지 지역코드 | 1 | 51 | - |
| `areaNm` | 관광지 지역명 | 0 | 강원특별자치도 | - |
| `signguCd` | 관광지 시군구코드 | 1 | 51130 | - |
| `signguNm` | 관광지 시군구명 | 0 | 원주시 | - |
| `rlteTatsCd` | 연관관광지코드 | 1 | 31c13fce36918d9c... | - |
| `rlteTatsNm` | 연관관광지명 | 1 | 황금들밥/오크밸리월송점 | - |
| `rlteRegnCd` | 연관관광지 지역코드 | 1 | 51 | - |
| `rlteRegnNm` | 연관관광지 지역명 | 0 | 강원특별자치도 | - |
| `rlteSignguCd` | 연관관광지 시군구코드 | 1 | 51130 | - |
| `rlteSignguNm` | 연관관광지 시군구명 | 0 | 원주시 | - |
| `rlteCtgryLclsNm` | 연관 카테고리 대분류 | 0 | 음식 | - |
| `rlteCtgryMclsNm` | 연관 카테고리 중분류 | 0 | 음식 | - |
| `rlteCtgrySclsNm` | 연관 카테고리 소분류 | 0 | 한식 | - |
| `rlteRank` | 연관순위 | 1 | 1 | - |

#### 요청/응답 예제

**요청 URL**
```
http://apis.data.go.kr/B551011/TarRlteTarService1/searchKeyword1?serviceKey=서비스인증키&numOfRows=10&pageNo=1&MobileOS=ETC&MobileApp=AppTest&baseYm=202504&areaCd=51&signguCd=51130&keyword=뮤지엄산
```

**응답 (XML)**
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<response>
  <header>
    <resultCode>0000</resultCode>
    <resultMsg>OK</resultMsg>
  </header>
  <body>
    <items>
      <item>
        <baseYm>202504</baseYm>
        <tAtsCd>0bfeca2105aa7bf8d83e4622e5da19ec</tAtsCd>
        <tAtsNm>뮤지엄산</tAtsNm>
        <areaCd>51</areaCd>
        <areaNm>강원특별자치도</areaNm>
        <signguCd>51130</signguCd>
        <signguNm>원주시</signguNm>
        <rlteTatsCd>31c13fce36918d9c6bab361f0fd20cc7</rlteTatsCd>
        <rlteTatsNm>황금들밥/오크밸리월송점</rlteTatsNm>
        <rlteRegnCd>51</rlteRegnCd>
        <rlteRegnNm>강원특별자치도</rlteRegnNm>
        <rlteSignguCd>51130</rlteSignguCd>
        <rlteSignguNm>원주시</rlteSignguNm>
        <rlteCtgryLclsNm>음식</rlteCtgryLclsNm>
        <rlteCtgryMclsNm>음식</rlteCtgryMclsNm>
        <rlteCtgrySclsNm>한식</rlteCtgrySclsNm>
        <rlteRank>1</rlteRank>
      </item>
      <item>
        <baseYm>202504</baseYm>
        <tAtsCd>0bfeca2105aa7bf8d83e4622e5da19ec</tAtsCd>
        <tAtsNm>뮤지엄산</tAtsNm>
        <areaCd>51</areaCd>
        <areaNm>강원특별자치도</areaNm>
        <signguCd>51130</signguCd>
        <signguNm>원주시</signguNm>
        <rlteTatsCd>488af5b2e04bba94e29498c4f9a5686d</rlteTatsCd>
        <rlteTatsNm>원주소금산출렁다리</rlteTatsNm>
        <rlteRegnCd>51</rlteRegnCd>
        <rlteRegnNm>강원특별자치도</rlteRegnNm>
        <rlteSignguCd>51130</rlteSignguCd>
        <rlteSignguNm>원주시</rlteSignguNm>
        <rlteCtgryLclsNm>관광지</rlteCtgryLclsNm>
        <rlteCtgryMclsNm>기타관광</rlteCtgryMclsNm>
        <rlteCtgrySclsNm>기타관광</rlteCtgrySclsNm>
        <rlteRank>2</rlteRank>
      </item>
      <!-- ... -->
    </items>
    <numOfRows>10</numOfRows>
    <pageNo>1</pageNo>
    <totalCount>50</totalCount>
  </body>
</response>
```

---

## IV. 지역/시군구 코드표

### 서울특별시 (areaCd: 11)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 11110 | 종로구 |
| 11140 | 중구 |
| 11170 | 용산구 |
| 11200 | 성동구 |
| 11215 | 광진구 |
| 11230 | 동대문구 |
| 11260 | 중랑구 |
| 11290 | 성북구 |
| 11305 | 강북구 |
| 11320 | 도봉구 |
| 11350 | 노원구 |
| 11380 | 은평구 |
| 11410 | 서대문구 |
| 11440 | 마포구 |
| 11470 | 양천구 |
| 11500 | 강서구 |
| 11530 | 구로구 |
| 11545 | 금천구 |
| 11560 | 영등포구 |
| 11590 | 동작구 |
| 11620 | 관악구 |
| 11650 | 서초구 |
| 11680 | 강남구 |
| 11710 | 송파구 |
| 11740 | 강동구 |

### 부산광역시 (areaCd: 26)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 26110 | 중구 |
| 26140 | 서구 |
| 26170 | 동구 |
| 26200 | 영도구 |
| 26230 | 부산진구 |
| 26260 | 동래구 |
| 26290 | 남구 |
| 26320 | 북구 |
| 26350 | 해운대구 |
| 26380 | 사하구 |
| 26410 | 금정구 |
| 26440 | 강서구 |
| 26470 | 연제구 |
| 26500 | 수영구 |
| 26530 | 사상구 |
| 26710 | 기장군 |

### 대구광역시 (areaCd: 27)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 27110 | 중구 |
| 27140 | 동구 |
| 27170 | 서구 |
| 27200 | 남구 |
| 27230 | 북구 |
| 27260 | 수성구 |
| 27290 | 달서구 |
| 27710 | 달성군 |
| 27720 | 군위군 |

### 인천광역시 (areaCd: 28)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 28110 | 중구 |
| 28140 | 동구 |
| 28177 | 미추홀구 |
| 28185 | 연수구 |
| 28200 | 남동구 |
| 28237 | 부평구 |
| 28245 | 계양구 |
| 28260 | 서구 |
| 28710 | 강화군 |
| 28720 | 옹진군 |

### 광주광역시 (areaCd: 29)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 29110 | 동구 |
| 29140 | 서구 |
| 29155 | 남구 |
| 29170 | 북구 |
| 29200 | 광산구 |

### 대전광역시 (areaCd: 30)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 30110 | 동구 |
| 30140 | 중구 |
| 30170 | 서구 |
| 30200 | 유성구 |
| 30230 | 대덕구 |

### 울산광역시 (areaCd: 31)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 31110 | 중구 |
| 31140 | 남구 |
| 31170 | 동구 |
| 31200 | 북구 |
| 31710 | 울주군 |

### 세종특별자치시 (areaCd: 36)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 36110 | 세종특별자치시 |

### 경기도 (areaCd: 41)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 41111 | 수원시 장안구 |
| 41113 | 수원시 권선구 |
| 41115 | 수원시 팔달구 |
| 41117 | 수원시 영통구 |
| 41131 | 성남시 수정구 |
| 41133 | 성남시 중원구 |
| 41135 | 성남시 분당구 |
| 41150 | 의정부시 |
| 41171 | 안양시 만안구 |
| 41173 | 안양시 동안구 |
| 41192 | 부천시 원미구 |
| 41194 | 부천시 소사구 |
| 41196 | 부천시 오정구 |
| 41210 | 광명시 |
| 41220 | 평택시 |
| 41250 | 동두천시 |
| 41271 | 안산시 상록구 |
| 41273 | 안산시 단원구 |
| 41281 | 고양시 덕양구 |
| 41285 | 고양시 일산동구 |
| 41287 | 고양시 일산서구 |
| 41290 | 과천시 |
| 41310 | 구리시 |
| 41360 | 남양주시 |
| 41370 | 오산시 |
| 41390 | 시흥시 |
| 41410 | 군포시 |
| 41430 | 의왕시 |
| 41450 | 하남시 |
| 41461 | 용인시 처인구 |
| 41463 | 용인시 기흥구 |
| 41465 | 용인시 수지구 |
| 41480 | 파주시 |
| 41500 | 이천시 |
| 41550 | 안성시 |
| 41570 | 김포시 |
| 41590 | 화성시 |
| 41610 | 광주시 |
| 41630 | 양주시 |
| 41650 | 포천시 |
| 41670 | 여주시 |
| 41800 | 연천군 |
| 41820 | 가평군 |
| 41830 | 양평군 |

### 충청북도 (areaCd: 43)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 43111 | 청주시 상당구 |
| 43112 | 청주시 서원구 |
| 43113 | 청주시 흥덕구 |
| 43114 | 청주시 청원구 |
| 43130 | 충주시 |
| 43150 | 제천시 |
| 43720 | 보은군 |
| 43730 | 옥천군 |
| 43740 | 영동군 |
| 43745 | 증평군 |
| 43750 | 진천군 |
| 43760 | 괴산군 |
| 43770 | 음성군 |
| 43800 | 단양군 |

### 충청남도 (areaCd: 44)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 44131 | 천안시 동남구 |
| 44133 | 천안시 서북구 |
| 44150 | 공주시 |
| 44180 | 보령시 |
| 44200 | 아산시 |
| 44210 | 서산시 |
| 44230 | 논산시 |
| 44250 | 계룡시 |
| 44270 | 당진시 |
| 44710 | 금산군 |
| 44760 | 부여군 |
| 44770 | 서천군 |
| 44790 | 청양군 |
| 44800 | 홍성군 |
| 44810 | 예산군 |
| 44825 | 태안군 |

### 전라남도 (areaCd: 46)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 46110 | 목포시 |
| 46130 | 여수시 |
| 46150 | 순천시 |
| 46170 | 나주시 |
| 46230 | 광양시 |
| 46710 | 담양군 |
| 46720 | 곡성군 |
| 46730 | 구례군 |
| 46770 | 고흥군 |
| 46780 | 보성군 |
| 46790 | 화순군 |
| 46800 | 장흥군 |
| 46810 | 강진군 |
| 46820 | 해남군 |
| 46830 | 영암군 |
| 46840 | 무안군 |
| 46860 | 함평군 |
| 46870 | 영광군 |
| 46880 | 장성군 |
| 46890 | 완도군 |
| 46900 | 진도군 |
| 46910 | 신안군 |

### 경상북도 (areaCd: 47)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 47111 | 포항시 남구 |
| 47113 | 포항시 북구 |
| 47130 | 경주시 |
| 47150 | 김천시 |
| 47170 | 안동시 |
| 47190 | 구미시 |
| 47210 | 영주시 |
| 47230 | 영천시 |
| 47250 | 상주시 |
| 47280 | 문경시 |
| 47290 | 경산시 |
| 47730 | 의성군 |
| 47750 | 청송군 |
| 47760 | 영양군 |
| 47770 | 영덕군 |
| 47820 | 청도군 |
| 47830 | 고령군 |
| 47840 | 성주군 |
| 47850 | 칠곡군 |
| 47900 | 예천군 |
| 47920 | 봉화군 |
| 47930 | 울진군 |
| 47940 | 울릉군 |

### 경상남도 (areaCd: 48)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 48121 | 창원시 의창구 |
| 48123 | 창원시 성산구 |
| 48125 | 창원시 마산합포구 |
| 48127 | 창원시 마산회원구 |
| 48129 | 창원시 진해구 |
| 48170 | 진주시 |
| 48220 | 통영시 |
| 48240 | 사천시 |
| 48250 | 김해시 |
| 48270 | 밀양시 |
| 48310 | 거제시 |
| 48330 | 양산시 |
| 48720 | 의령군 |
| 48730 | 함안군 |
| 48740 | 창녕군 |
| 48820 | 고성군 |
| 48840 | 남해군 |
| 48850 | 하동군 |
| 48860 | 산청군 |
| 48870 | 함양군 |
| 48880 | 거창군 |
| 48890 | 합천군 |

### 제주특별자치도 (areaCd: 50)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 50110 | 제주시 |
| 50130 | 서귀포시 |

### 강원특별자치도 (areaCd: 51)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 51110 | 춘천시 |
| 51130 | 원주시 |
| 51150 | 강릉시 |
| 51170 | 동해시 |
| 51190 | 태백시 |
| 51210 | 속초시 |
| 51230 | 삼척시 |
| 51720 | 홍천군 |
| 51730 | 횡성군 |
| 51750 | 영월군 |
| 51760 | 평창군 |
| 51770 | 정선군 |
| 51780 | 철원군 |
| 51790 | 화천군 |
| 51800 | 양구군 |
| 51810 | 인제군 |
| 51820 | 고성군 |
| 51830 | 양양군 |

### 전북특별자치도 (areaCd: 52)

| 시군구코드 | 시군구명 |
|-----------|---------|
| 52111 | 전주시 완산구 |
| 52113 | 전주시 덕진구 |
| 52130 | 군산시 |
| 52140 | 익산시 |
| 52180 | 정읍시 |
| 52190 | 남원시 |
| 52210 | 김제시 |
| 52710 | 완주군 |
| 52720 | 진안군 |
| 52730 | 무주군 |
| 52740 | 장수군 |
| 52750 | 임실군 |
| 52770 | 순창군 |
| 52790 | 고창군 |
| 52800 | 부안군 |
