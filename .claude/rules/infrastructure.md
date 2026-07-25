---
paths:
  - "infrastructure/**/*"
---

# infrastructure 모듈 규칙

`domain`의 repository 포트를 구현하는 JPA 영속성 어댑터 모듈.

## 필수 규칙

- 의존 방향: `domain`을 `implementation`으로 의존. `spring-boot-starter-data-jpa` + H2(runtime).
- 패키지 구조: `persistence/adapter`(구현체), `persistence/entity`(JPA 엔티티), `persistence/repository`(Spring Data JPA)

## 설계 규칙
- FK 파라미터(`walletId`, `policyId` 등)는 어댑터 `save()` 파라미터로 받아 JPA 엔티티 FK 컬럼에 채운다 ([domain.md](./domain.md) 참고).

## 네이밍 규칙

- 어댑터: `XxxPersistenceAdapter`, JPA 엔티티: `XxxEntity`, Spring Data 리포지토리: `XxxJpaRepository`

## 테스트 작성 규칙

- Kotest `BehaviorSpec`(Given/When/Then), 시나리오는 한글로 작성.
- 컨테이너 기반 테스트(`*ContainerTest`)는 Testcontainers(MySQL) 사용, 실행 전 Docker 필요.
- `testFixtures(project(":domain"))`을 `testImplementation`으로 참조하고, `domain`의 엔티티/인터페이스는 그 테스트 픽스처와 fake 구현체를 그대로 사용한다.
