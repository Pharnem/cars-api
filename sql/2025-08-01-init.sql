-- Prepare Database
SET GLOBAL log_bin_trust_function_creators = 1;
CREATE DATABASE Cars;
CREATE USER 'zygzak' IDENTIFIED BY 'NiedzielnyKierowca7';
GRANT ALL PRIVILEGES ON Cars.* TO 'zygzak';
GRANT EXECUTE ON Cars.* TO 'zygzak';
USE Cars;

-- Prepare Spring Tables
CREATE TABLE SPRING_SESSION (
                                PRIMARY_ID CHAR(36) NOT NULL,
                                SESSION_ID CHAR(36) NOT NULL,
                                CREATION_TIME BIGINT NOT NULL,
                                LAST_ACCESS_TIME BIGINT NOT NULL,
                                MAX_INACTIVE_INTERVAL INT NOT NULL,
                                EXPIRY_TIME BIGINT NOT NULL,
                                PRINCIPAL_NAME VARCHAR(100),
                                CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC;

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
                                           SESSION_PRIMARY_ID CHAR(36) NOT NULL,
                                           ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
                                           ATTRIBUTE_BYTES BLOB NOT NULL,
                                           CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
                                           CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC;

-- For special values required in the database
CREATE TABLE KVStore (
                         store_key VARCHAR(32) NOT NULL UNIQUE,
                         val_int BIGINT,
                         val_str TEXT
);

INSERT INTO KVStore (store_key, val_int, val_str) VALUES
                                                      ('IdCounter',0,NULL),
                                                      ('IdMoment',UNIX_TIMESTAMP(NOW(3)) * 1000000 + MICROSECOND(NOW(3)),NULL);
-- Time-sorted unique IDs with resolution for simultaneous requests.
DROP FUNCTION IF EXISTS MAKE_ID;
DELIMITER $$
CREATE FUNCTION MAKE_ID()
    RETURNS VARCHAR(11)
    READS SQL DATA
    MODIFIES SQL DATA
BEGIN
    DECLARE moment BIGINT;
    DECLARE moment_s VARCHAR(10);
    DECLARE counter_s VARCHAR(5);
    DECLARE moment_prev BIGINT;
    DECLARE counter_prev BIGINT;

    SELECT val_int INTO moment_prev FROM KVStore WHERE store_key='IdMoment';
    SELECT val_int INTO counter_prev FROM KVStore WHERE store_key='IdCounter';

    SET moment = UNIX_TIMESTAMP(NOW(3)) * 1000000 + MICROSECOND(NOW(3));

    IF moment > moment_prev THEN
        SET counter_prev = -1;
    END IF;
    UPDATE KVStore
    SET val_int = moment
    WHERE store_key='IdMoment';
    UPDATE KVStore
    SET val_int = counter_prev+1
    WHERE store_key='IdCounter';
    SET moment_s = LOWER(CONV(LPAD(moment, 18, '0'), 10, 36));
    SET counter_s = LOWER(CONV(counter_prev+1, 10, 36));

    RETURN CONCAT(moment_s,counter_s);
END $$
DELIMITER ;

-- Data tables
CREATE TABLE Administrator(
                              administrator_id VARCHAR(11) NOT NULL PRIMARY KEY,
                              email VARCHAR(320) NOT NULL UNIQUE,
                              username VARCHAR(32) NOT NULL UNIQUE,
                              password_hash TEXT NOT NULL
);

CREATE TABLE Customer(
                         customer_id VARCHAR(11) NOT NULL PRIMARY KEY,
                         email VARCHAR(320) NOT NULL UNIQUE,
                         username VARCHAR(32) NOT NULL UNIQUE,
                         password_hash TEXT NOT NULL
);

CREATE TABLE Producer(
                         producer_id VARCHAR(11) NOT NULL PRIMARY KEY,
                         name VARCHAR(128) NOT NULL,
                         shortname VARCHAR(32) NOT NULL
);

CREATE TABLE FuelType(
                         fuel_type_id VARCHAR(11) NOT NULL PRIMARY KEY,
                         name VARCHAR(64) NOT NULL
);

CREATE TABLE Model(
                      model_id VARCHAR(11) NOT NULL PRIMARY KEY,
                      producer_id VARCHAR(11) NOT NULL,
                      name VARCHAR(32) NOT NULL,
                      production_year INTEGER NOT NULL,
                      fuel_type_id VARCHAR(11) NOT NULL,
                      fuel_capacity INTEGER NOT NULL,
                      seat_count INTEGER NOT NULL,
                      door_count INTEGER NOT NULL,
                      daily_rate DECIMAL(10,2) NOT NULL,
                      FOREIGN KEY (producer_id) REFERENCES Producer (producer_id),
                      FOREIGN KEY (fuel_type_id) REFERENCES FuelType (fuel_type_id)
);

CREATE TABLE Location(
                         location_id VARCHAR(11) NOT NULL PRIMARY KEY,
                         country VARCHAR(64) NOT NULL,
                         city VARCHAR(64) NOT NULL,
                         postal_code VARCHAR(16) NOT NULL,
                         street VARCHAR(64) NOT NULL,
                         street_number VARCHAR(10) NOT NULL,
                         full_address TEXT GENERATED ALWAYS AS (CONCAT(street,' ',street_number,', ',postal_code,' ',city,', ',country)),
                         coordinates POINT NOT NULL
);

CREATE TABLE Image(
                      image_id VARCHAR(11) NOT NULL PRIMARY KEY,
                      data MEDIUMBLOB NOT NULL
);

CREATE TABLE Car(
                    car_id VARCHAR(11) NOT NULL PRIMARY KEY,
                    model_id VARCHAR(11) NOT NULL,
                    location_id VARCHAR(11) NOT NULL,
                    image_id VARCHAR(11) NOT NULL,
                    FOREIGN KEY (model_id) REFERENCES Model (model_id),
                    FOREIGN KEY (location_id) REFERENCES Location (location_id),
                    FOREIGN KEY (image_id) REFERENCES Image (image_id)
);
CREATE TABLE Rental(
                       rental_id VARCHAR(11) NOT NULL PRIMARY KEY,
                       customer_id VARCHAR(11) NOT NULL,
                       car_id VARCHAR(11) NOT NULL,
                       start_at DATETIME NOT NULL,
                       end_at DATETIME NOT NULL,
                       cancelled BOOLEAN NOT NULL DEFAULT 0,
                       duration_days FLOAT GENERATED ALWAYS AS ((TIMESTAMPDIFF(HOUR,end_at,start_at))/24),
                       FOREIGN KEY (customer_id) REFERENCES Customer (customer_id),
                       FOREIGN KEY (car_id) REFERENCES Car (car_id)
);

CREATE TABLE ExternalCustomer(
                                 external_customer_id VARCHAR(11) NOT NULL PRIMARY KEY,
                                 email VARCHAR(320) NOT NULL UNIQUE
);
CREATE TABLE ExternalRental(
                               external_rental_id VARCHAR(11) NOT NULL PRIMARY KEY,
                               external_customer_id VARCHAR(11) NOT NULL,
                               car_id VARCHAR(11) NOT NULL,
                               start_at DATETIME NOT NULL,
                               end_at DATETIME NOT NULL,
                               cancelled BOOLEAN NOT NULL DEFAULT 0,
                               duration_days FLOAT GENERATED ALWAYS AS ((TIMESTAMPDIFF(HOUR,end_at,start_at))/24),
                               FOREIGN KEY (external_customer_id) REFERENCES ExternalCustomer (external_customer_id),
                               FOREIGN KEY (car_id) REFERENCES Car (car_id)
);

CREATE VIEW AvailableCar AS (
                            SELECT * FROM Car AS C WHERE
                                NOT EXISTS (
                                    (SELECT * FROM Rental AS R
                                     WHERE R.cancelled=0
                                       AND NOW()<R.end_at
                                       AND R.car_id=C.car_id)
                                    UNION ALL
                                    (SELECT * FROM ExternalRental AS ER
                                     WHERE ER.cancelled=0
                                       AND NOW()<ER.end_at
                                       AND ER.car_id=C.car_id)
                                )
                                );
-- Sample data for tables which are not yet present/will not be present from the API level
-- Insert into Producer (Define in SQL?)
INSERT INTO Producer (producer_id, name, shortname)
VALUES
    (MAKE_ID(), 'Toyota', 'Toyota'),
    (MAKE_ID(), 'Ford', 'Ford'),
    (MAKE_ID(), 'Tesla', 'Tesla');

-- Insert into FuelType (Define in SQL)
INSERT INTO FuelType (fuel_type_id, name)
VALUES
    (MAKE_ID(), 'Petrol'),
    (MAKE_ID(), 'Diesel'),
    (MAKE_ID(), 'Electric');

-- Insert into Model (Add api-based modification?)
INSERT INTO Model (model_id, producer_id, name, production_year, fuel_type_id, fuel_capacity, seat_count, door_count, daily_rate)
VALUES
    (MAKE_ID(), (SELECT producer_id FROM Producer WHERE name = 'Toyota'), 'Corolla', 2022, (SELECT fuel_type_id FROM FuelType WHERE name = 'Petrol'), 50, 5, 4, 30.00),
    (MAKE_ID(), (SELECT producer_id FROM Producer WHERE name = 'Ford'), 'Focus', 2021, (SELECT fuel_type_id FROM FuelType WHERE name = 'Diesel'), 60, 5, 4, 35.00),
    (MAKE_ID(), (SELECT producer_id FROM Producer WHERE name = 'Tesla'), 'Model 3', 2023, (SELECT fuel_type_id FROM FuelType WHERE name = 'Electric'), 80, 5, 4, 50.00);

-- Insert into Location (Add api-based modification?)
INSERT INTO Location (location_id, country, city, postal_code, street, street_number, coordinates)
VALUES
    (MAKE_ID(), 'USA', 'New York', '10001', '5th Avenue', '10', POINT(40.7128, -74.0060)),
    (MAKE_ID(), 'Germany', 'Berlin', '10115', 'Alexanderplatz', '3', POINT(52.5200, 13.4050));