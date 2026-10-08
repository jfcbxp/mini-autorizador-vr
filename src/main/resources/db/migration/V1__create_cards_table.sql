CREATE TABLE cards (
    card_number   VARCHAR(19)   NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    balance       DECIMAL(19,2) NOT NULL DEFAULT 500.00,
    version       BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (card_number)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
