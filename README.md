# 멋사 방학세션 #3 - 카카오로그인 테스트

Spring Boot 기반의 카카오 OAuth2 로그인 예제 프로젝트입니다.  
이 프로젝트는 다음 흐름을 한 번에 보여줍니다.

- 카카오 계정으로 로그인
- 첫 로그인 시 회원가입처럼 사용자 저장
- 로그인 성공 시 JWT Access Token / Refresh Token 발급
- HttpOnly 쿠키 기반 인증 유지
- Spring Security Filter Chain 안에서 JWT로 사용자 인증 복원

---

## 1. 이 프로젝트가 해결하는 문제

일반 로그인은 보통 아래 작업이 필요합니다.

- 아이디/비밀번호 입력 UI 만들기
- 비밀번호 암호화
- 회원가입, 로그인, 비밀번호 관리
- 외부 계정 연동

카카오 로그인 같은 소셜 로그인은 이 중에서 "사용자 인증"을 카카오에게 맡기고,  
우리 서비스는 "인증 결과를 받아 우리 서비스 사용자로 연결"하는 방식입니다.

즉:

- 카카오는 "이 사용자가 누구인지" 확인해 줍니다.
- 우리 서버는 "이 사용자를 우리 서비스 회원으로 어떻게 다룰지" 결정합니다.

이렇게 하면 사용자는 회원가입 과정을 줄일 수 있고,  
서비스는 더 빠르게 로그인 기능을 제공할 수 있습니다.

---

## 2. 왜 OAuth2 프레임워크가 필요한가

OAuth2는 외부 서비스가 인증 결과를 안전하게 넘겨주기 위한 표준 흐름입니다.

카카오 로그인을 직접 구현하려고 하면 다음을 직접 처리해야 합니다.

- 카카오 인증 페이지로 이동할 URL 생성
- 인가 코드 처리
- 토큰 요청
- 사용자 정보 API 호출
- 예외 처리
- 리다이렉트 및 보안 검증

Spring Security의 OAuth2 Client를 사용하면 이 복잡한 과정을 프레임워크가 표준 방식으로 처리해 줍니다.

이 프로젝트에서 그 역할을 담당하는 핵심 의존성은 [build.gradle](/Users/shinae/Downloads/kakao/build.gradle:1)의 아래 항목입니다.

- `spring-boot-starter-oauth2-client`
- `spring-boot-starter-security`

정리하면:

- OAuth2 프레임워크는 카카오 로그인 프로토콜 처리를 담당합니다.
- 우리 코드는 로그인 성공 후 사용자 저장, JWT 발급, 화면 이동 같은 "서비스 로직"에 집중합니다.

---

## 3. 전체 동작 원리

### 3-1. 큰 흐름

1. 사용자가 `/` 페이지에서 `카카오로 로그인하기` 버튼을 클릭합니다.
2. Spring Security가 `/oauth2/authorization/kakao` 요청을 받아 카카오 로그인 페이지로 보냅니다.
3. 카카오 로그인 성공 후, 카카오는 우리 서버의 콜백 URL로 인가 코드를 전달합니다.
4. Spring Security OAuth2 Client가 인가 코드를 이용해 카카오 토큰과 사용자 정보를 가져옵니다.
5. [CustomOAuth2UserService.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/security/CustomOAuth2UserService.java:1)가 카카오 사용자 정보를 읽고 회원가입 또는 업데이트를 수행합니다.
6. [OAuth2AuthenticationSuccessHandler.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/handler/OAuth2AuthenticationSuccessHandler.java:1)가 JWT Access Token과 Refresh Token을 발급합니다.
7. 발급된 토큰은 `HttpOnly` 쿠키로 저장됩니다.
8. 이후 요청에서는 [JwtAuthenticationFilter.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/security/JwtAuthenticationFilter.java:1)가 쿠키 또는 `Authorization: Bearer ...` 헤더에서 토큰을 읽어 인증을 복원합니다.

---

## 4. JWT가 왜 필요한가

OAuth2 로그인만으로도 "로그인 성공"까지는 만들 수 있습니다.  
하지만 이후 API 호출마다 사용자가 누구인지 빠르게 확인할 수 있어야 합니다.

