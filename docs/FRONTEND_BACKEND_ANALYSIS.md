# FILLIN 프론트엔드-백엔드 연동 분석

> 분석일: 2025년  
> 프론트엔드(FILLIN_FE)와 백엔드(FILLIN_BE) 코드를 동시에 분석한 결과입니다.

---

## 1. 아키텍처 개요

| 구분 | 기술 스택 | Base URL |
|------|----------|----------|
| **프론트엔드** | Kotlin, Jetpack Compose, Retrofit | `https://api.fillin.site/` |
| **백엔드** | Java, Spring Boot, JPA | - |

---

## 2. API 매핑 및 검증 결과

### 2.1 인증 (Auth)

| 기능 | 백엔드 경로 | 프론트 경로 | 상태 |
|------|-------------|-------------|------|
| 테스트 회원가입 | `POST /api/auth/test/signup` | ~~`api/v1/auth/test/signup`~~ → `api/auth/test/signup` | ✅ **수정 완료** |
| 테스트 로그인 | `POST /api/auth/test/login` | ~~`api/v1/auth/test/login`~~ → `api/auth/test/login` | ✅ **수정 완료** |
| 카카오 로그인 | `POST /api/auth/kakao/login` | `api/auth/kakao/login` | ✅ 일치 |
| 구글 로그인 | `POST /api/auth/google/login` | `api/auth/google/login` | ✅ 일치 |
| 온보딩 완료 | `POST /api/auth/onboarding` | `api/auth/onboarding` | ✅ 일치 |
| 토큰 재발급 | `POST /api/auth/reissue` | `api/auth/reissue` | ✅ 일치 |
| 로그아웃 | `PATCH /api/auth/logout` | `api/auth/logout` | ✅ 일치 |

### 2.2 제보 (Report)

| 기능 | 백엔드 | 프론트 | 상태 |
|------|--------|--------|------|
| 제보 등록 | `POST /api/reports` (multipart) | `api/reports` | ✅ 일치 |
| 인기 제보 | `GET /api/reports/popular` | `api/reports/popular` | ✅ 일치 |
| 피드백 생성 | `POST /api/reports/{id}/feedback` (type: DONE/NOW) | `api/reports/{id}/feedback?type=` | ✅ 일치 |
| 좋아요 토글 | `POST /api/reports/{id}/like` | `api/reports/{id}/like` | ✅ 일치 |

**ReportCreateRequestDto 매핑:**
- 백엔드: `title`, `latitude`, `longitude`, `category` (DANGER/INCONVENIENCE/DISCOVERY)
- 프론트: `ReportCreateRequest` 동일 필드, 카테고리 "위험"→DANGER, "불편"→INCONVENIENCE, "발견"→DISCOVERY

### 2.3 마이페이지 (Mypage)

| 기능 | 백엔드 경로 | 프론트 경로 | 상태 |
|------|-------------|-------------|------|
| 나의 제보 목록 | `GET /api/mypage/reports` | `api/mypage/reports` | ✅ 일치 |
| 제보 삭제(만료) | `POST /api/mypage/reports/{id}/expired` | `api/mypage/reports/{id}/expired` | ✅ 일치 |
| 프로필 조회/수정 | `GET/POST /api/mypage/profile` | 동일 | ✅ 일치 |

### 2.4 알림 (Alarm)

| 기능 | 백엔드 | 프론트 | 상태 |
|------|--------|--------|------|
| 알림 목록 | `GET /api/alarm/list?read=` | `api/alarm/list?read=` | ✅ 일치 |
| 읽음 처리 | `PATCH /api/alarm/{id}/read` | `api/alarm/{id}/read` | ✅ 일치 |

**AlarmResponse 필드:** alarmId, alarmType, message, read, referId, createdAt

---

## 3. 백엔드 응답 구조

```java
// Response<T> 래퍼
{
  "status": "OK",
  "code": "성공코드",
  "message": "메시지",
  "data": T  // 실제 데이터
}
```

프론트엔드 DTO들은 `status`, `code`, `message`, `data` 구조에 맞게 정의됨.

---

## 4. 발견된 이슈 및 조치

### 4.1 ✅ 수정 완료: 테스트 인증 API 경로

- **문제:** 프론트가 `api/v1/auth/test/*` 호출, 백엔드는 `api/auth/test/*` 제공
- **조치:** `UserApiService`, `AuthTokenInterceptor`에서 `api/v1` → `api`로 수정

### 4.2 ⚠️ 백엔드 개선 권장: 제보 등록 시 주소(address) 미전송

- **현황:** `ReportCreateRequestDto`에 `address` 필드 없음. `Report` 엔티티에는 `address` 존재.
- **영향:** 새로 등록한 제보의 `address`가 항상 null. 프론트는 `location`(주소)을 가지고 있으나 전송 불가.
- **권장:** 백엔드에 `ReportCreateRequestDto.address` 추가 후, `ReportService.createReport`에서 `address` 설정

### 4.3 인기 제보 카테고리

- 백엔드 `ReportService.getPopularReports()`: `DISCOVERY`, `INCONVENIENCE`만 조회 (DANGER 제외)
- 프론트: 3가지 카테고리 모두 표시 가능하도록 구현됨

---

## 5. 데이터 흐름 요약

```
[제보 등록]
ReportRegistrationScreen → ReportViewModel.uploadReport()
  → ReportRepository.uploadReportViaApi()
  → POST /api/reports (ReportCreateRequest + image)
  → 백엔드: ReportService.createReport() → S3 업로드 → DB 저장
  → 응답: reportId (Long)

[홈 지도 제보 표시]
- 로그인: GET /api/mypage/reports → MyReportListResponseDto
- 비로그인: GET /api/reports/popular → PopularReportListResponse
→ HomeScreen.updatedSampleReports → 마커 렌더링
```

---

## 6. 참고: 백엔드 주요 컨트롤러

| 컨트롤러 | 경로 | 담당 |
|----------|------|------|
| AuthController | `/api/auth/test` | 테스트 회원가입/로그인 |
| OAuthController | `/api/auth` | 소셜 로그인, 온보딩, 토큰 |
| ReportController | `/api/reports` | 제보 등록, 인기 제보 |
| ReportImageDetailController | `/api/reports` | 제보 상세, 피드백, 좋아요 |
| MyReportController | `/api/mypage/reports` | 나의 제보 CRUD |
| AlarmController | `/api/alarm` | 알림 목록, 읽음 처리 |
