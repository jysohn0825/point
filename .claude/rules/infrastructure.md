---
paths:
  - "infrastructure/**/*"
---

# infrastructure 모듈 규칙

`domain`의 repository 포트를 구현하는 JPA 영속성 어댑터 모듈이자, 캐시/키채번/분산락 등 `support` 포트의 실제 구현체 모듈.

Docker 없이 단일 인스턴스로 돌아가야 한다는 전제로, DB는 H2(파일 기반), 캐시/분산락/분산채번은 JVM 힙 기반 인메모리 구현체를 사용한다. 각 기능은 `support`의 포트로 추상화되어 있으므로, 인스턴스를 여러 대로 확장해야 하는 시점에는 이 모듈의 구현체만(예: Redisson 기반) 교체하면 된다.

## 필수 규칙

- 의존 방향: `domain`/`support`를 `implementation`으로 의존. `spring-boot-starter-data-jpa` + H2(runtime, `com.h2database:h2`).
- 패키지 구조: `persistence/adapter`(구현체), `persistence/adapter/mapper`(엔티티↔도메인 매퍼), `persistence/entity`(JPA 엔티티), `persistence/repository`(Spring Data JPA), `event`(도메인 이벤트 리스너 — `domain`의 이벤트를 구독해 부가 기록을 영속화), `config`(`DatasourceConfig`), `lock`(`MemoryDistributedLockExecutor`, `support.lock.DistributedLockExecutor` 구현체), `cache`(`MemoryCacheExecutor`, `support.cache.CacheExecutor` 구현체 — 내부적으로 Caffeine 캐시 라이브러리 사용), `key`(`MemoryDistributedKeyGenerator`, `support.key.DistributedKeyGenerator` 구현체)

## 설계 규칙
- FK 파라미터(`walletId`, `policyId` 등)는 어댑터 `save()` 파라미터로 받아 JPA 엔티티 FK 컬럼에 채운다 ([domain.md](./domain.md) 참고).
- 엔티티↔도메인 변환(`toDomain`/`toEntity`)은 어댑터에 private 함수로 두지 않고 `persistence/adapter/mapper`의 전용 `XxxMapper` 클래스로 뺀다. 변환 방향이 여러 개면(엔티티→도메인, 도메인→엔티티, VO→엔티티 등) 모두 `of(...)`라는 이름의 오버로드로 companion object에 둔다(파라미터 타입으로 구분되므로 이름을 나눌 필요 없음).
- `event` 패키지의 리스너는 `@EventListener`(기본 동기 실행)만 사용한다. `@Async`나 `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 바꾸면 발행자의 트랜잭션과 분리되어 [application.md](./application.md)가 전제하는 "같은 트랜잭션 안에서 커밋·롤백" 보장이 깨지므로 사용하지 않는다.

## 네이밍 규칙

- 어댑터: `XxxPersistenceAdapter`, JPA 엔티티: `XxxEntity`, Spring Data 리포지토리: `XxxJpaRepository`, 매퍼: `XxxMapper`(companion object의 `of()` 정적 팩토리로만 사용, 인스턴스화하지 않음)
- 이벤트 리스너: `XxxEventListener`
- `support`의 포트 구현체는 구체 기술을 드러내지 않는 `MemoryXxx` 접두어를 쓴다(예: `MemoryDistributedLockExecutor`) — 호출부는 물론 이름만 봐서도 이게 로컬 메모리 구현인지 Redis 등 분산 구현인지 알 수 없어야 한다. 추후 실제 분산 인프라(Redis 등)로 교체하면 그때는 `RedissonXxx`처럼 기술명을 접두어로 쓴다.

## 테스트 작성 규칙

- Kotest `BehaviorSpec`(Given/When/Then), 시나리오는 한글로 작성.
- `XxxMapper`는 Spring/DB에 의존하지 않는 순수 함수이므로 `BehaviorSpec` 단위 테스트로 커버한다(가장 저비용으로 커버리지를 확보할 수 있는 지점).
- `lock`/`cache`/`key`의 `MemoryXxx` 구현체는 외부 인프라 의존이 없으므로 `BehaviorSpec` 단위 테스트로 동시성/TTL/멱등성 등의 동작을 직접 검증한다.
- `persistence/adapter`, `persistence/entity`의 JPA 연동 테스트는 `@DataJpaTest`로 내장 H2에 붙여 검증한다(Docker 불필요). Spring 컨텍스트를 띄우는 테스트라 Kotest 통합이 세팅되어 있지 않으므로 JUnit5 `@Test`로 작성한다(다른 모듈의 `BehaviorSpec` 원칙에 대한 예외 — [presentation.md](./presentation.md)의 e2e 테스트와 동일한 사유).
- `testFixtures(project(":domain"))`을 `testImplementation`으로 참조하고, `domain`의 엔티티/인터페이스는 그 테스트 픽스처와 fake 구현체를 그대로 사용한다.
- 테스트 커버리지는 80% 이상을 유지한다.