이 프로젝트는 그 문제를 JWT로 해결합니다.

### Access Token

- 짧은 수명
- 매 API 요청 인증에 사용
- 이 프로젝트에서는 `access_token` 쿠키에 저장

### Refresh Token

- 더 긴 수명
- Access Token이 만료됐을 때 재발급 용도
- 이 프로젝트에서는 `refresh_token` 쿠키에 저장
- DB에도 저장해서 서버가 유효성을 한 번 더 확인

이 구조의 장점:

- 매 요청마다 세션 저장소 조회에만 의존하지 않아도 됨
- 이후 프론트 분리 구조나 모바일 앱으로 확장하기 쉬움
- 서버가 refresh token을 DB에서 관리하므로 강제 로그아웃이나 토큰 교체 제어가 가능함

---

## 5. Spring Security Filter Chain과 JWT 연결 원리

이 부분이 가장 중요합니다.

### 보안 설정 진입점

[SecurityConfig.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/config/SecurityConfig.java:1)에서 전체 인증 흐름을 조립합니다.

핵심 설정:

- `oauth2Login(...)`
    - 카카오 로그인 흐름 활성화
- `successHandler(successHandler)`
    - 로그인 성공 시 JWT 발급
- `failureHandler(failureHandler)`
    - 로그인 실패 시 에러 메시지와 함께 `/`로 이동
- `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`
    - Spring Security 기본 인증 필터 앞에서 JWT 검사

### 필터체인에서 실제로 일어나는 일

요청이 들어오면 Spring Security는 여러 필터를 순서대로 실행합니다.

이 프로젝트에서는 그중 [JwtAuthenticationFilter.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/security/JwtAuthenticationFilter.java:1)가 먼저 동작합니다.

동작 순서:

1. `Authorization` 헤더에 Bearer 토큰이 있는지 확인
2. 없으면 `access_token` 쿠키를 확인
3. 토큰이 있으면 [JwtTokenProvider.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/security/JwtTokenProvider.java:1)로 유효성 검증
4. 유효하면 토큰에서 사용자 ID를 꺼냄
5. [JwtPrincipalFactory.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/security/JwtPrincipalFactory.java:1)가 사용자 정보를 다시 읽어 `CustomOAuth2User` 생성
6. `SecurityContextHolder`에 인증 객체 저장
7. 이후 컨트롤러에서는 `@AuthenticationPrincipal`로 로그인 사용자를 바로 받을 수 있음

즉, 이 프로젝트에서 JWT 필터는 "요청마다 로그인 상태를 다시 복원하는 장치"입니다.

---

## 6. OAuth2 로그인 성공 후 JWT 발급 원리

로그인 성공 시점은 [OAuth2AuthenticationSuccessHandler.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/handler/OAuth2AuthenticationSuccessHandler.java:1)에서 처리합니다.

핵심 흐름:

1. `Authentication`에서 `CustomOAuth2User`를 꺼냄
2. [JwtAuthService.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/service/JwtAuthService.java:1)에 토큰 발급 요청
3. `JwtAuthService`가 Access Token, Refresh Token 생성
4. Refresh Token은 [AppUser.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/domain/AppUser.java:1)에 저장
5. [JwtCookieUtils.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/util/JwtCookieUtils.java:1)가 두 토큰을 `HttpOnly` 쿠키로 응답에 추가
6. `/login-success` 페이지로 리다이렉트

실패 시에는 [OAuth2AuthenticationFailureHandler.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/handler/OAuth2AuthenticationFailureHandler.java:1)가 `/`로 돌려보내며 오류 메시지를 붙입니다.

---

## 7. Refresh Token 재발급 원리

토큰 재발급 엔드포인트는 [AuthController.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/presentation/AuthController.java:1)의 `POST /api/auth/refresh` 입니다.

동작 순서:

1. 요청 쿠키에서 `refresh_token` 조회
2. [JwtAuthService.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/service/JwtAuthService.java:1)가 refresh token 유효성 검사
3. DB에 저장된 refresh token과 일치하는 사용자 조회
4. 새 access token / refresh token 발급
5. 새 refresh token으로 DB 갱신
6. 새 토큰을 다시 쿠키로 저장

