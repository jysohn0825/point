---
paths:
  - "application/**/*"
---

# application 모듈 규칙

유스케이스(서비스) 계층.

## 필수 규칙

- 의존 방향: `domain`의 repository 포트만 바라보고, `infrastructure` 구현에는 의존하지 않는다.
- 포인트 변경은 반드시 `PointWallet`을 통해서만 이루어진다. 서비스에서 원장 엔티티의 잔액을 직접 조작하지 않는다.
- 지갑 조작과 그 결과로 생기는 원장(`PointEarning`/`PointUsage`) 생성은 같은 트랜잭션·같은 서비스 메서드 안에서 처리한다 (지갑 버전 충돌 시 전체 롤백되어 보유한도 위반을 막음).

## 설계 규칙

- FK 값(`walletId`, `policyId`)은 서비스가 로드한 `PointWallet`/`PointPolicy` 인스턴스에서 그대로 꺼내(`wallet.id`) repository `save()` 호출에 전달한다. 별도 조회나 도메인 이벤트로 우회하지 않는다.
- 서비스 로직이 두꺼워지거나(판단/분기 로직이 늘어나면) 그 로직이 실은 도메인 서비스로 가야 하는 건 아닌지 점검한다. 서비스는 오케스트레이션(조회 → 도메인 호출 → 저장)만 담당하고, 비즈니스 판단은 도메인에 둔다.

## 네이밍 규칙

- `~Service` 접미사를 사용한다 (예: `EarnPointService`).

## 테스트 작성 규칙

- Kotest `BehaviorSpec`(Given/When/Then), 시나리오는 한글로 작성.
- `domain`의 엔티티/인터페이스를 참조할 때는 `domain`의 테스트 픽스처와 fake 구현체를 그대로 사용한다.
