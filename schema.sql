CREATE DATABASE IF NOT EXISTS blood_bank_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE blood_bank_db;

CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role ENUM('ADMIN','STAFF') NOT NULL DEFAULT 'STAFF',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  active_status BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS donors (
  donor_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NULL,
  phone VARCHAR(30) NOT NULL,
  date_of_birth DATE NULL,
  gender VARCHAR(30) NULL,
  blood_group ENUM('A+','A-','B+','B-','AB+','AB-','O+','O-') NOT NULL,
  address VARCHAR(500) NULL,
  registration_date DATE NOT NULL DEFAULT (CURRENT_DATE),
  last_donation_date DATE NULL,
  eligibility_status ENUM('PENDING','ELIGIBLE','INELIGIBLE','DEFERRED') NOT NULL DEFAULT 'PENDING',
  active_status BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uq_donor_email (email),
  KEY idx_donor_name (full_name),
  KEY idx_donor_phone (phone),
  KEY idx_donor_blood_group (blood_group)
);

CREATE TABLE IF NOT EXISTS blood_stock (
  stock_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  blood_group ENUM('A+','A-','B+','B-','AB+','AB-','O+','O-') NOT NULL UNIQUE,
  available_units INT NOT NULL DEFAULT 0,
  minimum_threshold INT NOT NULL DEFAULT 5,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_available_units CHECK (available_units >= 0),
  CONSTRAINT chk_minimum_threshold CHECK (minimum_threshold >= 0)
);

CREATE TABLE IF NOT EXISTS donations (
  donation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  donor_id BIGINT NOT NULL,
  donation_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  quantity INT NOT NULL,
  collection_centre VARCHAR(180) NOT NULL,
  staff_user_id BIGINT NOT NULL,
  status ENUM('ACCEPTED','REJECTED','CANCELLED') NOT NULL DEFAULT 'ACCEPTED',
  remarks VARCHAR(1000) NULL,
  request_token VARCHAR(64) NOT NULL UNIQUE,
  CONSTRAINT chk_donation_quantity CHECK (quantity > 0),
  CONSTRAINT fk_donation_donor FOREIGN KEY (donor_id) REFERENCES donors(donor_id),
  CONSTRAINT fk_donation_staff FOREIGN KEY (staff_user_id) REFERENCES users(user_id),
  KEY idx_donation_date (donation_date)
);

CREATE TABLE IF NOT EXISTS stock_transactions (
  transaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  blood_group ENUM('A+','A-','B+','B-','AB+','AB-','O+','O-') NOT NULL,
  transaction_type ENUM('DONATION','ISSUE','ADJUSTMENT') NOT NULL,
  quantity INT NOT NULL,
  reference_id BIGINT NULL,
  reason VARCHAR(500) NOT NULL,
  staff_user_id BIGINT NOT NULL,
  transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_stock_tx_staff FOREIGN KEY (staff_user_id) REFERENCES users(user_id),
  KEY idx_stock_tx_date (transaction_date)
);

CREATE TABLE IF NOT EXISTS donation_receipts (
  receipt_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  receipt_number VARCHAR(40) NOT NULL UNIQUE,
  donation_id BIGINT NOT NULL UNIQUE,
  generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_receipt_donation FOREIGN KEY (donation_id) REFERENCES donations(donation_id)
);

CREATE TABLE IF NOT EXISTS stock_alerts (
  alert_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  blood_group ENUM('A+','A-','B+','B-','AB+','AB-','O+','O-') NOT NULL,
  current_units INT NOT NULL,
  threshold_units INT NOT NULL,
  alert_status ENUM('OPEN','ACKNOWLEDGED','RESOLVED') NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  acknowledged_at TIMESTAMP NULL
);

INSERT IGNORE INTO blood_stock (blood_group, available_units, minimum_threshold) VALUES
('A+',18,10),('A-',6,8),('B+',22,10),('B-',5,6),
('AB+',9,5),('AB-',3,4),('O+',27,12),('O-',7,8);
