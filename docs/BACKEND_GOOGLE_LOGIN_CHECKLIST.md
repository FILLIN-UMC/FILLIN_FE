# 백엔드 구글 로그인 검증 체크리스트

앱에서 보내는 구글 ID 토큰은 **Google tokeninfo API에서 정상 검증**됨을 확인했습니다.
403 오류는 백엔드 측 토큰 검증 로직 문제로 추정됩니다.

## 백엔드에서 확인할 사항

### 1. Google Client ID 설정

**앱에서 사용 중인 Web Client ID (반드시 동일해야 함):**
```
389866708760-3ii8agcc66jsfdsmadig5nj8k62vlgpi.apps.googleusercontent.com
```

백엔드 환경 변수/설정 파일에서 위 값과 **정확히 일치**하는지 확인하세요.

**검색 키워드:** `GOOGLE_CLIENT_ID`, `google.client-id`, `client_id`, `audience`

---

### 2. 토큰 검증 시 audience(aud) 체크

구글 ID 토큰의 `aud`(audience) 필드는 위 Web Client ID와 일치합니다.
백엔드 검증 로직에서 `aud`를 체크할 때 **위 Client ID**를 사용하는지 확인하세요.

**주의:** Android Client ID(`389866708760-rfceps7lvd5tf4s1huonjrtpcgdh4m06...`)가 아닌
**Web Client ID**를 사용해야 합니다.

---

### 3. 요청 필드명

앱은 다음 형식으로 요청을 보냅니다:
```json
{"code": "구글_ID_토큰_문자열"}
```

API 명세에 `code`가 맞는지, `idToken`/`id_token` 등 다른 필드명을 기대하는지 확인하세요.

---

### 4. 토큰 검증 방법

백엔드에서 권장하는 검증 방법:
- **Google tokeninfo API**: `GET https://oauth2.googleapis.com/tokeninfo?id_token=토큰`
- **Google Auth Library** (Java/Node 등): `verifyIdToken()` 사용 시 **Web Client ID**를 audience로 전달

---

## 앱에서 확인 완료된 사항

| 항목 | 상태 |
|------|------|
| API 경로 | `POST /api/v1/auth/google/android/login` ✅ |
| 토큰 발급 | Google Credential Manager 정상 ✅ |
| 토큰 검증 (tokeninfo) | `aud` = Web Client ID 일치 ✅ |
| 요청 형식 | `{"code": "id_token"}` ✅ |

---

## 백엔드 담당자에게 전달

> 구글 안드로이드 로그인 API 403 오류 관련
>
> 1. 앱에서 보내는 ID 토큰은 `https://oauth2.googleapis.com/tokeninfo`에서 정상 검증됨
> 2. 토큰의 `aud`는 `389866708760-3ii8agcc66jsfdsmadig5nj8k62vlgpi.apps.googleusercontent.com`
> 3. 백엔드 토큰 검증 시 **위 Web Client ID**를 사용하는지 확인 부탁드립니다
> 4. 403 반환 시 에러 메시지를 응답 본문에 포함해 주시면 원인 파악에 도움이 됩니다