이 방식의 장점:

- 탈취된 오래된 refresh token을 교체하기 쉬움
- 서버가 현재 유효한 refresh token을 통제 가능

---

## 8. 로그아웃 원리

로그아웃은 [CustomLogoutHandler.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/handler/CustomLogoutHandler.java:1)와 [CustomLogoutSuccessHandler.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/handler/CustomLogoutSuccessHandler.java:1)에서 처리합니다.

동작 순서:

1. 현재 로그인 사용자 확인
2. DB에 저장된 refresh token 삭제
3. `access_token`, `refresh_token` 쿠키 만료 처리
4. `/` 페이지로 리다이렉트

즉, 브라우저와 서버 양쪽의 로그인 흔적을 함께 정리합니다.

---

## 9. 현재 파일 구조

```text
.
├── build.gradle
├── README.md
├── src
│   └── main
│       ├── java
│       │   └── mutsa
│       │       └── kakao
│       │           ├── KakaoApplication.java
│       │           └── auth
│       │               ├── config
│       │               │   └── SecurityConfig.java
│       │               ├── domain
│       │               │   ├── AppUser.java
│       │               │   └── KakaoUserProfile.java
│       │               ├── dto
│       │               │   └── AuthTokenResponse.java
│       │               ├── handler
│       │               │   ├── CustomLogoutHandler.java
│       │               │   ├── CustomLogoutSuccessHandler.java
│       │               │   ├── OAuth2AuthenticationFailureHandler.java
│       │               │   └── OAuth2AuthenticationSuccessHandler.java
│       │               ├── presentation
│       │               │   └── AuthController.java
│       │               ├── repository
│       │               │   └── AppUserRepository.java
│       │               ├── security
│       │               │   ├── CustomOAuth2User.java
│       │               │   ├── CustomOAuth2UserService.java
│       │               │   ├── JwtAuthenticationFilter.java
│       │               │   ├── JwtPrincipalFactory.java
│       │               │   └── JwtTokenProvider.java
│       │               ├── service
│       │               │   ├── AppUserService.java
│       │               │   └── JwtAuthService.java
│       │               └── util
│       │                   └── JwtCookieUtils.java
│       └── resources
│           ├── application.yaml
│           ├── static
│           │   └── css
│           │       └── auth.css
│           └── templates
│               ├── index.html
│               └── login-success.html
```

---

## 10. 파일별 역할 정리

### 애플리케이션 시작

- [KakaoApplication.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/KakaoApplication.java:1)
    - Spring Boot 실행 진입점

### 설정

- [build.gradle](/Users/shinae/Downloads/kakao/build.gradle:1)
    - Spring Boot, Security, OAuth2 Client, Thymeleaf, JPA, JWT 의존성 관리
- [application.yaml](/Users/shinae/Downloads/kakao/src/main/resources/application.yaml:1)
    - 카카오 OAuth2 설정, JWT 설정, H2/JPA 설정 관리
- [SecurityConfig.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/config/SecurityConfig.java:1)
    - OAuth2 로그인, JWT 필터, 로그아웃, 인증 정책 조립

### 도메인

- [AppUser.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/domain/AppUser.java:1)
    - DB에 저장되는 사용자 엔티티
    - 카카오 provider 정보, 닉네임, 이메일, 프로필 이미지, refresh token 저장
- [KakaoUserProfile.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/domain/KakaoUserProfile.java:1)
    - 화면/인증 계층에서 사용하는 사용자 정보 DTO

### DTO

- [AuthTokenResponse.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/dto/AuthTokenResponse.java:1)
    - access token / refresh token 응답 포맷

### Repository / Service

- [AppUserRepository.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/repository/AppUserRepository.java:1)
    - 사용자 조회 및 refresh token 기준 조회
- [AppUserService.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/service/AppUserService.java:1)
    - 회원가입/업데이트, refresh token 저장/삭제, 사용자 조회 담당
