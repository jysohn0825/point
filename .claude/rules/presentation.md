---
paths:
  - "presentation/**/*"
---

# presentation 모듈 규칙

Spring Boot 진입점(`PointApiApplication`).

## 필수 규칙

- 의존 방향: `domain`/`application`은 `implementation`, `infrastructure`는 `runtimeOnly`. `infrastructure` 클래스를 직접 import하지 않는다.
- 패키지 구조: `controller`(일반 API), `admin`(관리자 API), `dto/request`·`dto/response`, `exception`(`GlobalExceptionHandler`), `config`

## 설계 규칙

- 모든 예외는 `GlobalExceptionHandler`에서 중앙 처리하고 `ErrorResponse`로 응답한다. 컨트롤러에서 개별 try/catch를 하지 않는다.
  - `PointDomainException`/`PointBusinessException`/`IllegalArgumentException` → 400, 그 외 `Exception` → 500
- 도메인 엔티티/값 객체를 컨트롤러 밖으로 그대로 노출하지 않고 DTO로 변환한다.

## 네이밍 규칙

- 컨트롤러: `XxxController`(일반), `AdminXxxController`(관리자)
- DTO: 요청은 `XxxRequest`, 응답은 `XxxResponse`

## 테스트 작성 규칙

- Kotest `BehaviorSpec`(Given/When/Then), 시나리오는 한글로 작성.
- 시나리오 기반 e2e 테스트로 작성한다.
