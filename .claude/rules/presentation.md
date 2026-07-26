---
paths:
  - "presentation/**/*"
---

# presentation 모듈 규칙

Spring Boot 진입점(`PointApiApplication`).

## 필수 규칙

- 의존 방향: `domain`/`application`/`support`는 `implementation`, `infrastructure`는 `runtimeOnly`. `infrastructure` 클래스를 직접 import하지 않는다. `support`는 `LockAcquisitionException`을 `GlobalExceptionHandler`에서 HTTP 상태로 매핑하는 용도로만 참조한다. `org.springframework:spring-tx`는 `DataIntegrityViolationException`을 같은 용도로 매핑하기 위해 별도로 추가했다(JPA 자체는 `infrastructure`에만 있고 `runtimeOnly`).
- 패키지 구조: `controller`(일반 API + 관리자 API 모두 포함, 관리자 컨트롤러는 별도 패키지로 빼지 않고 `Admin~Controller` 네이밍으로만 구분), `controller/dto/request`·`controller/dto/response`, `exception`(`GlobalExceptionHandler`, `ErrorResponse`), `scheduler`(`@Scheduled` 진입점), `config`(`OpenApiConfig` 등 프레젠테이션 전용 설정)

## 설계 규칙

- 모든 예외는 `GlobalExceptionHandler`에서 중앙 처리하고 `ErrorResponse`로 응답한다. 컨트롤러에서 개별 try/catch를 하지 않는다.
  - `PointDomainException`/`IllegalArgumentException`/`MethodArgumentNotValidException`(`@Valid` 검증 실패)/`HttpMessageNotReadableException`(요청 본문 파싱 실패) → 400
  - `LockAcquisitionException`(동일 key에 대한 진짜 동시(in-flight) 요청이라 분산락 획득에 실패한 경우) → 409
  - `DataIntegrityViolationException`(이미 끝난 요청의 재시도가 DB 유니크 제약 위반으로 커밋에 실패한 경우, 예: `uk_usage_order`) → 409
  - 그 외 `Exception` → 500
- 도메인 엔티티/값 객체를 컨트롤러 밖으로 그대로 노출하지 않고 DTO로 변환한다.
- 도메인 ↔ 응답 DTO 변환은 별도의 `mapper` 패키지를 두지 않고, 응답 DTO 자신의 companion object `of()` 팩토리로 처리한다(단건/리스트 오버로드 포함). 요청 DTO는 반대 방향으로 자기 자신에 `to(...)` 메서드를 둬서 application DTO로 변환한다.
- 중첩 응답 항목(사용 라인, 취소 라인 등)은 별도 top-level 파일로 빼지 않고 부모 응답 DTO 안에 nested data class로 둔다(예: `PointUsageResponse.UsageLineResponse`).

## 네이밍 규칙

- 컨트롤러: `XxxController`(일반), `AdminXxxController`(관리자) — 패키지는 동일하게 `controller`
- DTO: 요청은 `XxxRequest`, 응답은 `XxxResponse`

## 테스트 작성 규칙

- Spring 컨텍스트를 띄우는 테스트(`@SpringBootTest` 기반 e2e)는 Kotest와의 통합이 세팅되어 있지 않으므로 JUnit5 `@Test`로 작성한다(`infrastructure`의 `*ContainerTest`와 동일한 관례). 그 외 순수 Kotlin 단위 테스트는 Kotest `BehaviorSpec`(Given/When/Then)을 쓴다. 시나리오는 한글로 작성.
- 시나리오 기반 e2e 테스트로 작성한다: 실제 컨트롤러 + 실제 application 서비스를 그대로 띄우고, `domain`의 repository 포트와 `DistributedLockExecutor`만 fake 구현체로 대체한 뒤 MockMvc로 HTTP 요청·응답(상태 코드, JSON, `GlobalExceptionHandler` 처리 결과)을 검증한다. 서비스 계층을 Mock 프레임워크로 목킹하지 않는다(다른 모듈과 동일하게 fake 우선). repository 포트의 fake는 이 모듈에서 새로 만들지 않고 `domain`의 `testFixtures`에 있는 `FakeXxxRepository`를 `PresentationTestConfig`의 `@Bean`으로 등록해 재사용한다([domain.md](./domain.md) 참고). `DistributedLockExecutor`/`DistributedKeyGenerator`처럼 `domain` 인터페이스가 아닌 fake만 `presentation/support`에 둔다.
- `infrastructure`는 컴파일 의존이 아니지만(`runtimeOnly`) 실행 시 클래스패스에는 그대로 존재하고, Spring Boot 자동설정(JPA/Redisson)은 컴포넌트 스캔 범위가 아니라 클래스패스 존재 여부로 동작하므로 `scanBasePackages`를 좁히는 것만으로는 실제 DB/Redis 연결을 막을 수 없다. e2e 테스트용 부트 클래스에서 `DataSourceAutoConfiguration`/`HibernateJpaAutoConfiguration`/`JpaRepositoriesAutoConfiguration`/`RedissonAutoConfigurationV2`를 명시적으로 `exclude`해야 한다.
- 테스트 커버리지는 80% 이상을 유지한다.
