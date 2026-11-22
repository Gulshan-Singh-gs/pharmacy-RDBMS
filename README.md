# Gulshan Singh Pharmacy Management System

A desktop pharmacy management system built with **Java Swing** (NetBeans GUI
forms) and a **MySQL** database.

- **Developer:** Gulshan Singh
- **Roll No.:** 24011010059
- **Course:** Relational Database Management Systems (RDBMS)
- **Department:** Computer Science & Engineering
- **Institution:** Rayat Bahra University

## Features

Implemented and wired to the database:

- **Inventory** — view all drugs (`Drug_List`), search by name / barcode / type
  (`Search_Drug`), add a new drug (`Buy_Drug`)
- **Expiry alerts** — expired stock (`Expired_Drug`, `v_expired_drugs`) and
  stock expiring within 90 days (`Almost_Finish`, `v_almost_expire`)
- **Billing** — create a sales bill with line items and automatic stock
  decrement in one transaction (`Sales_Bill`); daily shift summaries
  (`Shift_Sales`); full sales history (`All_Deal`, `history_sales`)
- **Suppliers** — view and add companies (`Company`)
- **Messaging** — inbox for the logged-in user with mark-as-read
  (`Inbox`, `messages` table)
- **Warnings** — low-stock alerts (`Warning_List`, `v_low_stock`)
- **Authentication** — login with salted SHA-256 password hashes, login audit
  log, and forced first-login password change

## Tech stack

- Java 8+ (verified with Java 21) — Swing GUI
- MySQL 8.0 / MariaDB 10.6+ — `pharmacy` database (3NF schema, `pharmacy.sql`)
- NetBeans-form-compatible Ant build (`build.xml`, `nbproject/`)

## Getting started

### 1. Prerequisites

- JDK 8+ (or newer)
- MySQL / MariaDB running locally
- (Optional) NetBeans IDE for editing the `.form` GUI designer files

### 2. Create the database

```bash
mysql -u root < pharmacy.sql
```

This creates the `pharmacy` database (dropping an existing one), all tables,
views, and demo seed data.

### 3. Add the JDBC driver

The project intentionally does not commit MySQL connector JARs. Download the
MySQL Connector/J driver (e.g. 8.0.33) and place it in a `lib/` folder:

- `https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar`
- Save it as `lib/mysql-connector-j.jar`.

> The `lib/` folder is gitignored; re-download after a fresh clone.

### 4. Configure the connection

`Connect.java` defaults to `jdbc:mysql://localhost:3306/pharmacy` with
`root` and an empty password. Override without editing code via:

```bash
PHARMACY_DB_URL=jdbc:mysql://localhost:3306/pharmacy
PHARMACY_DB_USER=root
PHARMACY_DB_PASSWORD=yourpassword
```

### 5. Build and run (no IDE required)

```bash
# compile all sources
javac -d build/classes -cp lib/mysql-connector-j.jar -encoding UTF-8 \
  $(find src/main -name "*.java")     # or use a file list

# run
java -cp "build/classes;lib/mysql-connector-j.jar" main.Main
```

Or open the project in NetBeans (`File → Open Project`) and run `Main.java`.

### Demo logins

| Username | Password | Notes |
|----------|----------|-------|
| `admin` | `admin` | Forces a one-time password change on first login |
| `staff` | `staff123` | Demo user; has a seeded inbox message |

## Project structure

```
├── src/main/        Java sources + NetBeans GUI forms (package `main`)
├── nbproject/       NetBeans project metadata
├── pharmacy.sql      Database schema + demo seed data
├── lib/             JDBC driver (downloaded, gitignored)
└── build/           Compiled classes (generated, gitignored)
```

## Database

The `pharmacy` database uses a Third Normal Form (3NF) schema. Key tables:
`company`, `users`, `drugs`, `purchase_orders`, `purchase_items`, `sales`,
`sale_items`, `login_log`, `messages`, plus reporting views (`v_expired_drugs`,
`v_almost_expire`, `v_low_stock`, `history_sales`, `inbox`).

Passwords are stored only as salted SHA-256 hashes — never plaintext.

## License & conduct

See `CODE_OF_CONDUCT.md`. This is an academic project.
