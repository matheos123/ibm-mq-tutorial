# IBM MQ & Spring Boot TLS Certificate Management Guide

This directory documents and stores the development PKI (Public Key Infrastructure) artifacts used to secure communications between the **Spring Boot Document Processing Application** and **IBM MQ (`DOCUMENT.QM`)** via **Mutual TLS (mTLS)**.

---

## 1. Security Overview

| Participant | Authenticates | Mechanism | Key Store Type |
|---|---|---|---|
| **Spring Boot Client** | Verifies IBM MQ Server Identity | CA certificate validated via Java Truststore | PKCS12 / JKS (`truststore.p12`) |
| **IBM MQ Server** | Verifies Spring Boot Client Identity | CA certificate validated via Key Repository | PKCS12 / CMS (`key.p12` / `key.kdb`) |
| **Both (mTLS)** | Encrypted Traffic & Mutual Authentication | TLS 1.3 / TLS 1.2 with Compatible CipherSpec | Mutual Certificate Exchange |

> [!CAUTION]
> **Security Notice:**  
> These certificates and keys are strictly for **local development and testing**.  
> Never use development private keys in production.  
> As configured in [.gitignore], private keys (`*.key`), certificates with private keys, keystores (`*.p12`, `*.jks`), and password files are strictly excluded from source control.

---

## 2. Directory Layout & Artifacts

```text
infrastructure/certificates/
├── README.md                 # This documentation
├── ca/                       # Local Development Certificate Authority (CA)
│   ├── dev-ca.key            # CA Private Key (4096-bit RSA) - NEVER COMMIT
│   └── dev-ca.crt            # CA Root Certificate (X.509 PEM)
├── server/                   # IBM MQ Server Credentials
│   ├── mq-server.key         # Server Private Key (2048-bit RSA) - NEVER COMMIT
│   ├── mq-server.csr         # Server Certificate Signing Request
│   ├── mq-server.crt         # Server Certificate signed by dev-ca
│   └── mq-server-keystore.p12# PKCS12 keystore for IBM MQ server
└── client/                   # Spring Boot Client Credentials
    ├── client.key            # Client Private Key (2048-bit RSA) - NEVER COMMIT
    ├── client.csr            # Client Certificate Signing Request
    ├── client.crt            # Client Certificate signed by dev-ca
    ├── client-keystore.p12   # Client PKCS12 Keystore (contains client cert + private key)
    └── client-truststore.p12 # Client PKCS12 Truststore (contains dev-ca.crt)
```

---

## 3. IBM MQ Certificate Label Convention

IBM MQ requires exact certificate label formats for the queue manager's personal certificate:
- **Format:** `ibmwebspheremq<qmgr_name_lowercase>`
- **Queue Manager:** `DOCUMENT.QM`
- **Required Certificate Label:** `ibmwebspheremqdocumentqm`

If the label does not match this format, the queue manager will fail with `AMQ9660E: SSL key repository: personal certificate not found`.

---

## 4. Cipher Specification & Compatibility

IBM MQ uses CipherSpecs, while Java uses CipherSuites. They must be compatible:

| IBM MQ CipherSpec | Java / Spring Boot CipherSuite | Protocol |
|---|---|---|
| `TLS_RSA_WITH_AES_128_GCM_SHA256` | `TLS_RSA_WITH_AES_128_GCM_SHA256` | TLS 1.2 |
| `TLS_AES_128_GCM_SHA256` | `TLS_AES_128_GCM_SHA256` | TLS 1.3 |
| `ANY_TLS12_OR_HIGHER` | Negotiated TLS 1.2+ Suite | TLS 1.2 / 1.3 |

---

## 5. End-to-End Generation Roadmap

1. **Step 29:** Generate Development CA (`dev-ca.key`, `dev-ca.crt`).
2. **Step 30:** Generate IBM MQ Server Private Key & CSR, signed by `dev-ca`.
3. **Step 31:** Generate Spring Boot Client Private Key & CSR, signed by `dev-ca`.
4. **Step 32:** Package private keys and certificates into PKCS12 keystores (`mq-server-keystore.p12` and `client-keystore.p12`).
5. **Step 33:** Create Spring Boot Java Truststore (`client-truststore.p12`) containing `dev-ca.crt` using `keytool`.
6. **Step 34:** Mount server keystore into IBM MQ container and configure SVRCONN channel with `SSLCIPH`.
7. **Step 35:** Update Spring Boot `application.yml` with keystore and truststore paths and passwords.
8. **Step 36:** Verify encrypted one-way TLS communication.
9. **Step 37:** Enable Mutual TLS (`SSLCAUTH(REQUIRED)`) on IBM MQ channel.
10. **Step 38:** Execute negative security test cases (untrusted cert, wrong password, expired cert).

---

## 6. Negative Security Test Scenarios & Diagnostics

To guarantee defense-in-depth and operational reliability, the table below documents the negative test cases, expected behaviors, diagnostic error codes, and troubleshooting steps.

