-- ETF Stock Platform Database Schema
-- 전략과제 #1, #2, #3을 위한 테이블 구조

-- 테이블 삭제 (역순으로)
DROP TABLE IF EXISTS alerts;
DROP TABLE IF EXISTS alert_conditions;
DROP TABLE IF EXISTS user_watchlist;
DROP TABLE IF EXISTS etf_info;
DROP TABLE IF EXISTS users;

-- 사용자 테이블 생성
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- ETF 정보 테이블 생성
CREATE TABLE etf_info (
    id BIGSERIAL PRIMARY KEY,
    base_date DATE NOT NULL,
    srtn_cd VARCHAR(10),
    isin_cd VARCHAR(12) NOT NULL,
    itms_nm VARCHAR(200) NOT NULL,
    close_price DECIMAL(15,2),
    vs DECIMAL(15,2),
    flt_rt DECIMAL(8,4),
    nav DECIMAL(15,2),
    open_price DECIMAL(15,2),
    high_price DECIMAL(15,2),
    low_price DECIMAL(15,2),
    trade_volume BIGINT,
    trade_price DECIMAL(20,2),
    market_total_amt DECIMAL(20,2),
    net_asset_total_amt DECIMAL(20,2),
    st_lstg_cnt BIGINT,
    base_index_name VARCHAR(100),
    base_index_close_price DECIMAL(15,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 관심종목 테이블 생성
CREATE TABLE user_watchlist (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    isin_cd VARCHAR(12) NOT NULL,
    etf_name VARCHAR(200),
    short_code VARCHAR(10),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    removed_at TIMESTAMP,
    memo VARCHAR(500),
    notification_enabled BOOLEAN DEFAULT TRUE,
    UNIQUE(user_id, isin_cd)
);

-- 알림 조건 테이블 생성
CREATE TABLE alert_conditions (
    id BIGSERIAL PRIMARY KEY,
    watchlist_id BIGINT NOT NULL REFERENCES user_watchlist(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    condition_type VARCHAR(20) NOT NULL,
    threshold_value DECIMAL(10,4) NOT NULL,
    base_price DECIMAL(15,2),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(200),
    last_triggered_at TIMESTAMP
);

-- 알림 이력 테이블 생성
CREATE TABLE alerts (
    id BIGSERIAL PRIMARY KEY,
    alert_condition_id BIGINT NOT NULL REFERENCES alert_conditions(id) ON DELETE CASCADE,
    watchlist_id BIGINT NOT NULL REFERENCES user_watchlist(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    isin_cd VARCHAR(12) NOT NULL,
    etf_name VARCHAR(200) NOT NULL,
    alert_type VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    priority VARCHAR(10) DEFAULT 'NORMAL',
    trigger_price DECIMAL(15,2) NOT NULL,
    base_price DECIMAL(15,2) NOT NULL,
    change_percentage DECIMAL(10,4) NOT NULL,
    change_amount DECIMAL(15,2) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    triggered_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP,
    alert_status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
-- ETF 정보 인덱스
CREATE INDEX idx_etf_base_date ON etf_info(base_date);
CREATE INDEX idx_etf_code ON etf_info(isin_cd);
CREATE INDEX idx_etf_short_code ON etf_info(srtn_cd);
CREATE INDEX idx_etf_change_rate ON etf_info(flt_rt);
CREATE INDEX idx_etf_name ON etf_info(itms_nm);

-- 사용자 인덱스
CREATE INDEX idx_user_username ON users(username);
CREATE INDEX idx_user_email ON users(email);

-- 관심종목 인덱스
CREATE INDEX idx_watchlist_user_id ON user_watchlist(user_id);
CREATE INDEX idx_watchlist_isin_cd ON user_watchlist(isin_cd);
CREATE INDEX idx_watchlist_user_etf ON user_watchlist(user_id, isin_cd);
CREATE INDEX idx_watchlist_active ON user_watchlist(is_active);
CREATE INDEX idx_watchlist_created ON user_watchlist(created_at);

-- 알림 조건 인덱스
CREATE INDEX idx_alert_condition_watchlist ON alert_conditions(watchlist_id);
CREATE INDEX idx_alert_condition_active ON alert_conditions(is_active);
CREATE INDEX idx_alert_condition_type ON alert_conditions(condition_type);
CREATE INDEX idx_alert_condition_user ON alert_conditions(user_id);

-- 알림 이력 인덱스
CREATE INDEX idx_alert_user ON alerts(user_id);
CREATE INDEX idx_alert_condition ON alerts(alert_condition_id);
CREATE INDEX idx_alert_watchlist ON alerts(watchlist_id);
CREATE INDEX idx_alert_triggered ON alerts(triggered_at);
CREATE INDEX idx_alert_read ON alerts(is_read);
CREATE INDEX idx_alert_status ON alerts(alert_status);
CREATE INDEX idx_alert_isin ON alerts(isin_cd);

-- 시퀀스 초기화 (테이블 삭제 후 재생성 시)
ALTER SEQUENCE users_id_seq RESTART WITH 1;
ALTER SEQUENCE etf_info_id_seq RESTART WITH 1;
ALTER SEQUENCE user_watchlist_id_seq RESTART WITH 1;
ALTER SEQUENCE alert_conditions_id_seq RESTART WITH 1;
ALTER SEQUENCE alerts_id_seq RESTART WITH 1;
