CREATE DATABASE IF NOT EXISTS contact_management
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE contact_management;

CREATE TABLE IF NOT EXISTS contacts (
  id INT NOT NULL AUTO_INCREMENT,
  first_name VARCHAR(50) NOT NULL,
  last_name VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  email VARCHAR(100) NOT NULL,
  address VARCHAR(255),
  city VARCHAR(50),
  state VARCHAR(50),
  country VARCHAR(50),
  zip_code VARCHAR(20),
  company VARCHAR(100),
  job_title VARCHAR(100),
  birthday DATE,
  notes TEXT,
  favorite BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_contacts_phone UNIQUE (phone),
  CONSTRAINT uq_contacts_email UNIQUE (email),
  CONSTRAINT chk_contacts_first_name CHECK (CHAR_LENGTH(TRIM(first_name)) > 0),
  CONSTRAINT chk_contacts_last_name CHECK (CHAR_LENGTH(TRIM(last_name)) > 0),
  INDEX idx_contacts_name (last_name, first_name),
  INDEX idx_contacts_company (company),
  INDEX idx_contacts_city (city),
  INDEX idx_contacts_country (country),
  INDEX idx_contacts_favorite_created (favorite, created_at),
  INDEX idx_contacts_birthday (birthday)
) ENGINE=InnoDB;

INSERT IGNORE INTO contacts (first_name,last_name,phone,email,address,city,state,country,zip_code,company,job_title,birthday,notes,favorite)
VALUES
('Avery','Morgan','+1-212-555-0101','avery.morgan@example.com','18 West 24th Street','New York','NY','USA','10010','Northstar Studio','Designer','1990-10-01','Sample contact',TRUE),
('Jordan','Lee','+1-415-555-0132','jordan.lee@example.com','400 Market Street','San Francisco','CA','USA','94105','Fieldwork Labs','Engineer','1988-03-14','Sample contact',FALSE),
('Riley','Patel','+44 20 7946 0198','riley.patel@example.com','22 Baker Street','London','','United Kingdom','NW1 6XE','Greenline Group','Consultant','1993-12-20','Sample contact',FALSE)
