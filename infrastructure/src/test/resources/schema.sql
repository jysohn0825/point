CREATE TABLE point_policy (
    id                        CHAR(36)    NOT NULL                COMMENT '정책 ID',
    policy_version            INT         NOT NULL                COMMENT '정책 버전 (증가)',
    max_earn_per_transaction  INT         NOT NULL                COMMENT '1회 적립 상한 (1~100,000)',
    max_holding_amount        BIGINT      NOT NULL                COMMENT '개인 보유한도 기본값',
    default_expiration_days   INT         NOT NULL                COMMENT '기본 유효기간 (일). 1~1824',
    applied_at                DATETIME    NOT NULL                COMMENT '적용 시각. 미래값 = 예약 등록',
    created_by_admin_id       CHAR(36)    NOT NULL                COMMENT '등록 관리자',
    created_at                DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_policy_version    (policy_version),
    UNIQUE KEY uk_policy_applied_at (applied_at)
) COMMENT='포인트 정책 (버전별 이력)';

CREATE TABLE point_wallet (
    id                      CHAR(36) NOT NULL                COMMENT '지갑 ID',
    member_id               CHAR(36) NOT NULL                COMMENT '회원 ID',
    balance                 BIGINT   NOT NULL DEFAULT 0      COMMENT '총 잔액',
    holding_limit_override  BIGINT   NULL                    COMMENT '개인 한도 예외. NULL이면 정책값 적용',
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_member (member_id)
) COMMENT='포인트 지갑 (회원당 1행)';

CREATE TABLE point_earning (
    id                   CHAR(36)    NOT NULL                COMMENT '적립 ID',
    wallet_id            CHAR(36)    NOT NULL                COMMENT '지갑 ID',
    policy_id            CHAR(36)    NOT NULL                COMMENT '적립 당시 적용 정책',
    amount               BIGINT      NOT NULL                COMMENT '최초 적립액 (불변)',
    remaining_amount     BIGINT      NOT NULL                COMMENT '잔여액',
    earn_type            VARCHAR(10) NOT NULL                COMMENT 'SYSTEM / MANUAL',
    granted_by_admin_id  CHAR(36)    NULL                    COMMENT '수기지급 관리자. MANUAL일 때만',
    earned_at            DATETIME    NOT NULL                COMMENT '적립 일시',
    expires_at           DATETIME    NOT NULL                COMMENT '만료 일시 (적립 시점 확정)',
    status               VARCHAR(15) NOT NULL                COMMENT 'ACTIVE / EXHAUSTED / EXPIRED / CANCELED',
    canceled_at          DATETIME    NULL                    COMMENT '적립취소 일시',
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_earning_fifo   (wallet_id, status, expires_at, earned_at),
    KEY idx_earning_expire (status, expires_at),
    KEY idx_earning_policy (policy_id),

    CONSTRAINT fk_earning_wallet FOREIGN KEY (wallet_id) REFERENCES point_wallet (id),
    CONSTRAINT fk_earning_policy FOREIGN KEY (policy_id) REFERENCES point_policy (id)
) COMMENT='포인트 적립건';

CREATE TABLE point_usage (
    id               CHAR(36)    NOT NULL                COMMENT '사용 ID',
    wallet_id        CHAR(36)    NOT NULL                COMMENT '지갑 ID',
    order_number     VARCHAR(30) NOT NULL                COMMENT '주문번호',
    total_amount     BIGINT      NOT NULL                COMMENT '사용 총액 (라인 합계)',
    canceled_amount  BIGINT      NOT NULL DEFAULT 0      COMMENT '취소 누계',
    status           VARCHAR(20) NOT NULL                COMMENT 'USED / PARTIALLY_CANCELED / FULLY_CANCELED',
    used_at          DATETIME    NOT NULL                COMMENT '사용 일시',
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usage_order  (order_number),
    KEY        idx_usage_wallet (wallet_id, used_at),

    CONSTRAINT fk_usage_wallet FOREIGN KEY (wallet_id) REFERENCES point_wallet (id)
) COMMENT='포인트 사용건';

