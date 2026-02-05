# GitHub 웹에서 백엔드 구글 로그인 확인 가이드

FILLIN_BE 저장소를 **웹에서만** 확인할 때 참고하세요.

---

## 1단계: 검색으로 관련 코드 찾기

저장소 페이지 상단 검색창 또는 **`t`** 키로 파일 검색 후, 아래 키워드로 검색해보세요.

### 검색 키워드 (순서대로 시도)

| 검색어 | 찾을 내용 |
|--------|-----------|
| `google` | 구글 관련 설정/코드 |
| `client` | Client ID 설정 |
| `389866708760` | 앱과 동일한 Client ID 사용 여부 |
| `tokeninfo` | Google tokeninfo API 사용 여부 |
| `verifyIdToken` | Google Auth Library 토큰 검증 |
| `google/android` | 구글 안드로이드 로그인 API |

---

## 2단계: 확인할 파일 위치

일반적으로 아래 경로에 있을 수 있습니다.

| 경로 | 확인할 내용 |
|------|-------------|
| `src/main/resources/application.yml` | `google`, `client-id` 등 설정 |
| `src/main/resources/application.properties` | 위와 동일 |
| `.env` 또는 `env.example` | `GOOGLE_CLIENT_ID` 등 환경 변수 |
| `**/auth/**` 또는 `**/google/**` | 구글 로그인 관련 서비스/컨트롤러 |

---

## 3단계: 꼭 확인할 값

### ✅ 맞아야 하는 Web Client ID

```
389866708760-3ii8agcc66jsfdsmadig5nj8k62vlgpi.apps.googleusercontent.com
```

백엔드 설정/코드에 **위 값이 그대로** 있는지 확인하세요.

### ❌ 잘못된 예 (Android Client ID)

```
389866708760-rfceps7lvd5tf4s1huonjrtpcgdh4m06.apps.googleusercontent.com
```

`rfceps7lvd5...`로 시작하면 **Android Client ID**라서 토큰 검증에 사용하면 안 됩니다.

### ✅ 요청 필드명

앱은 `{"code": "id_token"}` 형식으로 보냅니다.  
백엔드에서 `code` 필드를 받는지, `idToken`/`id_token` 등 다른 필드명을 기대하는지 확인하세요.

---

## 4단계: 확인 후 정리

| 확인 항목 | 결과 |
|-----------|------|
| Web Client ID가 위 값과 일치하는가? | ☐ 예 / ☐ 아니오 |
| Android Client ID를 사용하고 있지 않은가? | ☐ 예 / ☐ 아니오 |
| 요청 필드명이 `code`인가? | ☐ 예 / ☐ 아니오 |

---

## 백엔드 담당자에게 전달할 메시지 (문제가 있을 때)

> 구글 로그인 403 오류 관련
>
> - 앱에서 보내는 ID 토큰은 Google tokeninfo에서 정상 검증됨
> - 토큰의 `aud` = `389866708760-3ii8agcc66jsfdsmadig5nj8k62vlgpi.apps.googleusercontent.com`
> - 백엔드에서 위 **Web Client ID**로 토큰 검증하는지 확인 부탁드립니다
