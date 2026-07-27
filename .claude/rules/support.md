---
paths:
  - "support/**/*"
---

# support 모듈 규칙

포인트 도메인 로직과 무관한 기술 공통 기능(캐싱, 분산 환경 키 채번, 동시 요청 직렬화를 위한 분산락)의 포트와 그 AOP 구현을 담는 모듈.

## 필수 규칙

- 의존 방향: `domain`에는 의존하지 않는다(포인트 비즈니스 개념을 모른 채로 재사용 가능해야 한다). `domain`과 달리 Spring(`spring-context`, `spring-boot-starter-aop`) 의존은 허용한다.
- 이 모듈에는 포트 인터페이스와 애너테이션·AOP(`@Aspect`)까지만 두고, 실제 외부 인프라(Redis 등) 연동 구현체는 두지 않는다. 구현체는 `infrastructure`에 둔다([infrastructure.md](./infrastructure.md) 참고).
- 포인트 엔티티/값 객체 등 도메인 개념을 이 모듈에 끌어들이지 않는다. 키 문자열, TTL, 락 이름 같은 순수 기술 파라미터만 다룬다.

## 패키지 구조

- `lock`: `DistributedLockExecutor`(포트) + `LockAcquisitionException`, `@DistributedLock` 애너테이션, `DistributedLockAspect`(AOP 구현체)
- `cache`: `CacheExecutor`(포트)
- `key`: `DistributedKeyGenerator`(포트)

## 설계 규칙

- 포트 인터페이스는 기능(행위) 단위로 하나씩 두고, 여러 기술 공통 기능을 하나의 인터페이스에 섞지 않는다(`DistributedLockExecutor`/`CacheExecutor`/`DistributedKeyGenerator` 분리 유지).
- `@DistributedLock`처럼 선언적 사용이 필요한 기능은 애너테이션 + `@Aspect`로 제공하고, `CacheExecutor`/`DistributedKeyGenerator`처럼 호출부에서 명시적으로 다루는 게 자연스러운 기능은 포트를 직접 주입받아 쓰는 방식으로 제공한다. 새 기능을 추가할 때도 이 판단 기준을 따른다.

## 네이밍 규칙

- 포트: `XxxExecutor`(락/캐시처럼 "실행을 위임"하는 성격), `XxxGenerator`(채번처럼 "값을 생성"하는 성격)로 구분한다.
- 구현체는 이 모듈에 두지 않으므로 네이밍 규칙은 [infrastructure.md](./infrastructure.md)를 따른다(현재는 `MemoryXxx` 접두어).

## 테스트 작성 규칙

- 순수 로직(SpEL 파싱 등)이 아니라 Spring AOP 컨텍스트를 직접 띄워 애너테이션 동작을 검증하는 통합 성격의 테스트(`DistributedLockAspectTest`)는 Kotest `FunSpec`을 사용한다(다른 모듈의 `BehaviorSpec` 원칙에 대한 예외).
- 포트 인터페이스 자체는 로직이 없으므로 별도 테스트를 두지 않는다. 실제 동작 검증은 `infrastructure`의 구현체 테스트(`*ContainerTest`)에서 한다.
- 테스트 커버리지는 80% 이상을 유지한다.