- [JwtAuthService.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/service/JwtAuthService.java:1)
    - 토큰 발급 및 재발급 담당

### Security

- [CustomOAuth2User.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/security/CustomOAuth2User.java:1)
    - Spring Security가 다룰 로그인 사용자 객체
- [CustomOAuth2UserService.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/security/CustomOAuth2UserService.java:1)
    - 카카오 사용자 정보를 읽어 우리 서비스 사용자로 매핑
- [JwtTokenProvider.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/security/JwtTokenProvider.java:1)
    - JWT 생성, 파싱, 검증 담당
- [JwtAuthenticationFilter.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/security/JwtAuthenticationFilter.java:1)
    - 요청마다 JWT를 읽어 인증 복원
- [JwtPrincipalFactory.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/security/JwtPrincipalFactory.java:1)
    - 토큰 속 사용자 ID를 실제 로그인 사용자 객체로 변환

### Handler

- [OAuth2AuthenticationSuccessHandler.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/handler/OAuth2AuthenticationSuccessHandler.java:1)
    - 로그인 성공 후 JWT 발급, 쿠키 저장, 성공 페이지 이동
- [OAuth2AuthenticationFailureHandler.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/handler/OAuth2AuthenticationFailureHandler.java:1)
    - 로그인 실패 후 에러 메시지와 함께 홈으로 이동
- [CustomLogoutHandler.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/handler/CustomLogoutHandler.java:1)
    - refresh token 삭제, 쿠키 만료 처리
- [CustomLogoutSuccessHandler.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/handler/CustomLogoutSuccessHandler.java:1)
    - 로그아웃 완료 후 홈 이동

### Presentation / View

- [AuthController.java](/Users/shinae/Downloads/kakao/src/main/java/mutsa/kakao/auth/presentation/AuthController.java:1)
    - 홈 화면, 로그인 성공 화면, 내 정보 API, 토큰 재발급 API 담당
- [index.html](/Users/shinae/Downloads/kakao/src/main/resources/templates/index.html:1)
    - 카카오 로그인 시작 화면
- [login-success.html](/Users/shinae/Downloads/kakao/src/main/resources/templates/login-success.html:1)
    - 로그인 성공 후 프로필 및 JWT 안내 화면
- [auth.css](/Users/shinae/Downloads/kakao/src/main/resources/static/css/auth.css:1)
    - 화면 스타일 정의

---

## 11. 카카오 개발자 콘솔에서 무엇을 받아야 하나

카카오 로그인을 붙이려면 [Kakao Developers](https://developers.kakao.com/) 콘솔에서 앱을 생성해야 합니다.

필요한 값은 사실상 아래 두 가지입니다.

### 반드시 필요한 값

- `REST API 키`
    - 이 프로젝트의 `KAKAO_CLIENT_ID`에 넣습니다.

### 상황에 따라 필요한 값

- `Client Secret`
    - 카카오 콘솔에서 보안 설정 후 사용할 수 있습니다.
    - 이 프로젝트의 `KAKAO_CLIENT_SECRET`에 넣습니다.
    - 현재 코드에서는 비어 있어도 되도록 `${KAKAO_CLIENT_SECRET:}` 로 설정되어 있습니다.

---

## 12. 카카오 개발자 콘솔에서 꼭 설정해야 하는 항목

### 1. 플랫폼 등록

카카오 개발자 콘솔에서 웹 플랫폼을 추가하고 사이트 도메인을 등록합니다.

로컬 개발 예시:

- `http://localhost:8080`

### 2. Redirect URI 등록

이 프로젝트의 OAuth2 콜백 URL은 아래 값입니다.

```text
http://localhost:8080/login/oauth2/code/kakao
```

카카오 콘솔의 Redirect URI에도 이 값을 그대로 등록해야 합니다.

### 3. 동의 항목 설정

현재 코드에서 요청하는 scope는 [application.yaml](/Users/shinae/Downloads/kakao/src/main/resources/application.yaml:1)에 정의되어 있습니다.

```yaml
scope:
  - profile_nickname
  - profile_image
  - account_email
```

따라서 카카오 콘솔에서 다음 동의 항목을 확인해야 합니다.

- 닉네임
- 프로필 이미지
- 이메일

이메일은 사용자가 동의하지 않을 수도 있으므로, 코드에서도 `null` 가능성을 열어두고 있습니다.

---

## 13. 어떤 값을 어디에 입력해야 하나

이 프로젝트는 [application.yaml](/Users/shinae/Downloads/kakao/src/main/resources/application.yaml:1)에서 환경변수를 읽습니다.

### 카카오 관련

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET:}
```

입력해야 할 값:

- `KAKAO_CLIENT_ID`
    - 카카오 콘솔의 `REST API 키`
- `KAKAO_CLIENT_SECRET`
    - 카카오 콘솔에서 발급한 `Client Secret`

### JWT 관련

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:change-this-secret-key-change-this-secret-key-123456}
    access-token-expiration: 1800000
    refresh-token-expiration: 1209600000
    cookie-secure: false
```

