# 새 제보 마커가 잠시 보였다가 사라지는 문제 — 분석 보고서

## 1. 현상

- 새 제보 등록 성공 후, 해당 위치에 마커가 잠깐 보였다가 **사라짐**.

---

## 2. 백엔드 동작 요약 (수정 없이 분석만)

### 2.1 제보 등록 API

- **경로**: `POST /api/reports`
- **컨트롤러**: `ReportController.createReport()`
- **서비스**: `ReportService.createReport(memberId, requestDto, imageFile)`
  - Report 엔티티 생성 시 `status = ReportStatus.PUBLISHED`, `latitude`/`longitude`는 요청 DTO 값으로 저장
  - `reportRepository.save(report)` 후 **즉시** `savedReport.getId()` 반환
- **응답**: `Response<Long>` — `data`에 **reportId(Long)** 한 개만 내려감.

→ 등록 직후부터 DB에 PUBLISHED 상태로 저장되며, **동일 트랜잭션에서 생성된 ID가 반환**되므로 600ms 뒤 조회 시에도 목록에 포함되는 것이 정상입니다.

### 2.2 내 제보 목록 API

- **경로**: `GET /api/mypage/reports`
- **컨트롤러**: `MyReportController.getMyReportList()`
- **서비스**: `MyReportService.getMyReportList(memberId)`
  - `reportRepository.findByMemberIdAndStatus(memberId, ReportStatus.PUBLISHED)`로 조회
  - 각 Report에 대해 `MyReportListResponseDto` 생성 시 **reportId, latitude, longitude, address, title, reportCategory** 등 모두 매핑

백엔드 DTO/엔티티:

- `MyReportListResponseDto`: reportId, latitude, longitude, address, title, reportCategory, reportImageUrl, viewCount 등
- `Report` 엔티티: id, latitude, longitude, status(PUBLISHED), address 등

→ **백엔드만 보면** 새로 저장한 제보는 곧바로 “내 제보” 목록에 포함되고, 좌표도 함께 내려가는 구조입니다.

---

## 3. 프론트엔드 흐름

### 3.1 제보 목록이 바뀌는 곳 (updatedSampleReports 대입)

| 위치 | 조건 | 동작 |
|------|------|------|
| **A** | `LaunchedEffect(Unit, userDeletedFromRegistered, reportViewModel.uploadStatus)` | `uploadStatus != true` 이고, `lastUploadTimeMillis`가 0이거나 5초 경과 시 **getMyReports() + getPopularReports()** 호출 후 `updatedSampleReports = reportsWithExpired + locallyAdded` |
| **B** | `LaunchedEffect(reportViewModel.uploadStatus, reportViewModel.lastUploadedReport)` — 업로드 성공 블록 | 새 제보 1개 **추가**: `updatedSampleReports = updatedSampleReports + newWithLocation` |
| **C** | 같은 업로드 LaunchedEffect 내, `delay(600)` 이후 | 로그인 시 **getMyReports()**만 다시 호출 후 `updatedSampleReports = reportsWithExpired + locallyAdded` (병합) |
| **D** | 피드백 선택 등 | `updatedSampleReports = updatedSampleReports.map { ... }` (특정 제보만 수정) |

마커는 `updatedSampleReports`를 기반으로 한 `activeReports`(ACTIVE, 삭제/만료 제외)로 그려지므로, **새 제보가 이 목록에서 빠지면** 마커가 사라집니다.

### 3.2 업로드 성공 시 순서 (요약)

1. `uploadStatus == true`, `lastUploadedReport != null` → 새 제보 1건 로컬 추가, `lastUploadTimeMillis` 설정.
2. Toast, `delay(600)`.
3. 로그인 시 `getMyReports()` 호출 → `items`로 `apiReports` 생성 → `reportsWithExpired`, `apiIds`, `locallyAdded` 계산 후  
   `updatedSampleReports = reportsWithExpired + locallyAdded`.
4. `reportListVersion++`, `delay(400)`, `reportListVersion++`, `reportViewModel.resetStatus()`, `delay(300)`, `lastUploadedLatLon = null`.

### 3.3 “첫 번째” API 로드 LaunchedEffect (A)와의 관계

- **키**: `(Unit, userDeletedFromRegistered, reportViewModel.uploadStatus)`.
- `uploadStatus == true`이면 **아무것도 안 하고 return**.
- `lastUploadTimeMillis > 0` 이고 **5초 이내**면 역시 **return** (덮어쓰지 않음).

`resetStatus()`로 `uploadStatus`가 `true → null`로 바뀌면 (A)가 **한 번 더 실행**되지만, 그 시점에는 보통 1.3초 정도만 지난 상태라 `lastUploadTimeMillis` 5초 가드에 걸려 **실제로 덮어쓰지는 않도록** 되어 있습니다.

