---
paths:
  - "infrastructure/**/*"
---

# infrastructure 모듈 규칙

`domain`의 repository 포트를 구현하는 JPA 영속성 어댑터 모듈이자, Redisson 등 외부 인프라 연동 구현체 모듈.

## 필수 규칙

- 의존 방향: `domain`을 `implementation`으로 의존. `spring-boot-starter-data-jpa` + MySQL(runtime, `mysql-connector-j`).
- 패키지 구조: `persistence/adapter`(구현체), `persistence/adapter/mapper`(엔티티↔도메인 매퍼), `persistence/entity`(JPA 엔티티), `persistence/repository`(Spring Data JPA), `config`(`DatasourceConfig`, `RedissonConfig`), `lock`(`RedissonDistributedLockExecutor`, `domain.lock.DistributedLockExecutor` 구현체)

## 설계 규칙
- FK 파라미터(`walletId`, `policyId` 등)는 어댑터 `save()` 파라미터로 받아 JPA 엔티티 FK 컬럼에 채운다 ([domain.md](./domain.md) 참고).
- 엔티티↔도메인 변환(`toDomain`/`toEntity`)은 어댑터에 private 함수로 두지 않고 `persistence/adapter/mapper`의 전용 `XxxMapper` 클래스로 뺀다. 변환 방향이 여러 개면(엔티티→도메인, 도메인→엔티티, VO→엔티티 등) 모두 `of(...)`라는 이름의 오버로드로 companion object에 둔다(파라미터 타입으로 구분되므로 이름을 나눌 필요 없음).

## 네이밍 규칙

- 어댑터: `XxxPersistenceAdapter`, JPA 엔티티: `XxxEntity`, Spring Data 리포지토리: `XxxJpaRepository`, 매퍼: `XxxMapper`(companion object의 `of()` 정적 팩토리로만 사용, 인스턴스화하지 않음)

## 테스트 작성 규칙

- Kotest `BehaviorSpec`(Given/When/Then), 시나리오는 한글로 작성.
- `XxxMapper`는 Spring/DB에 의존하지 않는 순수 함수이므로 `BehaviorSpec` 단위 테스트로 커버한다(가장 저비용으로 커버리지를 확보할 수 있는 지점).
- 컨테이너 기반 테스트(`*ContainerTest`)는 Testcontainers(MySQL) 사용, 실행 전 Docker 필요. 이 저장소의 실행 환경에 따라 Docker에 도달하지 못해 로컬에서 못 돌리는 경우가 있을 수 있는데, 이는 코드 결함이 아니라 환경 제약이다.
- `testFixtures(project(":domain"))`을 `testImplementation`으로 참조하고, `domain`의 엔티티/인터페이스는 그 테스트 픽스처와 fake 구현체를 그대로 사용한다.
- 테스트 커버리지는 80% 이상을 유지한다.
