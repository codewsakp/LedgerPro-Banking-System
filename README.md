# 💳 LedgerPro — Banking & Transaction Management System
A full fledged **banking application** built with **Spring Boot**, designed to simulate real-world financial operations.  

# LedgerPro is a Java-based banking application that provides account management, secure authentication, balance inquiries, deposits, withdrawals, fund transfers, transaction history, and bank statement generation.

The project uses Spring Boot for backend development, MySQL for persistent storage, and Spring Security with JWT-based authentication.

> **Project note:** This backend is adapted from the open-source [Spring Boot Banking System](https://github.com/tamaraghosn/springboot-banking-system.git), with additional configuration, modifications, and integration work.
> Frontend: https://github.com/codewsakp/LedgerPro-Banking-System-Frontend.git
## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.5.5
- **Security:** Spring Security, JWT
- **ORM:** Spring Data JPA, Hibernate
- **Database:** MySQL
- **Build Tool:** Maven
- **Email:** Spring Boot Starter Mail
- **PDF Generation:** iText
- **API Testing:** Postman
- **Documentation:** Springdoc OpenAPI&#x20;
- **Version Control:** Git and GitHub

## Features

### Account Management

- Register a new bank account.
- Generate a unique account number.
- Store customer information.
- Securely hash passwords.

### Authentication and Security

- User login using email and password.
- JWT-based authentication.
- Role-based authorization.
- Protected banking operations.

### Banking Operations

- Balance inquiry.
- Account name inquiry.
- Deposit money.
- Withdraw money.
- Transfer money between accounts.
- Transaction record creation.

### Transaction Management

- View transaction history.
- Store transaction details in MySQL.
- Maintain debit and credit records for transfers.
- Use database transactions for banking operations.
- Apply pessimistic write locking to protect account balance updates from concurrent modifications.

### Bank Statements

- Generate bank statements for a selected date range.
- Create PDF statements.
- Send generated statements through email.

## Project Structure

```text
src/
└── main/
    ├── java/
    │   └── com/tamara/BankingAppSpringBoot/
    │       ├── config/
    │       ├── controller/
    │       ├── dto/
    │       ├── entity/
    │       ├── repo/
    │       ├── service/
    │       └── ...
    └── resources/
        └── application.properties
```

## Prerequisites

Install the following:

- JDK 21 or compatible JDK.
- MySQL Server.
- MySQL Workbench 
- IntelliJ IDEA or another Java IDE.
- Postman.
- Git.

## Database Configuration

Create the database in MySQL:

```sql
CREATE DATABASE ledgerpro_db;
```

Configure the database connection in `src/main/resources/application.properties`.

Do not commit database passwords or other secrets to GitHub.

The application uses environment variables for sensitive configuration, including:

- `DB_PASSWORD`
- `MAIL_PASSWORD`
- `JWT_SECRET` (if configured in your application properties)

Configure the corresponding database username, mail username, and other properties in your local environment or application configuration.

For Gmail SMTP, use a Google App Password rather than your normal Gmail account password.

## Run the Application

Clone the repository:

```bash
git clone https://github.com/codewsakp/LedgerPro-Banking-System.git
cd springboot-banking-system
```

Run the tests:

```bash
.\mvnw.cmd clean test
```

Start the Spring Boot application:

```bash
.\mvnw.cmd spring-boot:run
```

The backend runs at:

```text
http://localhost:8080
```

## API Endpoints

| Method | Endpoint                                                          | Description                        |
| ------ | ----------------------------------------------------------------- | ---------------------------------- |
| POST   | `/api/users/create`                                               | Register a new account             |
| POST   | `/api/users/login`                                                | Authenticate a user                |
| GET    | `/api/users/user/balanceInquiry/{accountNumber}`                  | Check account balance              |
| GET    | `/api/users/user/nameInquiry/{accountNumber}`                     | Retrieve account name              |
| POST   | `/api/users/user/credit`                                          | Deposit money                      |
| POST   | `/api/users/user/debit`                                           | Withdraw money                     |
| POST   | `/api/users/user/transfer`                                        | Transfer money                     |
| GET    | `/api/transactions?accountNumber={accountNumber}`                 | Retrieve transaction history       |
| GET    | `/api/statements/{accountNumber}?startDate={date}&endDate={date}` | Generate and email a PDF statement |

Protected endpoints require a valid JWT token in the request header:

```http
Authorization: Bearer YOUR_JWT_TOKEN
```

## Transaction Consistency

Banking operations use transactional database operations to maintain data integrity.

- `@Transactional` manages transaction boundaries.
- Pessimistic write locks are used when accessing account records for balance modifications.
- Transfers update both participating accounts within a transaction.
- Consistent account-lock ordering helps reduce deadlock risk.

Concurrency and rollback behavior should be verified with dedicated tests before production deployment.

## Testing

Use Postman to test:

1. Account registration and login.
2. Balance and account-name inquiries.
3. Deposits and withdrawals.
4. Transfers between accounts.
5. Transaction history.
6. PDF statement generation and email delivery.
7. Unauthorized access and invalid requests.

Run automated tests using:

```bash
.\mvnw.cmd clean test
```
## Author

**Sakshi Patil**
GitHub: https://github.com/codewsakp/LedgerPro-Banking-System.git

