---
paths:
  - "infrastructure/**/*"
---

# infrastructure 모듈 규칙

`domain`의 repository 포트를 구현하는 JPA 영속성 어댑터 모듈이자, Redisson 등 외부 인프라 연동 구현체 모듈.

## 필수 규칙

- 의존 방향: `domain`/`support`를 `implementation`으로 의존. `spring-boot-starter-data-jpa` + MySQL(runtime, `mysql-connector-j`).
- 패키지 구조: `persistence/adapter`(구현체), `persistence/adapter/mapper`(엔티티↔도메인 매퍼), `persistence/entity`(JPA 엔티티), `persistence/repository`(Spring Data JPA), `event`(도메인 이벤트 리스너 — `domain`의 이벤트를 구독해 부가 기록을 영속화), `config`(`DatasourceConfig`, `RedissonConfig`), `lock`(`RedissonDistributedLockExecutor`, `support.lock.DistributedLockExecutor` 구현체), `cache`(`RedissonCacheExecutor`, `support.cache.CacheExecutor` 구현체), `key`(`RedissonDistributedKeyGenerator`, `support.key.DistributedKeyGenerator` 구현체)

## 설계 규칙
- FK 파라미터(`walletId`, `policyId` 등)는 어댑터 `save()` 파라미터로 받아 JPA 엔티티 FK 컬럼에 채운다 ([domain.md](./domain.md) 참고).
- 엔티티↔도메인 변환(`toDomain`/`toEntity`)은 어댑터에 private 함수로 두지 않고 `persistence/adapter/mapper`의 전용 `XxxMapper` 클래스로 뺀다. 변환 방향이 여러 개면(엔티티→도메인, 도메인→엔티티, VO→엔티티 등) 모두 `of(...)`라는 이름의 오버로드로 companion object에 둔다(파라미터 타입으로 구분되므로 이름을 나눌 필요 없음).
- `event` 패키지의 리스너는 `@EventListener`(기본 동기 실행)만 사용한다. `@Async`나 `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 바꾸면 발행자의 트랜잭션과 분리되어 [application.md](./application.md)가 전제하는 "같은 트랜잭션 안에서 커밋·롤백" 보장이 깨지므로 사용하지 않는다.

## 네이밍 규칙

- 어댑터: `XxxPersistenceAdapter`, JPA 엔티티: `XxxEntity`, Spring Data 리포지토리: `XxxJpaRepository`, 매퍼: `XxxMapper`(companion object의 `of()` 정적 팩토리로만 사용, 인스턴스화하지 않음)
- 이벤트 리스너: `XxxEventListener`

## 테스트 작성 규칙

- Kotest `BehaviorSpec`(Given/When/Then), 시나리오는 한글로 작성.
- `XxxMapper`는 Spring/DB에 의존하지 않는 순수 함수이므로 `BehaviorSpec` 단위 테스트로 커버한다(가장 저비용으로 커버리지를 확보할 수 있는 지점).
- 컨테이너 기반 테스트(`*ContainerTest`)는 MySQL/Redis 둘 다 Docker가 있어야 실행된다는 공통점이 있지만, 컨테이너를 띄우는 주체가 다르다.
  - MySQL: Testcontainers(`MySqlTestContainer`, JVM 싱글턴으로 1회만 기동)가 자체적으로 컨테이너를 띄운다. Docker 데몬만 있으면 되고 별도 준비 불필요.
  - Redis: 별도 컨테이너를 띄우지 않고, `docker-compose.yml`이 띄운 `localhost:6379`(prod와 동일한 인스턴스)에 직접 연결한다. 실행 전 리포지토리 루트에서 `docker compose up -d`가 되어 있어야 한다.
  - 이 저장소의 실행 환경에 따라 Docker에 도달하지 못해 로컬에서 못 돌리는 경우가 있을 수 있는데, 이는 코드 결함이 아니라 환경 제약이다.
- `testFixtures(project(":domain"))`을 `testImplementation`으로 참조하고, `domain`의 엔티티/인터페이스는 그 테스트 픽스처와 fake 구현체를 그대로 사용한다.
- 테스트 커버리지는 80% 이상을 유지한다.
