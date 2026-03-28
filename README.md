# 🔬 MedLab — Medical Laboratory System
### Version 2.0 · Spring Boot Monolith · Cognizant Team 5

A full-stack **Spring Boot 3.3.5 + Java 21** monolith managing the complete
medical laboratory workflow — from patient registration through sample collection,
test processing, report generation, billing, and notifications.

---

## 📋 Project Context

| Version | Stack | Description |
|---------|-------|-------------|
| v1 (done) | Core Java + JDBC + MySQL | CLI application |
| **v2 (this)** | **Spring Boot + JPA + Thymeleaf** | **Full-stack monolith** |
| v3 (future) | Microservices + K8s + React | Distributed system |

**Project 10 — Medical Laboratory System**  
Mandatory services present in all versions:
- ✅ Test Order Service
- ✅ Sample Service
- ✅ Report Service
- ✅ Notification Service

---

## 🏗️ Architecture

```
com.cognizant.medlab/
├── MedLabApplication.java          ← @SpringBootApplication entry point
├── config/                         ← Security, Swagger, Auditing, DevDataLoader
├── domain/                         ← JPA entities (DDD-aligned packages)
│   ├── common/BaseEntity.java      ← Audit fields + soft-delete (all entities extend this)
│   ├── identity/                   ← User, Role
│   ├── patient/                    ← Patient
│   ├── testcatalog/                ← LabTest, Panel
│   ├── scheduling/                 ← Appointment, Sample
│   ├── processing/                 ← TestOrder, TestResult
│   ├── reporting/                  ← Report
│   ├── billing/                    ← Invoice, Payment
│   ├── notification/               ← NotificationLog
│   └── audit/                      ← AuditLog
├── repository/                     ← Spring Data JPA repositories
├── application/service/            ← Business logic (8 services)
├── web/
│   ├── controller/                 ← 9 REST controllers (/api/**)
│   ├── thymeleaf/UiController.java ← Thymeleaf MVC (/ui/**)
│   └── dto/                        ← Request/Response DTOs
├── security/                       ← JwtUtil, JwtAuthFilter, UserDetailsService
├── integration/                    ← PaymentGateway interface + MockPaymentGateway
└── exception/                      ← GlobalExceptionHandler + custom exceptions
```

---

## ⚙️ Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Persistence | Spring Data JPA + Hibernate |
| Database (prod/dev) | MySQL 8.x |
| Database (test) | H2 in-memory |
| Migrations | Flyway (V1 schema + V2 seed) |
| Security | Spring Security 6 + JWT (jjwt 0.12.x) + BCrypt |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Frontend | Thymeleaf + plain HTML/CSS/JS |
| Testing | JUnit 5 + Mockito + Cucumber BDD |
| Coverage | JaCoCo (≥ 70% line coverage enforced) |
| Code Quality | SonarQube |
| Build | Maven 3.6+ |
| Logging | SLF4J + Logback |

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version |
|------|---------|
| JDK | 21+ |
| Maven | 3.6+ |
| MySQL | 8.x |

### 1. Create the database

