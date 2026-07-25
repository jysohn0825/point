# 커밋 컨벤션

모든 git commit 메시지는 저장소나 기존 커밋 히스토리의 스타일과 무관하게 항상 AngularJS 커밋 컨벤션을 따른다. 기존 커밋 로그가 다른 스타일(예: 단순 설명형, Conventional Commits 변형 등)을 쓰고 있어도 이 컨벤션을 우선한다.

## 메시지 구조

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

- header(`<type>(<scope>): <subject>`)는 필수, body와 footer는 변경 내용에 따라 선택.
- header는 100자를 넘기지 않는다.

## type (필수)

다음 중 하나만 사용한다:

- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 변경만 있는 경우
- `style`: 코드 동작에 영향 없는 포맷팅, 세미콜론 등 (공백, 정렬 등)
- `refactor`: 버그 수정도 기능 추가도 아닌 코드 변경
- `perf`: 성능 개선을 위한 코드 변경
- `test`: 누락된 테스트 추가 또는 기존 테스트 수정
- `build`: 빌드 시스템이나 외부 의존성에 영향을 주는 변경 (예: gradle, npm)
- `ci`: CI 설정 파일/스크립트 변경
- `chore`: 기타 잡무성 변경 (src나 test 파일을 건드리지 않는 유지보수)
- `revert`: 이전 커밋을 되돌리는 경우. subject는 `revert: <되돌리는 커밋의 header>` 형태로 쓰고, body에 `This reverts commit <hash>.` 를 남긴다.

## scope (선택)

변경 영향 범위를 괄호 안에 명시한다 (예: `auth`, `point-service`, `config`). 범위를 특정하기 애매하면 생략하고 `<type>: <subject>` 형태로 쓴다.

## subject (필수)

- 명령형, 현재 시제 사용: "change"이지 "changed"나 "changes"가 아니다.
- 첫 글자 대문자로 시작하지 않는다.
- 끝에 마침표(`.`)를 찍지 않는다.
- 무엇을 왜 바꿨는지 간결하게 요약한다.

## body (선택)

- 명령형, 현재 시제 사용.
- 이전 동작과 대비하여 변경의 동기(motivation)를 설명한다.
- 무엇을(what) 보다 왜(why)와 어떻게(how)에 집중한다.

## footer (선택)

- **Breaking Change**: 호환성이 깨지는 변경은 `BREAKING CHANGE:` 로 시작하는 줄에 설명한다.
- **이슈 참조**: 관련 이슈를 닫을 때 `Closes #123`, `Closes #123, #456` 형태로 남긴다.

## 예시

```
feat(point): add point expiration batch job

Add a scheduled job that expires unused points after 12 months,
since the current implementation never removes stale point balances.

Closes #42
```

```
fix(auth): prevent duplicate login token issuance
```

```
refactor: extract point calculation into PointCalculator

BREAKING CHANGE: PointService.calculate() no longer accepts a raw Map;
use PointCalculator.Request instead.
```

## 적용 시 주의

- 사용자가 커밋 메시지를 직접 지정하지 않는 한, 커밋을 생성하기 전에 이 컨벤션에 맞춰 메시지를 구성한다.
- 여러 타입에 걸친 변경이라면 가장 지배적인 목적(type) 하나를 고른다. 커밋을 쪼개는 것이 더 적절하다면 사용자에게 제안한다.
- 기존 저장소의 커밋 로그가 다른 컨벤션을 쓰고 있더라도 새 커밋에는 이 규칙을 적용한다.