---

## 4. 원인 후보 (FE 관점)

### 4.1 API 재조회 병합 시 “새 제보”가 빠지는 경우

- **getMyReports()가 600ms 뒤에 새 제보를 포함하지 않는 경우**  
  - 이론상 백엔드는 즉시 PUBLISHED로 저장하므로, 600ms면 대부분 포함될 수 있음.  
  - 다만 **캐시/프록시/네트워크 지연**으로 예전 응답이 올 수 있음.
- 이때:
  - `apiIds`에 새 reportId가 없음.
  - `locallyAdded = updatedSampleReports.filter { it.report.id !in apiIds }` 에서 **방금 추가한 새 제보가 포함**되어야 함.
  - 따라서 `updatedSampleReports = reportsWithExpired + locallyAdded` 하면 **새 제보는 유지**되는 구조가 맞음.

즉, “API에 없어서”만으로는 새 제보가 목록에서 사라지지 않아야 합니다.

### 4.2 API는 새 제보를 주는데, FE에서 “한 번만” 쓰는 경우

- **getMyReports()가 새 제보를 포함해 주고**, `apiReports`에 정상 매핑되는 경우:
  - `apiIds`에 새 id 포함.
  - `locallyAdded`에는 “API에 없는 id”만 남음 → 새 제보는 여기 없음.
  - 최종 목록 = `reportsWithExpired`(API 기준) + `locallyAdded`.
  - 이때 **API에서 내려준 항목의 latitude/longitude가 null**이면, FE에서는 기본값(서울)으로 대체하고 있음.  
    → **마커가 서울로 붙거나, 지도 밖으로 나가 “사라진 것처럼” 보일 수 있음.**

이전에 넣은 수정(방금 올린 제보는 좌표 null이면 업로드 시 사용한 좌표로 채우기)이 이 경우를 노린 것입니다.

### 4.3 첫 번째 LaunchedEffect(A)가 “덮어쓰기”하는 타이밍

- **5초 가드**: `lastUploadTimeMillis`가 설정된 뒤 5초 이내면 (A)는 `updatedSampleReports`를 덮어쓰지 않음.
- **가드가 통과하는 경우**:
  - 업로드 후 5초가 지난 뒤 (A)가 다시 실행될 때 (예: `userDeletedFromRegistered` 변경, 또는 다른 이유로 키 변경).
  - 이때 (A)가 **getMyReports() + getPopularReports()** 결과로만 `updatedSampleReports`를 세팅하고,
  - 만약 그 시점의 `updatedSampleReports`(즉, “현재 스냅샷”)에 새 제보가 **이미 없으면**  
    `locallyAdded`에도 새 제보가 없어서, 한 번 덮어쓸 때 **새 제보가 영구히 빠질 수 있음**.

그래서 “언제 (A)가 실행되느냐”와 “그때 `updatedSampleReports`에 새 제보가 들어 있느냐”가 중요합니다.

### 4.4 화면 이탈 후 재진입 (구성 재진입)

- `updatedSampleReports`, `lastUploadTimeMillis` 등은 **Compose `remember`/`mutableStateOf`** 로만 들고 있음.
- **HomeScreen이 composition을 벗어났다가 다시 들어오면** (다른 탭/화면 갔다가 홈 복귀):
  - `lastUploadTimeMillis`는 **다시 0**으로 초기화됨.
  - `updatedSampleReports`는 **첫 진입 시** `SharedReportData.getReports()`로 초기화됨.
- 이때:
  - 업로드 직후 **SharedReportData에는** LaunchedEffect(updatedSampleReports, permanentlyDeleted)를 통해 새 제보가 반영되어 있을 수 있음.
  - 하지만 **재진입 시점에 (A)가 바로 돌면** `lastUploadTimeMillis == 0`이라 5초 가드를 타지 않고,  
    **getMyReports() 결과로 덮어쓸 수 있음.**
  - 만약 그 API 호출이 **이전에 시작된 요청**이거나 **캐시**라서 새 제보를 아직 안 담고 있으면,  
    `locallyAdded`는 “현재 `updatedSampleReports`” 기준인데, 재진입 시 `updatedSampleReports` 초기값이 **SharedReportData에서 온 것**이라 새 제보가 있으면 `locallyAdded`에 포함될 수 있음.  
  - 반대로, 재진입 시점에 SharedReportData가 아직 동기화되지 않았거나, (A)가 **먼저** 끝나서 예전 목록으로 덮어쓴 뒤에 SharedReportData가 갱신되면, 새 제보가 빠질 수 있음.