```sql
CREATE DATABASE medlab_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Flyway creates all tables and seeds data automatically on first startup.

### 2. Configure environment variables

```bash
# Linux / macOS
export DB_URL="jdbc:mysql://localhost:3306/medlab_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true"
export DB_USER="root"
export DB_PASSWORD="your_password"
export JWT_SECRET="your-256-bit-secret-change-in-production"
```

```bat
REM Windows
set DB_URL=jdbc:mysql://localhost:3306/medlab_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
set DB_USER=root
set DB_PASSWORD=your_password
set JWT_SECRET=your-256-bit-secret-change-in-production
```

### 3. Run the application

```bash
# Dev profile (Swagger enabled, data loader active, debug logging)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or build and run fat JAR
mvn clean package -DskipTests
java -jar target/medlab-2.0.0.jar --spring.profiles.active=dev
```

### 4. Access

| URL | Description |
|-----|-------------|
| http://localhost:8080 | → redirects to dashboard |
| http://localhost:8080/login | Thymeleaf login page |
| http://localhost:8080/ui/dashboard | Main dashboard |
| http://localhost:8080/swagger-ui.html | Swagger UI (dev/test only) |
| http://localhost:8080/actuator/health | Health check |

---

## 👥 Sample Users

All passwords: **`Admin@123`**

| Username | Password | Role | Permissions |
|----------|----------|------|-------------|
| `admin` | Admin@123 | ROLE_ADMIN | Full access |
| `labmanager1` | Admin@123 | ROLE_LAB_MANAGER | Test catalog, reports, orders |
| `labtech1` | Admin@123 | ROLE_LAB_TECH | Samples, results, reports |
| `labtech2` | Admin@123 | ROLE_LAB_TECH | Samples, results, reports |
| `doctor1` | Admin@123 | ROLE_DOCTOR | View patients, verify results |
| `reception1` | Admin@123 | ROLE_RECEPTION | Register patients, schedule appointments |
| `billing1` | Admin@123 | ROLE_BILLING | Invoices, payments |

### Sample Patients (Seeded)

| MRN | Name | DOB | Gender | Mobile |
|-----|------|-----|--------|--------|
| MRN-000001 | John Doe | 2000-05-15 | M | 9876543210 |
| MRN-000002 | Jane Smith | 1995-08-22 | F | 9876543211 |
| MRN-000003 | Richard Roe | 1988-03-10 | M | 9876543212 |
| MRN-000004 | Emily Johnson | 2002-11-30 | F | 9876543213 |
| MRN-000005 | Michael Brown | 1975-07-04 | M | 9876543214 |
| MRN-000006 | Sarah Wilson | 1990-12-18 | F | 9876543215 |
| MRN-000007 | David Lee | 1982-04-25 | M | 9876543216 |
| MRN-000008 | Priya Patel | 1998-09-14 | F | 9876543217 |
| MRN-000009 | Ahmed Khan | 1965-01-07 | M | 9876543218 |
| MRN-000010 | Meera Nair | 1993-06-19 | F | 9876543219 |

---

## 🔄 Typical Workflow

```
1. Login as reception1 / Admin@123
2. Patients → Register new patient (or use seeded ones)
3. Appointments → Schedule appointment for patient
4. [Switch to labtech1]
5. Samples → Collect sample for appointment (barcode auto-generated)
6. Test Orders → Create test order (link sample + lab test)
7. Test Orders → Submit result (value + reference range)
8. Test Orders → Verify result
9. Reports → Generate consolidated report for appointment
10. [Switch to labmanager1]
11. Reports → Verify report
12. Reports → Release report (patient is notified)
13. Billing → Create invoice for appointment
14. Billing → Record payment
```

---

## 📡 Key API Endpoints

All REST endpoints are under `/api/**` and require a JWT Bearer token.
Obtain one via `POST /api/auth/login`.

```
POST   /api/auth/login                     → { token, username, roles }
GET    /api/auth/me                        → current user info

POST   /api/patients                       → register patient
GET    /api/patients?search=&page=&size=   → list/search patients
GET    /api/patients/{id}                  → get patient
GET    /api/patients/mrn/{mrn}             → get by MRN

POST   /api/appointments                   → schedule appointment
GET    /api/appointments                   → list appointments

POST   /api/samples                        → collect sample
GET    /api/samples/barcode/{barcode}      → get by barcode
PATCH  /api/samples/{id}/status            → update status

POST   /api/testorders                     → create test order
POST   /api/testorders/{id}/results        → submit result
POST   /api/testorders/{id}/verify         → verify result

POST   /api/reports/appointment/{id}/generate  → generate report
POST   /api/reports/{id}/verify                → verify report
POST   /api/reports/{id}/release               → release to patient
GET    /api/reports/{id}/download              → download as text

POST   /api/billing/invoices/appointment/{id}  → create invoice
POST   /api/billing/invoices/{id}/pay          → record payment

GET    /api/notifications/patient/{id}         → notifications for patient
PATCH  /api/notifications/{id}/read            → mark as read

GET    /api/admin/auditlogs                    → paged audit trail
GET    /api/admin/users                        → user management
```

Full interactive documentation at `/swagger-ui.html` (dev profile).

---

## 🗄️ Database Design

### Core Flow

```
Patient → Appointment → Sample → TestOrder ←→ LabTest
                                    ↓
                               TestResult
                                    ↓
                    Report (aggregates all TestResults for Appointment)
                                    ↓
                    Invoice → Payment
```

### Audit Fields (every table)

```sql
created_at  DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6)
updated_at  DATETIME(6)  ON UPDATE CURRENT_TIMESTAMP(6)
created_by  VARCHAR(100)  -- populated by Spring Data JPA Auditing
updated_by  VARCHAR(100)  -- populated by Spring Data JPA Auditing
is_deleted  BOOLEAN DEFAULT FALSE  -- soft-delete flag
```

### Status Lifecycles

```
Appointment: SCHEDULED → CONFIRMED → SAMPLE_COLLECTED → COMPLETED | CANCELLED
Sample:      COLLECTED → PROCESSING → ANALYSED | REJECTED
TestOrder:   ORDERED → SAMPLE_COLLECTED → IN_PROGRESS → RESULT_ENTERED → VERIFIED → COMPLETED
                                                                                   → CANCELLED
Report:      DRAFT → VERIFIED → RELEASED
Invoice:     PENDING → PAID | OVERDUE | CANCELLED
```

---

## 🧪 Running Tests

```bash
# Run all tests (unit + BDD)
mvn test

# Run specific test class
mvn test -Dtest=PatientServiceTest
mvn test -Dtest=TestOrderServiceTest

# Run BDD tests only
mvn test -Dtest=CucumberRunnerTest

# Generate JaCoCo coverage report
mvn verify
# Report: target/site/jacoco/index.html

# Run with SonarQube analysis
mvn verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=your_sonar_token
```

### Test Coverage

| Test Class | Covers | Scenarios |
|------------|--------|-----------|
| `PatientServiceTest` | PatientService | Register, duplicate checks, find, update, soft-delete |
| `TestOrderServiceTest` | TestOrderService | Create order, submit/verify result, cancel, abnormal detection |
| `PatientRegistrationSteps` | BDD | Patient registration feature (4 Cucumber scenarios) |

---

## 🔐 Security

- **REST API** (`/api/**`): Stateless JWT authentication. CSRF disabled.
- **Thymeleaf UI** (`/ui/**`): Session-based form login. CSRF enabled.
- Passwords: BCrypt (strength 10).
- Method-level RBAC: `@PreAuthorize` on all service methods.
- Sensitive endpoints (admin, actuator) restricted to `ROLE_ADMIN`.
- Swagger UI disabled in `prod` profile.

---

## 🌍 Profiles

| Profile | DB | Swagger | Data Loader | Log Level |
|---------|----|---------|-------------|-----------|
| `dev` | MySQL (local) | ✅ Enabled | ✅ Active | DEBUG |
| `test` | H2 in-memory | ❌ | ❌ | WARN |
| `stage` | MySQL (staging) | ✅ Enabled | ❌ | INFO |
| `prod` | MySQL (prod) | ❌ Disabled | ❌ | WARN |

---

## 💳 Mock Payment Gateway

`MockPaymentGateway` is the active implementation in v2.  
It always succeeds and returns a `MOCK-XXXXXXXX` transaction reference.

To swap in a real gateway in v3:
1. Add Razorpay/Stripe SDK to `pom.xml`
2. Create `RazorpayGateway implements PaymentGateway`
3. Annotate `MockPaymentGateway` with `@Profile("!prod")`
4. No changes required in `BillingService`

---

## 🗺️ Upgrade Path to v3 (Microservices)

| v2 Component | v3 Equivalent |
|-------------|---------------|
| Single Spring Boot app | Separate service JARs per domain |
| Spring Security + JWT | OAuth2 / Keycloak Identity Service |
| `MockPaymentGateway` | `RazorpayGateway` / `StripeGateway` |
| Console/DB notifications | Kafka → Email/SMS consumers |
| Flyway per-app | Flyway per-service |
| Spring Data JPA | Spring Data JPA (unchanged) |
| `@PreAuthorize` | Retained or replaced by API Gateway policies |
| Thymeleaf UI | React/Angular SPA (separate repo) |
| Single fat JAR | Docker containers → K8s Pods |

The package structure `domain/patient`, `domain/billing`, etc. already maps
1-to-1 to future microservices — no package refactoring needed.

---

## 📁 Project Structure Summary

```
medlab/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/cognizant/medlab/
    │   │   ├── MedLabApplication.java
    │   │   ├── config/          (Security, Swagger, Auditing, DevDataLoader)
    │   │   ├── domain/          (9 entity packages)
    │   │   ├── repository/      (12 JPA repositories)
    │   │   ├── application/service/ (8 business services)
    │   │   ├── web/controller/  (9 REST controllers)
    │   │   ├── web/thymeleaf/   (UiController)
    │   │   ├── web/dto/         (Request/Response DTOs)
    │   │   ├── security/        (JwtUtil, Filter, UserDetailsService)
    │   │   ├── integration/     (PaymentGateway + mock)
    │   │   └── exception/       (GlobalExceptionHandler + 3 custom exceptions)
    │   └── resources/
    │       ├── application.yml + 4 profile files
    │       ├── db/migration/V1__init_schema.sql
    │       ├── db/migration/V2__seed_data.sql
    │       ├── templates/       (Thymeleaf: login, dashboard, patients, reports, billing)
    │       └── static/css/ + static/js/
    └── test/
        ├── java/com/cognizant/medlab/
        │   ├── application/service/ (PatientServiceTest, TestOrderServiceTest)
        │   └── bdd/                 (PatientRegistrationSteps, CucumberRunnerTest)
        └── resources/
            ├── application.yml      (test profile activation)
            └── features/            (patient_registration.feature)
```
