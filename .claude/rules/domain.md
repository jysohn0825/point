---
paths:
  - "domain/**/*"
---

# domain 모듈 규칙

포인트 도메인 로직을 담는 순수 Kotlin 모듈.

## 필수 규칙

- 의존 방향: 어떤 프레임워크(Spring, JPA 등)에도 의존하지 않는다. `java-library` + `java-test-fixtures`만 적용.
- 패키지 구조: `entity`(애그리거트), `vo`(값 객체), `repository`(포트 인터페이스), `exception`(도메인 예외)
- 잔액 변경은 `PointWallet`을 경유한다. 원장(`PointEarning`/`PointUsage`)을 직접 수정해 잔액을 바꾸지 않는다.

## 설계 규칙

- FK 전용 값(`walletId`, `policyId` 등)은 도메인 로직이 실제로 쓰지 않으면 엔티티 필드로 두지 않고 repository `save()` 파라미터로 전달한다.
- ID/FK 타입은 값 객체로 감싸지 않고 plain `String`으로 통일한다.
- 생성 팩토리(`earn()`/`use()`)와 영속성 복원 팩토리(`reconstitute()`)를 분리한다. 복원 팩토리는 생성 시점 불변식을 재검증하지 않는다.

## 네이밍 규칙

- 영속성 복원 팩토리는 `reconstitute`로 고정한다 (`restore`는 `restoreUsage()`와 의미 충돌).
- 도메인 인터페이스(repository 등)의 메서드명에는 파라미터가 드러나지 않게 하고, 행위 자체를 의미하는 이름으로 짓는다 (예: `findByWalletIdAndStatus`가 아니라 `findRedeemable`, 파라미터는 시그니처로만 전달).

## 테스트 작성 규칙

- Kotest `BehaviorSpec`(Given/When/Then), 시나리오는 한글로 작성.
- `testFixtures`: 값 객체는 소문자 팩토리 함수(`pointAmount()` 등), 엔티티는 `XxxFixture.kt` 헬퍼.
- 테스트 커버리지는 95% 이상을 유지한다.
- 테스트 픽스처를 먼저 만들고, 그 픽스처를 기반으로 테스트를 작성한다.
- repository 등 인터페이스는 mocking하지 않고 fake 구현체를 만들어 테스트에 사용한다.
