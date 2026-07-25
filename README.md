# 무료 포인트 시스템 (Point API)

무료 포인트의 적립/적립취소/사용/사용취소를 관리하는 REST API 과제입니다.

## 산출물 체크리스트

| # | 항목 | 필수/옵션 | 위치 | 상태 |
|---|------|-----------|------|------|
| 1 | 무료 포인트 시스템 API 개발 | 필수 | GitHub 저장소 | ⬜ |
| 2 | ERD (PDF/이미지) | 필수 | `resource/` | ⬜ |
| 3 | AWS 아키텍처 구성도 (PDF/이미지) | 옵션 | `resource/` | ⬜ |
| 4 | 빌드 방법 및 과제 설명 | 필수 | `README.md` | ⬜ (본 문서) |

## 개발 환경

| 항목 | 버전                                    |
|------|---------------------------------------|
| 언어 | Kotlin 1.9.25 |
| 프레임워크 | Spring Boot 3.5.7                     |
| DB | MySQL 8.0 (Docker Compose)             |
| 캐시 / 분산 락 | Redis 7 (Docker Compose)         |
| 빌드 도구 | Gradle (Kotlin DSL)                   |

---

## 개선 및 고려 사항

---

## 빌드 및 실행 방법

MySQL, Redis 등 로컬 환경 차이로 인한 이슈를 피하기 위해, 로컬 빌드/실행은 **반드시 Docker Compose로 인프라(MySQL, Redis)를 띄운 뒤** 진행합니다.

### 0. 사전 준비

- **(필수) Docker / Docker Compose 설치** ([Docker Desktop](https://www.docker.com/products/docker-desktop/) 등). 로컬 실행(`bootRun`)과 테스트(`test`) 모두 Docker 없이는 동작하지 않습니다.
- 실행(`./gradlew :presentation:bootRun`)은 아래 1번의 `docker compose up -d`로 MySQL/Redis를 먼저 띄워야 합니다.
- 테스트(`./gradlew test`)는 Testcontainers가 MySQL 컨테이너를 자동으로 띄우므로, 별도의 docker-compose 실행 없이 **Docker 데몬만 실행 중이면** 됩니다.
- Docker Desktop 대신 [Colima](https://github.com/abiosoft/colima)/OrbStack 등을 쓰는 경우에도 별도 설정이 필요 없습니다. `:infrastructure:test`가 실행 시점에 현재 활성화된 `docker context`를 조회해 `DOCKER_HOST`/`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`를 자동으로 맞춰줍니다.

### 1. 인프라 실행 (MySQL + Redis)

    docker compose up -d

- MySQL: `localhost:3306` (DB `point` / 계정 `point` / 비밀번호 `point`)
- Redis: `localhost:6379`
- MySQL 컨테이너는 최초 실행 시 `infrastructure/src/test/resources/schema.sql`을 이용해 스키마를 자동 생성합니다.
- 접속 정보를 바꾸고 싶다면 리포지토리 루트에 `.env` 파일을 만들어 `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`, `DB_NAME`, `REDIS_PORT` 등을 오버라이드할 수 있습니다. (`docker-compose.yml` 참고)

인프라 상태 확인 / 종료:

    # 상태 확인
    docker compose ps

    # 로그 확인
    docker compose logs -f mysql redis

    # 종료 (데이터 유지)
    docker compose down

    # 종료 + 데이터 삭제
    docker compose down -v

### 2. 빌드 / 테스트 / 실행

    # 빌드
    ./gradlew build

    # 테스트 (Testcontainers가 MySQL을 자동 기동 — Docker 데몬 필요)
    ./gradlew test

    # 로컬 실행 (presentation 모듈, 1번의 docker compose가 먼저 실행되어 있어야 함)
    ./gradlew :presentation:bootRun

---

## 기능 요구 사항

### 적립

- [ ] 1회 적립 가능 포인트는 1포인트 이상, 10만포인트 이하이며, 1회 최대 적립 가능 포인트는 하드코딩이 아닌 방법으로 제어할 수 있다.
- [ ] 개인별로 보유 가능한 무료포인트의 최대 금액 제한이 존재하며, 하드코딩이 아닌 별도의 방법으로 변경할 수 있다.
- [ ] 특정 시점에 적립된 포인트는 1원 단위까지 어떤 주문에서 사용되었는지 추적할 수 있다.
- [ ] 포인트 적립은 관리자가 수기로 지급할 수 있으며, 수기 지급한 포인트는 다른 적립과 구분되어 식별할 수 있다.
- [ ] 모든 포인트는 만료일이 존재하며, 최소 1일 이상 최대 5년 미만의 만료일을 부여할 수 있다. (기본 365일)

### 적립 취소

- [ ] 특정 적립 행위에서 적립한 금액만큼 취소 가능하며, 적립한 금액 중 일부가 사용된 경우라면 적립 취소될 수 없다.

### 사용

- [ ] 주문 시에만 포인트를 사용할 수 있다고 가정한다.
- [ ] 포인트 사용 시에는 주문번호를 함께 기록하여 어떤 주문에서 얼마의 포인트를 사용했는지 식별할 수 있다.
- [ ] 포인트 사용 시에는 관리자가 수기 지급한 포인트가 우선 사용되어야 하며, 만료일이 짧게 남은 순서로 사용해야 한다.

### 사용 취소

- [ ] 사용한 금액 중 전체 또는 일부를 사용취소할 수 있다.
- [ ] 사용취소 시점에 이미 만료된 포인트를 사용취소해야 한다면 그 금액만큼 신규 적립 처리한다.