### Test Scenario 1: Untrusted Certificate
* **Condition:** A client or server presents a certificate signed by an unknown CA (not present in the peer's truststore).
* **Expected Client Behavior:** TLS handshake aborts before connection establishment.
* **Client Diagnostic (Spring Boot / JSSE):**
  ```text
  javax.net.ssl.SSLHandshakeException: PKIX path building failed:
  sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
  com.ibm.mq.MQException: JMSCMQ0001: IBM MQ call failed with compcode '2' ('MQCC_FAILED') reason '2397' ('MQRC_JSSE_ERROR').
  ```
* **Server Diagnostic (IBM MQ AMQERR01.LOG):**
  ```text
  AMQ9633E: Bad SSL certificate for channel '????'.
  CommentInsert2: gsk_environment_init / Certificate validation failed (GSKit return code 414 / GSKVAL_ERR_NO_CHAIN_FOUND).
  ```
* **Remediation:** Ensure the issuing root CA (`dev-ca.crt`) is imported into the truststore (`client-truststore.p12` for Spring Boot, and `key.kdb` for IBM MQ).

---

### Test Scenario 2: Incorrect Keystore / Truststore Password
* **Condition:** Spring Boot is started with an incorrect password for `keyStorePassword` or `trustStorePassword` in `application.yml`.
* **Expected Client Behavior:** Application startup fails during JMS connection factory initialization.
* **Client Diagnostic:**
  ```text
  java.io.IOException: Keystore was tampered with, or password was incorrect
  Caused by: java.security.UnrecoverableKeyException: Password verification failed
  org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jmsConnectionFactory'
  ```
* **Remediation:** Verify that the configured password matches the password supplied when creating the PKCS12 keystore (`keytool -list -keystore client-keystore.p12 -storepass <password>`).

---

### Test Scenario 3: Missing Client Certificate (Mutual TLS Enforcement)
* **Condition:** IBM MQ channel `DEV.APP.SVRCONN` has `SSLCAUTH(REQUIRED)`, but a connecting client does not provide a client certificate (e.g., empty keystore or standard TLS without keystore).
* **Expected Behavior:** IBM MQ immediately terminates the handshake during client certificate verification.
* **Server Diagnostic (IBM MQ AMQERR01.LOG):**
  ```text
  AMQ9637E: Channel is lacking a SSL client certificate for channel 'DEV.APP.SVRCONN'.
  AMQ9999E: Channel 'DEV.APP.SVRCONN' ended abnormally.
  ```
* **Client Diagnostic:**
  ```text
  javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure
  com.ibm.mq.MQException: JMSCMQ0001: IBM MQ call failed with compcode '2' ('MQCC_FAILED') reason '2397' ('MQRC_JSSE_ERROR').
  ```
* **Remediation:** Provide a valid client certificate and private key via `ibm.mq.jks.keyStore` in `application.yml` and ensure the client certificate is signed by a CA trusted in MQ's `key.kdb`.

---

### Test Scenario 4: Expired Certificate
* **Condition:** A certificate's `notAfter` timestamp is earlier than current system time, or `notBefore` is in the future.
* **Expected Behavior:** Handshake is rejected during certificate path validation.
* **Client Diagnostic:**
  ```text
  java.security.cert.CertificateExpiredException: NotAfter: Sun Nov 15 12:00:00 UTC 2026
  javax.net.ssl.SSLHandshakeException: Certificate expired
  ```
* **Server Diagnostic (IBM MQ AMQERR01.LOG):**
  ```text
  AMQ9635E: An SSL certificate has expired for channel 'DEV.APP.SVRCONN'.
  CommentInsert1: NotAfter date check failed (GSKit return code 406 / GSK_ERROR_CERT_EXPIRED).
  ```
* **Remediation:** Check certificate validity using `openssl x509 -in client.crt -noout -dates` or `keytool -list -v -keystore client-keystore.p12`. Re-issue and sign a renewed certificate from the CA.

---

## 7. Diagnostic Quick Reference

| Error Code / ID | Component | Cause | Resolution |
|---|---|---|---|
| **2397 (`MQRC_JSSE_ERROR`)** | Spring Boot | General Java SSL/TLS handshake error (cipher mismatch, untrusted CA, missing client cert). | Inspect `javax.net.debug=ssl:handshake` and MQ `AMQERR01.LOG`. |
| **`AMQ9657E`** | IBM MQ | Key repository (`SSLKEYR`) cannot be opened (permissions, bad path, missing `.sth` stash). | Convert to CMS (`.kdb`), create stash (`.sth`), set permissions `chmod 666`. |
| **`AMQ9660E`** | IBM MQ | Personal certificate not found matching label convention `ibmwebspheremq<qmgr>`. | Ensure label is lowercased (e.g. `ibmwebspheremqdocumentqm`). |
| **`AMQ9637E`** | IBM MQ | Client certificate missing when channel has `SSLCAUTH(REQUIRED)`. | Configure Spring Boot `ibm.mq.jks.keyStore` with client PKCS12 cert. |
| **`AMQ9633E`** | IBM MQ | Client certificate failed CA verification. | Import CA root certificate into queue manager key repository (`key.kdb`). |
| **`AMQ9635E`** | IBM MQ | Certificate validity expired. | Re-generate and sign new certificate with updated validity period. |