CREATE TABLE point_usage_line (
    id               CHAR(36) NOT NULL                COMMENT '사용 라인 ID',
    usage_id         CHAR(36) NOT NULL                COMMENT '사용 ID',
    earning_id       CHAR(36) NOT NULL                COMMENT '차감 대상 적립건',
    amount           BIGINT   NOT NULL                COMMENT '이 적립건에서 차감한 금액',
    canceled_amount  BIGINT   NOT NULL DEFAULT 0      COMMENT '복원된 금액 누계',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usage_line (usage_id, earning_id),
    KEY idx_usage_line_earning (earning_id),

    CONSTRAINT fk_usage_line_usage
        FOREIGN KEY (usage_id) REFERENCES point_usage (id),
    CONSTRAINT fk_usage_line_earning
        FOREIGN KEY (earning_id) REFERENCES point_earning (id)
) COMMENT='포인트 사용 라인';

CREATE TABLE point_usage_cancellation (
    id               CHAR(36) NOT NULL                COMMENT '사용취소 ID',
    usage_id         CHAR(36) NOT NULL                COMMENT '원 사용건',
    restored_amount  BIGINT   NOT NULL                COMMENT '복원 총액 (라인 합계)',
    canceled_at      DATETIME NOT NULL                COMMENT '취소 일시',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_cancellation_usage (usage_id, canceled_at),

    CONSTRAINT fk_cancellation_usage
        FOREIGN KEY (usage_id) REFERENCES point_usage (id)
) COMMENT='포인트 사용취소 헤더';

CREATE TABLE point_usage_cancellation_line (
    id                   CHAR(36)    NOT NULL                COMMENT '취소 라인 ID',
    cancellation_id      CHAR(36)    NOT NULL                COMMENT '사용취소 ID',
    usage_line_id        CHAR(36)    NOT NULL                COMMENT '복원 대상 원 사용 라인',
    restored_amount      BIGINT      NOT NULL                COMMENT '복원액',
    restore_type         VARCHAR(15) NOT NULL                COMMENT 'RESTORED / RE_EARNED',
    reearned_earning_id  CHAR(36)    NULL                    COMMENT 'RE_EARNED 시 생성된 신규 적립건',
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cancellation_line (cancellation_id, usage_line_id),
    KEY idx_cancellation_line_usage_line (usage_line_id),
    KEY idx_cancellation_line_reearned   (reearned_earning_id),

    CONSTRAINT fk_cancellation_line_header
        FOREIGN KEY (cancellation_id) REFERENCES point_usage_cancellation (id),
    CONSTRAINT fk_cancellation_line_usage_line
        FOREIGN KEY (usage_line_id) REFERENCES point_usage_line (id),
    CONSTRAINT fk_cancellation_line_reearned
        FOREIGN KEY (reearned_earning_id) REFERENCES point_earning (id)
) COMMENT='포인트 사용취소 라인';

CREATE TABLE point_wallet_transaction (
    id                CHAR(36)    NOT NULL                COMMENT '거래 ID',
    wallet_id         CHAR(36)    NOT NULL                COMMENT '지갑 ID',
    transaction_type  VARCHAR(20) NOT NULL                COMMENT 'EARN / USE / EARN_CANCEL / USE_CANCEL / EXPIRE',
    amount            BIGINT      NOT NULL                COMMENT '증감액. 증가 +, 감소 -',
    balance_after     BIGINT      NOT NULL                COMMENT '기록 시점 잔액',
    earning_id        CHAR(36)    NULL                    COMMENT 'EARN / EARN_CANCEL / EXPIRE',
    usage_id          CHAR(36)    NULL                    COMMENT 'USE',
    cancellation_id   CHAR(36)    NULL                    COMMENT 'USE_CANCEL',
    occurred_at       DATETIME    NOT NULL                COMMENT '발생 일시',
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_transaction_wallet       (wallet_id, occurred_at, id),
    KEY idx_transaction_earning      (earning_id),
    KEY idx_transaction_usage        (usage_id),
    KEY idx_transaction_cancellation (cancellation_id),

    CONSTRAINT fk_transaction_wallet
        FOREIGN KEY (wallet_id) REFERENCES point_wallet (id),
    CONSTRAINT fk_transaction_earning
        FOREIGN KEY (earning_id) REFERENCES point_earning (id),
    CONSTRAINT fk_transaction_usage
        FOREIGN KEY (usage_id) REFERENCES point_usage (id),
    CONSTRAINT fk_transaction_cancellation
        FOREIGN KEY (cancellation_id) REFERENCES point_usage_cancellation (id)
) COMMENT='포인트 지갑 거래 원장';
