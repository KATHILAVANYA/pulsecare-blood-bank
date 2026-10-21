# PulseCare — Java Full-Stack Blood Bank Management System

Stack: Java 17, Servlets/JSP (`javax.servlet`, for Tomcat 9), JDBC, MySQL 8, Bootstrap 5, Chart.js, BCrypt.

## Features implemented in this starter project
- BCrypt password hashing, session login/logout, protected routes, registration, role field
- Dashboard metrics from MySQL and stock chart
- Donor add/list/search/deactivate
- Blood inventory with thresholds and validated adjustments
- Blood group/quantity availability search
- Donation recording with transaction-safe stock increment and duplicate-submit token
- Receipt view and browser print / Save as PDF
- Low-stock alerts and basic donation report
- SQL schema, demo sample stock, and deployment/setup instructions

## Requirements
- JDK 17+
- Maven 3.9+
- MySQL 8+
- Apache Tomcat 9 (not Tomcat 10+, because this project uses `javax.servlet`)

## Setup
1. Create database and tables:
   - Open MySQL Workbench or MySQL CLI.
   - Run `src/main/resources/db/schema.sql`.
2. Configure DB connection using environment variables (recommended):
   - `DB_URL=jdbc:mysql://localhost:3306/blood_bank_db?useSSL=false&serverTimezone=UTC`
   - `DB_USER=your_mysql_user`
   - `DB_PASSWORD=your_mysql_password`
   Windows PowerShell example:
   ```powershell
   $env:DB_URL="jdbc:mysql://localhost:3306/blood_bank_db?useSSL=false&serverTimezone=UTC"
   $env:DB_USER="root"
   $env:DB_PASSWORD="your_password"
   ```
   The code has local development defaults for DB URL/user, but set `DB_PASSWORD` in your environment.
3. From the project root:
   ```bash
   mvn clean package
   ```
4. Copy `target/blood-bank-management.war` to Tomcat 9's `webapps` folder and start Tomcat.
5. Open `http://localhost:8080/blood-bank-management/`
6. Register the first staff account using **Create staff account**. The first account receives ADMIN role; later accounts receive STAFF role. Keep registration restricted in a real deployment.
7. Log in, add donors, record donations, and test stock alerts.

## Important notes
- This is an academic application baseline, not a certified clinical system. Demo stock is illustrative.
- Donor eligibility is not medically determined by the software.
- Do not put database passwords in source control. Configure environment variables.
- Registration is open for demonstration. Before production, restrict registration to admins and configure HTTPS, CSRF protection, rate limiting, audit logging, backups, and a managed database.
- SMTP notifications are not wired in this baseline; alerts are shown in-app.
- Before using real donor data, review local privacy, security, and blood-bank regulations.
