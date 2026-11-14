# Secure Document Processing System
## Project Functionality & Development Roadmap

**Project Type:** Spring Boot + IBM MQ Messaging Application  
**Purpose:** Learning and practicing enterprise messaging, security, and testing concepts.

---

# PART 1: PROJECT FUNCTIONALITY

## 1. Project Overview

Build a simple Secure Document Processing System using Spring Boot and IBM MQ.

The application allows users to submit document processing requests through REST APIs. Requests are published to IBM MQ, consumed by a message listener, processed, and stored in PostgreSQL.

The application should demonstrate reliable message processing, error handling, message recovery, and secure communication between Spring Boot and IBM MQ.

Actual document processing is not required. Simulate document validation and processing.

## 2. Technology Stack

- Java 21+
- Spring Boot
- Maven
- IBM MQ
- Spring JMS
- PostgreSQL
- Spring Data JPA
- JUnit 5
- Mockito
- Spring Boot Test
- Docker / Docker Compose
- OpenSSL
- Java Keytool
- Postman

## 3. Core Functionalities

### 3.1 Document Submission

Users can submit document processing requests through a REST API.

Each document should contain:

- Document ID
- File name
- Document type
- Description
- Submission date
- Processing status

The application should generate a unique document ID.

Initially, only document metadata is required. Actual file storage and uploads are outside the MVP scope.

### 3.2 Message Publishing

After receiving a document submission:

1. Validate the request.
2. Save the document in PostgreSQL with `PENDING` status.
3. Create a processing request message.
4. Publish the message to IBM MQ.
5. Use persistent message delivery.

Queue name:

`DOCUMENT.REQUEST.QUEUE`

The message should contain:

- Document ID
- Document name
- Document type
- Correlation ID
- Submission timestamp

### 3.3 Message Consumption

Implement a JMS consumer using Spring Boot's `@JmsListener`.

The consumer should:

1. Receive a message from IBM MQ.
2. Read its metadata.
3. Update document status to `PROCESSING`.
4. Validate the document.
5. Simulate document processing.
6. Save the processing result in PostgreSQL.
7. Publish a successful processing event.

Queue name:

`DOCUMENT.SUCCESS.QUEUE`

### 3.4 Processing Status

Support these statuses:

- `PENDING`
- `PROCESSING`
- `COMPLETED`
- `FAILED`

Users should be able to retrieve document information and its current processing status.

### 3.5 Error Handling

The application should demonstrate:

- Exception handling.
- JMS transaction rollback.
- Message redelivery.
- Backout count.
- Poison messages.
- Backout queue.
- Dead Letter Queue (DLQ).

Configure:

`DOCUMENT.BACKOUT.QUEUE`

Use the IBM MQ queue manager's configured dead-letter queue:

`SYSTEM.DEAD.LETTER.QUEUE`

Repeatedly failing messages should be routed to the backout queue according to the configured backout threshold and application recovery logic.

Business validation failures, such as unsupported document types, should be handled separately from technical processing failures.

### 3.6 Message Recovery

Provide a basic recovery mechanism.

An administrator should be able to:

- Inspect failed document processing records.
- Identify the associated message.
- View the error message and retry count.
- Request processing again after correcting the issue.

Do not implement automatic infinite retries.

### 3.7 Database Integration

Use PostgreSQL with Spring Data JPA.

Create two main tables.

**documents**

| Field | Type |
|---|---|
| id | UUID |
| file_name | VARCHAR |
| document_type | VARCHAR |
| description | TEXT |
| status | VARCHAR |
| created_at | TIMESTAMP |
| updated_at | TIMESTAMP |

**processing_logs**

| Field | Type |
|---|---|
| id | UUID |
| document_id | UUID |
| message_id | VARCHAR |
| correlation_id | VARCHAR |
| status | VARCHAR |
| retry_count | INTEGER |
| error_message | TEXT |
| created_at | TIMESTAMP |

Use Flyway for database migrations.

### 3.8 Secure IBM MQ Communication

Configure SSL/TLS between the Spring Boot application and IBM MQ.

Practice the following:

- X.509 certificates.
- Certificate Authority (CA).
- Private and public keys.
- PEM certificate format.
- PKCS12 format.
- Java KeyStore (JKS).
- Keystore and Truststore.
- Certificate import and export.
- OpenSSL.
- Java Keytool.
- IBM MQ TLS configuration.
- Cipher specifications.
- Mutual TLS authentication.

