# Google 로그인 설정 가이드

"Developer console is not set up correctly" 오류가 발생하면 Google Cloud Console 설정을 확인하세요.

## 1. SHA-1 지문 확인

### 방법 A: keytool (권장 - Gradle 불필요)

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

출력에서 **SHA1:** 뒤의 값을 복사하세요.

### 방법 B: Gradle signingReport

**Java 25 사용 시** Gradle 빌드가 실패할 수 있습니다. Java 17로 실행하세요:

```bash
# Mac: Java 17이 설치되어 있다면
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew signingReport
```

Java 17 미설치 시: [Adoptium](https://adoptium.net/) 또는 [Oracle JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) 설치

출력에서 **SHA-1** 값을 복사합니다. (debug, release 각각 확인)

## 2. Google Cloud Console 설정

1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. 프로젝트 선택 (또는 새 프로젝트 생성)
3. **API 및 서비스** → **사용자 인증 정보**
4. **OAuth 2.0 클라이언트 ID** 확인/생성

### 필요한 클라이언트 ID

| 타입 | 용도 | 설정 |
|------|------|------|
| **웹 애플리케이션** | 서버 검증용 | `GoogleConfig.WEB_CLIENT_ID`에 사용 |
| **Android** | 앱 로그인용 | 패키지명 + SHA-1 필수 |

### Android OAuth 클라이언트 설정

- **패키지 이름**: `com.example.fillin`
- **SHA-1 인증서 지문**: 위에서 확인한 SHA-1 입력

⚠️ **디버그 빌드**와 **릴리즈 빌드**의 SHA-1이 다르므로, 각각 등록해야 합니다.

## 3. OAuth 동의 화면

- **API 및 서비스** → **OAuth 동의 화면**
- 앱 정보, 개발자 연락처 등 필수 항목 입력
- 테스트 모드: 최대 100명까지 제한

### ⚠️ 테스트 사용자 추가 (필수)

앱이 **테스트** 모드일 때는 **테스트 사용자**로 등록된 이메일만 로그인할 수 있습니다.

1. **OAuth 동의 화면** → **테스트 사용자** 탭
2. **+ ADD USERS** 클릭
3. 로그인에 사용할 Google 이메일 주소 입력 후 저장

테스트 사용자에 추가되지 않은 이메일로 로그인하면 "구글 로그인에 실패했어요." 또는 "access_denied" 오류가 발생합니다.

## 4. 확인 사항

- [ ] Android OAuth 클라이언트 ID 생성됨
- [ ] 패키지명 `com.example.fillin` 일치
- [ ] SHA-1 지문 등록됨 (디버그 키)
- [ ] OAuth 동의 화면 설정 완료
- [ ] `GoogleConfig.WEB_CLIENT_ID`가 **웹 클라이언트 ID**와 일치

## 5. 변경 후

설정 변경 후 **몇 분** 기다려야 반영될 수 있습니다. 앱 재설치 후 다시 시도하세요.
