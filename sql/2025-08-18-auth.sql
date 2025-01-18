ALTER TABLE Customer ADD COLUMN password_salt CHAR(6);
ALTER TABLE Customer MODIFY password_hash CHAR(64);

ALTER TABLE Administrator ADD COLUMN password_salt CHAR(6);
ALTER TABLE Administrator MODIFY password_hash CHAR(64);
DROP FUNCTION IF EXISTS GEN_SALT;
DELIMITER $$
CREATE FUNCTION GEN_SALT()
    RETURNS TEXT
    CONTAINS SQL
BEGIN
    RETURN SUBSTR(SHA1(RAND()),1,6);
END $$
DELIMITER ;
DROP FUNCTION IF EXISTS `PWD_HASH`;
DELIMITER $$
CREATE FUNCTION PWD_HASH(plain TEXT,salt TEXT)
    RETURNS CHAR(64)
    CONTAINS SQL
BEGIN
    RETURN SHA2(salt+plain,256);
END $$
DELIMITER ;
DROP PROCEDURE IF EXISTS REGISTER_CUSTOMER;
DELIMITER $$
CREATE PROCEDURE REGISTER_CUSTOMER(email_ VARCHAR(320), username_ VARCHAR(32), plain_password TEXT, OUT ret_id VARCHAR(11))
    MODIFIES SQL DATA
BEGIN
    DECLARE id VARCHAR(11);
    DECLARE salt TEXT;
    DECLARE pass TEXT;
    SELECT GEN_SALT() INTO salt;
    SELECT PWD_HASH(plain_password,salt) INTO pass;
    SELECT MAKE_ID() INTO id;
    SELECT id, salt, pass;
    INSERT INTO Customer (customer_id,email,username,password_salt,password_hash)
    VALUES (id,email_,username_,salt,pass);
    SELECT id INTO ret_id;
END;$$
DELIMITER ;
DROP FUNCTION IF EXISTS `GET_CUSTOMER_BY_CREDENTIALS`;
DELIMITER $$
CREATE FUNCTION GET_CUSTOMER_BY_CREDENTIALS(username_ VARCHAR(32), plain_password TEXT)
    RETURNS VARCHAR(11)
    READS SQL DATA
BEGIN
    DECLARE id VARCHAR(11);
    DECLARE salt CHAR(6);
    DECLARE pass CHAR(64);
    SELECT customer_id, password_salt, password_hash INTO id, salt, pass FROM Customer WHERE username=username_;
    IF id IS NULL THEN
        RETURN NULL;
    ELSEIF PWD_HASH(plain_password,salt)=pass THEN
        RETURN id;
    END IF;
    RETURN PWD_HASH(plain_password,salt);
END;$$
DELIMITER ;