The Spring Boot application should authenticate the MQ server using its truststore.

For mutual TLS, IBM MQ should also authenticate the Spring Boot client certificate.

Use development certificates only.

Never commit private keys or passwords to source control.

### 3.9 Testing

Use JUnit 5, Mockito, and Spring Boot Test.

Implement tests for:

- Document submission.
- Successful document processing.
- Invalid document requests.
- Database persistence.
- Message publishing.
- Message consumption.
- Exception handling.
- Transaction rollback.
- Duplicate message handling.
- Message recovery.

Include integration tests with PostgreSQL and IBM MQ where possible.

---

# PART 2: REST API ENDPOINTS

Implement these endpoints:

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/documents` | Submit a document |
| GET | `/api/documents` | Retrieve all documents |
| GET | `/api/documents/{id}` | Retrieve document details |
| GET | `/api/documents/{id}/history` | Retrieve processing history |
| POST | `/api/documents/{id}/retry` | Retry failed processing |

Use appropriate HTTP status codes and validation responses.

---

# PART 3: IBM MQ CONFIGURATION

Use one queue manager:

`DOCUMENT.QM`

Create the following queues:

| Queue | Purpose |
|---|---|
| DOCUMENT.REQUEST.QUEUE | Incoming processing requests |
| DOCUMENT.SUCCESS.QUEUE | Successful processing events |
| DOCUMENT.BACKOUT.QUEUE | Repeatedly failing messages |
| SYSTEM.DEAD.LETTER.QUEUE | Undeliverable messages |

Configure:

- `DEADQ`
- `BOTHRESH`
- `BOQNAME`

Configure the backout threshold to a small development value, such as 3.

Remember that configuring `BOTHRESH` and `BOQNAME` alone does not automatically move messages. The consumer must implement backout handling.

---

# PART 4: DEVELOPMENT ROADMAP

## IMPORTANT INSTRUCTIONS FOR THE CODING AGENT

Develop this project incrementally.

**Do not generate the entire application at once.**

Follow the development steps in order.

For every step:

1. Explain what you are about to implement.
2. Identify the exact file to create or modify.
3. Implement only that file.
4. Explain important code and configuration.
5. Verify imports, dependencies, and compilation where applicable.
6. Report the result.
7. Wait for approval before proceeding to the next file.

Do not create multiple files in a single step unless explicitly instructed.

Do not skip ahead.

Do not introduce unnecessary abstractions or additional technologies.

Keep the implementation simple and educational.

## PHASE 1: Project Foundation

**Step 1 — Maven configuration**

Create:

`pom.xml`

Add dependencies for:

- Spring Boot Web
- Spring Boot JMS
- IBM MQ Starter
- Spring Data JPA
- PostgreSQL Driver
- Validation
- Flyway
- Lombok (optional)
- Spring Boot Test
- Mockito

Use compatible dependency versions and document any IBM MQ-specific configuration requirements.

**Step 2 — Application entry point**

Create:

`src/main/java/com/documentprocessor/DocumentProcessorApplication.java`

Implement the Spring Boot main class.

**Step 3 — Application configuration**

Create:

`src/main/resources/application.yml`

Configure:

- Application name.
- Server port.
- PostgreSQL connection.
- JPA settings.
- IBM MQ connection placeholders.
- Logging.
- Environment-based credentials.

**Step 4 — Git configuration**

Create:

`.gitignore`

Exclude:

- Target directory.
- IDE files.
- Environment files.
- Private keys.
- Keystores.
- Truststores.
- Certificate passwords and secrets.

**Phase 1 acceptance criteria:**

- Maven dependencies resolve.
- Application starts.
- PostgreSQL connection works.
- No MQ functionality is required yet.

## PHASE 2: Database and Domain Model

**Step 5 — Document entity**

Create:

`src/main/java/com/documentprocessor/document/entity/Document.java`

Define the document entity and its fields.

**Step 6 — Processing status enum**

Create:

`src/main/java/com/documentprocessor/document/enums/DocumentStatus.java`

Define PENDING, PROCESSING, COMPLETED, and FAILED.

**Step 7 — Processing log entity**

Create:

`src/main/java/com/documentprocessor/log/entity/ProcessingLog.java`

Define message tracking and error information.

**Step 8 — Document repository**

Create:

`src/main/java/com/documentprocessor/document/repository/DocumentRepository.java`

Extend JpaRepository.

**Step 9 — Processing log repository**

Create:

`src/main/java/com/documentprocessor/log/repository/ProcessingLogRepository.java`

Extend JpaRepository.

**Step 10 — Database migration**

Create:

`src/main/resources/db/migration/V1__create_document_tables.sql`

Create the required tables and constraints.

**Phase 2 acceptance criteria:**

- Application connects to PostgreSQL.
- Flyway migration succeeds.
- Entities can be persisted and retrieved.

## PHASE 3: REST API

**Step 11 — Document request DTO**

Create:

`src/main/java/com/documentprocessor/document/dto/DocumentRequest.java`

Define and validate the submission request.

**Step 12 — Document response DTO**

Create:

`src/main/java/com/documentprocessor/document/dto/DocumentResponse.java`

Define the API response.

**Step 13 — Document service**

Create:

`src/main/java/com/documentprocessor/document/service/DocumentService.java`

Implement document creation and retrieval.

**Step 14 — Document controller**

Create:

`src/main/java/com/documentprocessor/document/controller/DocumentController.java`

Implement:

- POST /api/documents
- GET /api/documents
- GET /api/documents/{id}

**Step 15 — Global exception handler**

Create:

`src/main/java/com/documentprocessor/common/GlobalExceptionHandler.java`

Implement consistent API error responses.

**Phase 3 acceptance criteria:**

- Submit a document through Postman.
- Retrieve submitted documents.
- Verify PostgreSQL records.
- Validate invalid requests.

## PHASE 4: IBM MQ Integration

**Step 16 — IBM MQ configuration**

Create:

`src/main/java/com/documentprocessor/messaging/config/IbmMqConfig.java`

Configure the IBM MQ connection factory and JMS infrastructure.

**Step 17 — Message DTO**

Create:

`src/main/java/com/documentprocessor/messaging/dto/DocumentMessage.java`

Define the message structure.

**Step 18 — Message publisher**

Create:

`src/main/java/com/documentprocessor/messaging/producer/DocumentMessageProducer.java`

Publish document processing requests to:

`DOCUMENT.REQUEST.QUEUE`

Use persistent delivery and appropriate message metadata.

**Step 19 — Update document service**

Modify:

`src/main/java/com/documentprocessor/document/service/DocumentService.java`

After saving the document, publish its processing request.

Handle publishing failures without leaving misleading document statuses.

**Step 20 — Message consumer**

Create:

`src/main/java/com/documentprocessor/messaging/consumer/DocumentMessageConsumer.java`

Implement a JMS listener for DOCUMENT.REQUEST.QUEUE.

Initially, log the received message and verify successful consumption.

**Phase 4 acceptance criteria:**

- REST API publishes messages.
- IBM MQ receives messages.
- Spring Boot consumer receives messages.
- Message IDs and correlation IDs can be inspected.

## PHASE 5: Processing and Reliability

**Step 21 — Processing service**

Create:

`src/main/java/com/documentprocessor/document/service/DocumentProcessingService.java`

Implement simulated document validation and processing.

**Step 22 — Connect consumer and processing service**

Modify:

`DocumentMessageConsumer.java`

Delegate processing to DocumentProcessingService.

**Step 23 — Processing audit**

Update the processing service to persist processing history.

**Step 24 — Transaction handling**

Configure appropriate JMS transaction behavior.

Demonstrate:

- Successful commit.
- Exception and rollback.
- Message redelivery.

**Step 25 — Backout handling**

Implement backout count inspection and routing to DOCUMENT.BACKOUT.QUEUE.

Configure BOTHRESH and BOQNAME on the IBM MQ request queue.

**Step 26 — DLQ demonstration**

Configure the queue manager's dead-letter queue and demonstrate an undeliverable message.

**Step 27 — Duplicate protection**

Prevent duplicate document processing using a unique document or processing request identifier.

**Phase 5 acceptance criteria:**

- Successful messages are processed once logically.
- Failed messages are redelivered when appropriate.
- Poison messages are isolated.
- Backout and DLQ behavior can be demonstrated.

## PHASE 6: Certificate Management and TLS

**Step 28 — Certificate directory**

Create:

`infrastructure/certificates/README.md`

Document the certificate generation and storage procedure.

**Step 29 — Generate CA certificate**

Use OpenSSL to generate a development CA and its certificate.

**Step 30 — Generate server certificate**

Generate an IBM MQ server private key and certificate signing request.

Sign the server certificate using the development CA.

**Step 31 — Generate client certificate**

Generate a Spring Boot client private key and certificate signing request.

Sign it using the development CA.

**Step 32 — Create PKCS12 keystores**

Create PKCS12 keystores for the client and server.

**Step 33 — Configure Java truststore**

Use Java Keytool to import the CA certificate into the Spring Boot truststore.

**Step 34 — Configure IBM MQ TLS**

Configure the MQ server-side key repository, certificate label, channel TLS settings, and compatible CipherSpec.

**Step 35 — Configure Spring Boot TLS**

Update IBM MQ connection configuration to use the client keystore and truststore.

**Step 36 — Test TLS**

Verify successful encrypted communication.

**Step 37 — Configure mutual TLS**

Configure IBM MQ to require and validate the client certificate.

**Step 38 — Negative security tests**

Test:

- Untrusted certificate.
- Incorrect keystore password.
- Missing client certificate.
- Expired certificate.

Document the expected connection failures.

**Phase 6 acceptance criteria:**

- Spring Boot connects using TLS.
- Mutual TLS authentication works.
- Certificate failures are understood and documented.

## PHASE 7: JUnit Testing

**Step 39 — Document service tests**

Create:

`src/test/java/com/documentprocessor/document/service/DocumentServiceTest.java`

Test document submission and retrieval.

**Step 40 — Processing service tests**

Create:

`src/test/java/com/documentprocessor/document/service/DocumentProcessingServiceTest.java`

Test successful and failed processing.

**Step 41 — Producer tests**

Create:

`src/test/java/com/documentprocessor/messaging/producer/DocumentMessageProducerTest.java`

Test message publishing using mocks.

**Step 42 — Consumer tests**

Create:

`src/test/java/com/documentprocessor/messaging/consumer/DocumentMessageConsumerTest.java`

Test message consumption and exception handling.

**Step 43 — Integration tests**

Create integration tests for PostgreSQL and IBM MQ using dedicated test infrastructure.

Verify transaction rollback, redelivery, and recovery.

**Phase 7 acceptance criteria:**

- Unit tests pass.
- Integration tests pass.
- Failure scenarios are tested.
- No external test dependency is required for ordinary unit tests.

## PHASE 8: Documentation and Presentation

**Step 44 — Project README**

Create:

`README.md`

Include:

- Project overview.
- Technology stack.
- Architecture.
- Prerequisites.
- Installation.
- Configuration.
- Running instructions.
- API endpoints.
- IBM MQ setup.
- TLS configuration.
- Testing instructions.

**Step 45 — Architecture documentation**

Create:

`docs/architecture.md`

Explain the message lifecycle and component relationships.

**Step 46 — IBM MQ learning notes**

Create:

`docs/ibm-mq-concepts.md`

Explain the IBM MQ concepts demonstrated in the project.

**Step 47 — Security documentation**

Create:

`docs/security-and-certificates.md`

Explain PEM, PKCS12, JKS, keystores, truststores, TLS, and mutual TLS.

---

# PART 5: FINAL DEVELOPMENT RULES

1. Follow the roadmap sequentially.
2. Work on exactly one file at a time.
3. Never generate the whole project in one response.
4. Explain unfamiliar IBM MQ concepts before implementing them.
5. Prefer simple, readable Java code.
6. Use constructor injection.
7. Use DTOs instead of exposing JPA entities through REST APIs.
8. Use proper exception handling.
9. Keep secrets outside source control.
10. Verify each milestone before continuing.
11. Do not claim a feature works without testing it.
12. If a step fails, diagnose and fix it before proceeding.

**Important:** The project is primarily an IBM MQ learning project. Prioritize understanding message delivery, transactions, recovery, TLS, certificates, and security over building unnecessary business functionality.

## Final Expected Result

A working Spring Boot application demonstrating:

- REST API integration.
- IBM MQ producer and consumer.
- PostgreSQL persistence.
- Persistent messaging.
- Message acknowledgment and rollback.
- Message redelivery.
- Poison message recovery.
- Backout Queue and DLQ.
- SSL/TLS and mutual TLS.
- PEM, PKCS12, JKS, Keystore and Truststore.
- JUnit and Mockito testing.
