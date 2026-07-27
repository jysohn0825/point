# Point API — 프로젝트 지침

무료 포인트 시스템(적립/적립취소/사용/사용취소)을 다루는 Kotlin + Spring Boot REST API 과제. 상세 기능 요구사항은 `README.md`의 "기능 요구 사항" 섹션을 따른다.

## 아키텍처

Gradle 멀티모듈, 의존 방향은 아래로 고정한다. 새 코드를 추가할 때 이 방향을 거스르지 않는다 

- `domain` — 순수 Kotlin, 프레임워크 의존성 없음. 애그리거트/값 객체/repository 포트.
- `support` — 포인트 도메인과 무관한 기술 공통 기능(캐싱, 분산 키 채번, 동시 요청 직렬화를 위한 분산락)의 포트 + Spring AOP 구현. `domain`에 의존하지 않는다.
- `application` — 서비스 계층. `domain`의 포트, `support`의 포트/애너테이션만 의존.
- `infrastructure` — JPA 영속성 어댑터이자 `support`의 포트(캐시/키 채번/분산락) 구현체 모듈. `domain`의 repository 인터페이스를 구현하며, DB는 H2(파일 기반), 캐시/키채번/분산락은 Docker 없이 동작하는 인메모리 구현체를 사용한다(다중 인스턴스로 확장할 경우 이 구현체만 Redis 등으로 교체).
- `presentation` — Spring Boot 진입점. `domain`/`application`/`support`는 `implementation`, `infrastructure`는 `runtimeOnly`로만 참조. `support`는 `LockAcquisitionException` 등 예외를 `GlobalExceptionHandler`에서 HTTP 상태로 매핑하기 위해 참조한다.

각 모듈의 세부 규칙(설계 패턴, 네이밍, 테스트 컨벤션)은 `.claude/rules/<module>.md`에 있으며, 해당 모듈 하위 파일을 열면 자동으로 로드된다.

## 코딩 표준

- **테스트**: Kotest `BehaviorSpec`(Given/When/Then 스타일)을 전 모듈에서 사용하고, 시나리오 설명은 한글로 작성한다.
- **정적 분석**: ktlint(`org.jlleitschuh.gradle.ktlint`)가 전 서브프로젝트에 적용되어 있다. 커밋 전 포맷을 어기지 않도록 주의한다.
- **커버리지**: 모든 서브프로젝트에서 `check`가 `jacocoTestReport`에 의존하도록 설정되어 있다.
- **확장 함수 금지**: 확장 함수(extension function)로 만들지 않는다.
- **타입 명시**: 변수에 값을 할당할 때 타입을 반드시 명시한다 (타입 추론에 맡기지 않는다).
- **Named argument**: 메서드 호출 시 인자는 이름을 명시해서 전달한다 (예: `test(a = a)`).

## 빌드 / 테스트

```
./gradlew build          # 전체 빌드
./gradlew test           # 전체 테스트
./gradlew :presentation:bootRun   # 로컬 실행
```

DB는 H2(파일 기반), 캐시/분산락/분산채번은 인메모리 구현체를 쓰므로 Docker 없이 빌드/테스트/실행할 수 있다.

## 커밋 컨벤션

모든 git commit 메시지는 AngularJS 커밋 컨벤션(`<type>(<scope>): <subject>`)을 따른다. 상세 규칙: [`.claude/rules/commit-convention.md`](.claude/rules/commit-convention.md)