입력해야 할 값:

- `JWT_SECRET`
    - 운영에서는 반드시 복잡한 비밀키로 바꿔야 합니다.

설정 의미:

- `access-token-expiration`
    - access token 만료 시간, 현재 30분
- `refresh-token-expiration`
    - refresh token 만료 시간, 현재 14일
- `cookie-secure`
    - `true`이면 HTTPS에서만 쿠키 전송
    - 로컬 개발은 보통 `false`
    - 운영 HTTPS 환경에서는 `true` 권장

---

## 14. 로컬 실행 방법

### 1. 환경변수 설정

예시:

```bash
export KAKAO_CLIENT_ID=여기에_카카오_REST_API_키
export KAKAO_CLIENT_SECRET=여기에_카카오_Client_Secret
export JWT_SECRET=여기에_충분히_긴_JWT_비밀키
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 접속

브라우저에서 아래 주소로 접속합니다.

```text
http://localhost:8080
```

---

## 15. 로그인 후 실제로 어떤 엔드포인트를 쓰는가

### 화면

- `GET /`
    - 로그인 시작 페이지
- `GET /login-success`
    - 로그인 성공 후 결과 화면

### 인증 관련 API

- `GET /api/me`
    - 현재 로그인 사용자 정보 반환
- `POST /api/auth/refresh`
    - refresh token 기반 토큰 재발급
- `POST /logout`
    - 로그아웃

---

## 16. 이 구조가 실무적으로 왜 유용한가

이 프로젝트 구조는 학습용이면서도 실무 구조와 꽤 가깝습니다.

이유:

- OAuth2 로그인 처리와 서비스 로직이 분리되어 있음
- JWT 발급과 검증 책임이 분리되어 있음
- Spring Security Filter Chain 안에서 인증이 일관되게 복원됨
- refresh token을 DB에 저장해 서버 제어권을 유지함
- 서버 렌더링 화면과 API 인증 구조를 동시에 연습할 수 있음

---

## 17. 현재 프로젝트에서 기억하면 좋은 핵심 포인트

- 카카오 로그인 자체는 Spring Security OAuth2 Client가 처리합니다.
- 사용자 저장은 `CustomOAuth2UserService -> AppUserService` 흐름에서 처리합니다.
- JWT 발급은 로그인 성공 핸들러에서 처리합니다.
- 요청 인증 복원은 `JwtAuthenticationFilter`가 처리합니다.
- refresh token은 DB와 쿠키를 함께 사용합니다.
- 카카오 콘솔에서 가장 중요한 값은 `REST API 키`, `Redirect URI` 입니다.

---

## 18. 참고

현재 프로젝트는 기본적으로 H2 메모리 DB를 사용하도록 설정되어 있습니다.  
즉, 애플리케이션을 재시작하면 저장된 사용자와 refresh token 정보는 초기화됩니다.

실제 서비스로 확장하려면 보통 다음을 추가합니다.

- MySQL 등 영속 DB 설정
- 예외 응답 포맷 통일
- refresh token 만료/재사용 정책 강화
- 사용자 권한(Role) 체계
- 운영용 HTTPS 및 `cookie-secure=true` 적용
