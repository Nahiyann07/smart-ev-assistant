CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX idx_users_role_enabled ON users (role, enabled);

CREATE TABLE stations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    latitude DECIMAL(9, 6),
    longitude DECIMAL(9, 6),
    charger_type VARCHAR(20) NOT NULL,
    total_ports INT NOT NULL,
    available_ports INT NOT NULL,
    out_of_service_ports INT NOT NULL,
    charging_speed_kw DECIMAL(6, 2) NOT NULL,
    operating_hours VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    deleted_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_stations PRIMARY KEY (id),
    CONSTRAINT ck_stations_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_stations_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_stations_charger_type CHECK (charger_type IN ('AC', 'DC_FAST')),
    CONSTRAINT ck_stations_total_ports CHECK (total_ports > 0),
    CONSTRAINT ck_stations_available_ports CHECK (available_ports >= 0),
    CONSTRAINT ck_stations_out_of_service_ports CHECK (out_of_service_ports >= 0),
    CONSTRAINT ck_stations_port_sum CHECK (available_ports + out_of_service_ports <= total_ports),
    CONSTRAINT ck_stations_charging_speed CHECK (charging_speed_kw > 0),
    CONSTRAINT ck_stations_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'UNDER_MAINTENANCE', 'OUT_OF_SERVICE')),
    CONSTRAINT ck_stations_status_ports CHECK (
        (status = 'AVAILABLE' AND available_ports > 0)
        OR (status = 'OCCUPIED' AND available_ports = 0 AND total_ports > out_of_service_ports)
        OR (status = 'UNDER_MAINTENANCE' AND available_ports = 0 AND out_of_service_ports > 0)
        OR (status = 'OUT_OF_SERVICE' AND available_ports = 0 AND out_of_service_ports = total_ports)
    )
);

CREATE INDEX idx_stations_city ON stations (city);
CREATE INDEX idx_stations_name ON stations (name);
CREATE INDEX idx_stations_status_deleted ON stations (status, deleted_at);
CREATE INDEX idx_stations_charger_status ON stations (charger_type, status);
CREATE INDEX idx_stations_city_status ON stations (city, status);

CREATE TABLE reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    comment VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT uk_reviews_user_station UNIQUE (user_id, station_id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_station FOREIGN KEY (station_id) REFERENCES stations (id) ON DELETE RESTRICT
);

CREATE INDEX idx_reviews_station_created ON reviews (station_id, created_at);

CREATE TABLE reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    issue_type VARCHAR(40) NOT NULL,
    description VARCHAR(1500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolved_by_user_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    resolved_at DATETIME(6),
    CONSTRAINT pk_reports PRIMARY KEY (id),
    CONSTRAINT ck_reports_issue_type CHECK (issue_type IN (
        'CHARGER_NOT_WORKING', 'LONG_WAITING_TIME', 'STATION_CLOSED', 'PAYMENT_ISSUE', 'OTHER'
    )),
    CONSTRAINT ck_reports_status CHECK (status IN ('PENDING', 'RESOLVED', 'REJECTED')),
    CONSTRAINT ck_reports_resolution CHECK (
        (status = 'PENDING' AND resolved_by_user_id IS NULL AND resolved_at IS NULL)
        OR (status IN ('RESOLVED', 'REJECTED') AND resolved_by_user_id IS NOT NULL AND resolved_at IS NOT NULL)
    ),
    CONSTRAINT fk_reports_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reports_station FOREIGN KEY (station_id) REFERENCES stations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reports_resolver FOREIGN KEY (resolved_by_user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_reports_status_created ON reports (status, created_at);
CREATE INDEX idx_reports_station_status ON reports (station_id, status);
CREATE INDEX idx_reports_user_created ON reports (user_id, created_at);

CREATE TABLE favourites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_favourites PRIMARY KEY (id),
    CONSTRAINT uk_favourites_user_station UNIQUE (user_id, station_id),
    CONSTRAINT fk_favourites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_favourites_station FOREIGN KEY (station_id) REFERENCES stations (id) ON DELETE RESTRICT
);

CREATE INDEX idx_favourites_station ON favourites (station_id);
