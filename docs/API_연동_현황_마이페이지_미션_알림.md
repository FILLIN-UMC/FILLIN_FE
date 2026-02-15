# API 연동 현황 공유 (마이페이지 · 미션 · 알림)

안녕하세요, FE 담당입니다.  
API 명세서 기준으로 **마이페이지 / 미션 / 알림** 담당 구간의 연동 현황 정리했습니다.

---

## 1. 마이페이지 (My Page)

| API 명세 | Method | URL | 연동 현황 | 비고 |
|----------|--------|-----|-----------|------|
| 프로필 조회 | GET | `/api/mypage/profile` | ✅ 연동 완료 | MyPageViewModel에서 초기 로드 시 호출, 닉네임·프로필 이미지·memberId 저장 |
| 프로필 편집 | POST | `/api/mypage/profile/edit` | ✅ 연동 완료 | MyPageScreen 프로필 수정 시 호출 (닉네임·프로필 이미지) |
| 프로필 닉네임 중복조회 | GET | `/api/mypage/profile/check` | ✅ 연동 완료 | MyPageScreen 닉네임 변경 시 중복 검사 |
| 랭크 조회 | GET | `/api/mypage/profile/ranks` | ✅ 연동 완료 | MyPageViewModel에서 호출, 뱃지(achievement) 등 표시 |
| 총 제보 및 전체 조회수 | GET | `/api/mypage/reports/count` | ✅ 연동 완료 | MyPageViewModel에서 호출 |
| 내가 한 제보 (위험/불편/발견) | GET | `/api/mypage/reports/category` | ✅ 연동 완료 | MyPageViewModel에서 호출, 카테고리별 제보 수 표시 |
| 사라질 제보 (간단) | GET | `/api/mypage/reports/soon` | ✅ 연동 완료 | MyPageViewModel에서 호출 |
| 사라질 제보 (상세) | GET | `/api/mypage/reports/soon/detail` | ✅ 연동 완료 | ExpiringReportDetailScreen(사라질 제보 상세)에서 호출 |
| 나의 제보 (유지) | GET | `/api/mypage/reports` | ✅ 연동 완료 | HomeScreen(지도 제보 목록), MyPageViewModel, MyReportsScreen(나의 제보 탭), MyPageScreen 등에서 호출 |
| 나의 제보 (사라짐) | GET | `/api/mypage/reports/expired` | ✅ 연동 완료 | MyReportsScreen "사라진 제보" 탭에서 호출 |
| 나의 제보 삭제 | POST | `/api/mypage/reports/{reportId}/expired` | ✅ 연동 완료 | 명세서에는 DELETE로 되어 있을 수 있으나, 실제 백엔드는 POST …/expired(제보 만료로 변경)로 제공 중이며, FE는 해당 API와 연동 완료 (MyReportsScreen 등에서 호출) |
| 저장한 제보 | GET | `/api/mypage/reports/like` | ✅ 연동 완료 | MyPageViewModel에서 호출 |
| 회원 탈퇴 | DELETE | `/api/mypage/withdraw` | ✅ 연동 완료 | MyPageScreen 탈퇴 플로우에서 호출 |

- **구현 위치**: `MypageApiService`, `MypageRepository`, `MyPageViewModel`, `MyPageScreen`, `MyReportsScreen`, `ExpiringReportDetailScreen`, `HomeScreen` 등
- **참고**: 나의 제보 삭제는 백엔드가 `POST /api/mypage/reports/{reportId}/expired`(제보 만료로 변경)로 구현되어 있어, 해당 기준으로 연동 완료 상태입니다.

---

## 2. 미션 (Mission)

| API 명세 | Method | URL | 연동 현황 | 비고 |
|----------|--------|-----|-----------|------|
| 미션 달성도 | GET | `/api/mypage/missions` | ✅ 연동 완료 | MyPageViewModel에서 호출, 마이페이지 미션 영역에 반영 |

- **구현 위치**: `MypageApiService`, `MypageRepository`, `MyPageViewModel`

---

## 3. 알림 (Notification)

| API 명세 | Method | URL | 연동 현황 | 비고 |
|----------|--------|-----|-----------|------|
| 알림 설정 변경 | POST | `/api/mypage/profile/notiSet` | ✅ 연동 완료 | MyPageScreen 알림 설정 화면에서 토글 시 호출 |
| 알림 설정 조회 | GET | `/api/mypage/profile/notiSet` | ✅ 연동 완료 | MyPageScreen 알림 설정 화면 진입 시 호출 |
| 알림 목록 조회 | GET | `/api/alarm/list` | ✅ 연동 완료 | NotificationsScreen에서 목록 로드 시 호출 (쿼리 `read` 옵션 지원) |
| 알림 읽음 처리 | PATCH | `/api/alarm/{alarmId}/read` | ✅ 연동 완료 | NotificationsScreen에서 알림 항목 탭 시 호출 |

- **구현 위치**:  
  - 알림 설정: `MypageApiService`, `MypageRepository`, `MyPageScreen`  
  - 알림 목록/읽음: `AlarmApiService`, `AlarmRepository`, `NotificationsScreen`

---

## 요약

- **마이페이지**: 12개 모두 연동 완료. 나의 제보 삭제는 백엔드 구현대로 `POST /api/mypage/reports/{reportId}/expired`(제보 만료로 변경)와 연동되어 있습니다.
- **미션**: 1개 모두 연동 완료.
- **알림**: 4개 모두 연동 완료.

추가로 명세나 응답 형식이 바뀐 부분이 있으면 알려주시면 반영하겠습니다.
