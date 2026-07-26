---
paths:
  - "application/**/*"
---

# application 모듈 규칙

유스케이스(서비스) 계층.

## 필수 규칙

- 의존 방향: `domain`의 repository 포트, `support`의 포트/애너테이션(`@DistributedLock` 등)만 바라보고, `infrastructure` 구현에는 의존하지 않는다.
- 포인트 변경은 반드시 `PointWallet`을 통해서만 이루어진다. 서비스에서 원장 엔티티의 잔액을 직접 조작하지 않는다.
- 지갑 조작과 그 결과로 생기는 원장(`PointEarning`/`PointUsage`) 생성은 같은 트랜잭션·같은 서비스 메서드 안에서 처리한다 (지갑 버전 충돌 시 전체 롤백되어 보유한도 위반을 막음).

## 설계 규칙

- FK 값(`walletId`, `policyId`)은 서비스가 로드한 `PointWallet`/`PointPolicy` 인스턴스에서 그대로 꺼내(`wallet.id`) repository `save()` 호출에 전달한다. 별도 조회나 도메인 이벤트로 우회하지 않는다. 이 원칙은 지갑/원장(적립·사용) 저장에 한정된다 — 정합성 불변식이 없는 부가 기록(히스토리 등)은 도메인 이벤트로 분리해 기록할 수 있다. 이벤트는 항상 동기로 처리되어 발행자와 같은 트랜잭션 안에서 커밋·롤백을 공유하므로("같은 트랜잭션·같은 서비스 메서드" 원칙 자체는 유지된다), 서비스는 지갑/원장 저장을 마친 뒤 이벤트를 발행하기만 하고 구독자가 무엇을 하는지는 알지 못한다.
- 서비스 로직이 두꺼워지거나(판단/분기 로직이 늘어나면) 그 로직이 실은 도메인 서비스로 가야 하는 건 아닌지 점검한다. 서비스는 오케스트레이션(조회 → 도메인 호출 → 저장)만 담당하고, 비즈니스 판단은 도메인에 둔다.
- 배치/스케줄러의 전체 실행 단위(예: `expireAllDue()`)와 그것이 walletId 단위로 위임하는 단일 처리 메서드(예: `expireWalletEarnings()`)는 별도 클래스로 분리하지 않고 같은 서비스 안에 둔다. 단, 전체 실행 메서드에서 단일 처리 메서드를 호출할 때는 `@Transactional` self-invocation으로 프록시(락 AOP 포함)를 건너뛰지 않도록 반드시 `public` 메서드 호출 경로를 유지한다.

## 패키지 구조

- 유스케이스 서비스는 기능별로 패키지를 나누지 않고 전부 `service` 패키지에 평평하게 둔다(예: `service/EarnPointService.kt`, `service/UsePointService.kt`, `service/PointWalletService.kt`, `service/PointEarningExpirationService.kt`).
- 서비스 입출력 DTO는 `service/dto` 패키지에 둔다.
- `@DistributedLock` 애너테이션과 `DistributedLockAspect`(AOP 구현체)는 이 모듈이 아니라 `support`의 `lock` 패키지에 있다([support.md](./support.md) 참고). 서비스에서는 `import`해서 사용만 한다.

## 네이밍 규칙

- 서비스: `~Service` 접미사를 사용한다 (예: `EarnPointService`). 단건 조회든 여러 유스케이스를 함께 다루든 `~QueryService`처럼 세분화하지 않고 애그리거트 단위로 하나의 `~Service`에 모은다 (예: `PointWalletService`).
- DTO: `~Dto` 접미사를 사용한다 (예: `EarnPointDto`, `CancelUsagePointResultDto`). 커맨드/결과를 구분해야 할 때는 `~ResultDto`처럼 접미사를 덧붙인다.

## 테스트 작성 규칙

- Kotest `BehaviorSpec`(Given/When/Then), 시나리오는 한글로 작성.
- 테스트 파일은 대상 서비스와 동일한 패키지(`service`)에 둔다.
- `domain`의 repository 포트를 fake로 대체할 때는 파일마다 새로 만들지 않고 `domain`의 `testFixtures`에 있는 `FakeXxxRepository`(예: `FakePointWalletRepository`)를 그대로 가져다 쓴다([domain.md](./domain.md) 참고). `seed()`/`save()` 등으로 상태를 준비하고, 잔액처럼 엔티티가 in-place로 변경되는 값은 repository를 거치지 않고 테스트가 들고 있는 엔티티 참조로 직접 검증한다.
- `domain` 인터페이스가 아닌 `support`의 포트(`DistributedKeyGenerator` 등)는 서비스마다 필요한 동작이 다를 수 있으므로 테스트 파일별로 fake를 정의한다. 이때 파일마다 정의하는 fake 클래스명이 서로 충돌하지 않도록 접두어를 붙여 구분한다(예: `EarnPointServiceTest`의 `FakeEarnKeyGenerator` vs `UsePointServiceTest`의 `FakeUseKeyGenerator`). Kotlin top-level `private` 클래스는 파일 스코프만 가릴 뿐 클래스 파일명은 그대로 노출되므로, 같은 패키지에서 동일 이름을 재선언하면 컴파일 에러가 난다.
- 테스트 커버리지는 80% 이상을 유지한다.