즉, **탭/화면 전환 후 홈 복귀** 시 (A) 한 번의 실행 순서와 초기값에 따라 새 제보가 사라질 여지가 있습니다.

### 4.5 경쟁(레이스) 요약

- **업로드 LaunchedEffect** 안에서는:  
  “로컬 추가 → delay(600) → getMyReports() → 병합” 순서라서, 같은 코루틴 안에서 `updatedSampleReports`를 읽을 때는 **이미 새 제보가 들어 있는 상태**가 맞음.
- 문제는 **다른 LaunchedEffect (A)** 가:
  - `uploadStatus` 또는 `userDeletedFromRegistered` 변경 등으로 **재실행**되고,
  - **같은 시점의** `updatedSampleReports`를 읽어서 `locallyAdded`를 계산한 뒤 덮어쓸 때,
  - “그 시점의 스냅샷”에 새 제보가 없으면 (또는 (A)가 예전 API 결과로 덮어쓰면) 새 제보가 사라질 수 있음.
- 또한 **구성 이탈 후 재진입** 시 `lastUploadTimeMillis == 0`이 되므로, (A)가 5초 가드 없이 덮어쓸 수 있어, 위와 결합되면 “잠깐 보였다가 사라짐”이 재현될 수 있음.

---

## 5. 백엔드 쪽에서 확인할 만한 점 (수정 없이)

- **GET /api/mypage/reports** 응답에 방금 **POST /api/reports**로 만든 reportId가 **600ms~1초 안에** 포함되는지 (실제 네트워크/서버 로그로 확인).
- **latitude/longitude**가 항상 채워져 오는지, 아니면 null/0이 오는 경우가 있는지 (DB/엔티티 매핑, DTO 직렬화 확인).
- **ReportStatus**가 목록 조회 시점에 PUBLISHED로 나오는지 (다른 스케줄러/이벤트로 status가 바뀌는지 여부).

---

## 6. 프론트엔드 쪽에서 추가로 보강할 수 있는 방향 (요약)

(백엔드는 수정하지 않는다는 전제 하에 FE만 조정하는 경우)

1. **업로드 직후 “방금 추가한 reportId”를 더 오래 유지**  
   - 예: `lastUploadedReportId` + `lastUploadTimeMillis`를 함께 두고, (A)에서 “현재 API 목록에 lastUploadedReportId가 없으면” **무조건 로컬에 있던 해당 1건을 병합**하거나, **lastUploadedReportId가 있는 한 5초가 아니라 10초 정도는 덮어쓰지 않기**.
2. **구성 재진입 시**  
   - `lastUploadTimeMillis`를 ViewModel 또는 SavedStateHandle에 넣어서 “업로드 직후 5초” 보호가 화면을 나갔다 와도 유지되게 하기.
3. **API 병합 시 “방금 올린 1건”은 항상 보존**  
   - `reportsWithExpired + locallyAdded` 후에, `lastUploadedReportId`가 있고 현재 목록에 없으면 **한 번 더** 그 1건을 append (또는 id 기준으로 merge 시 로컬 좌표 우선).

이렇게 하면 백엔드 응답이 늦거나, 좌표가 null이거나, (A)가 한 번 덮어쓰거나, 재진입으로 5초 가드가 리셋되더라도, **새 제보 마커가 사라지지 않도록** 할 수 있습니다.

---

## 7. 관련 파일 정리

| 구분 | 파일 | 역할 |
|------|------|------|
| FE | `HomeScreen.kt` | updatedSampleReports 갱신 (A·B·C·D), 마커용 activeReports, lastUploadTimeMillis 가드 |
| FE | `ReportViewModel.kt` | uploadStatus, lastUploadedReport |
| FE | `ReportRepository.kt` | createReport API 호출, documentId = reportId.toString() |
| FE | `MypageRepository.kt` | getMyReports() |
| FE | `SharedReportData.kt` | getReports() / setReports(), 초기값 및 동기화 |
| BE | `ReportController.java` | POST /api/reports → reportId 반환 |
| BE | `ReportService.java` | createReport (PUBLISHED, lat/lon 저장) |
| BE | `MyReportController.java` | GET /api/mypage/reports |
| BE | `MyReportService.java` | getMyReportList (findByMemberIdAndStatus PUBLISHED) |
| BE | `Report.java` | id, latitude, longitude, status |
| BE | `MyReportListResponseDto.java` | reportId, latitude, longitude 등 |

이 문서는 **원인 분석과 대응 방향**만 정리한 것이며, 백엔드 코드는 수정하지 않는 전제로 작성했습니다.
