# Adaptive Knowledge Engine

An enterprise-grade, non-blocking asynchronous document ingestion, retrieval, and Query Transformation RAG (Retrieval-Augmented Generation) system. Built with **Spring Boot 3**, **Java 21 Virtual Threads (Project Loom)**, **LangChain4j**, and **PostgreSQL**, this engine supports multi-modal PDF parsing, vectorization, token-aware query transformation, and intelligent answer generation.

---

# Architecture

```text
                 ┌─────────────────────────────────────────┐
                 │               Client App                │
                 └────────────────────┬────────────────────┘
                                      │
                    POST /upload      │  POST /query
                                      ▼
 ┌────────────────────────────────────────────────────────────────────────┐
 │                     Spring Boot API (Java 21 Loom)                     │
 └───────────────────┬────────────────────────────────┬───────────────────┘
                     │                                │
        Async Ingestion (Virtual Thread)      Query Transformation
                     │                                │
                     ▼                                ▼
          ┌─────────────────────┐          ┌─────────────────────┐
          │   Apache PDFBox     │          │  Token-Aware Query  │
          │  Parsing & Chunking │          │ Expansion & Rewrite │
          └──────────┬──────────┘          └──────────┬──────────┘
                     │                                │
                     ▼                                ▼
          ┌─────────────────────┐          ┌─────────────────────┐
          │   Local Embedder    │          │    Vector Search    │
          │(all-minilm-l6-v2)   │          │     & RAG Engine    │
          └──────────┬──────────┘          └──────────┬──────────┘
                     │                                │
                     ▼                                ▼
          ┌─────────────────────┐          ┌─────────────────────┐
          │ PostgreSQL Database │          │ Intelligent Answer  │
          │   (CDM Storage)     │          │    Generation       │
          └─────────────────────┘          └─────────────────────┘
```

---

# Features

- **Asynchronous Document Ingestion**
  - Uses Java 21 Virtual Threads (`@EnableAsync`) for non-blocking background processing.
  - Upload requests immediately return **HTTP 202 Accepted** with a tracking ID while parsing and embedding continue asynchronously.

- **Canonical Document Model (CDM)**
  - Normalized relational schema consisting of:
    - `documents`
    - `document_versions`
    - `chunks`
  - Supports document metadata, lifecycle management, and version history.

- **Local Embedding Pipeline**
  - Generates embeddings locally using:
    - `all-minilm-l6-v2`
    - DJL
    - ONNX Runtime
  - Eliminates dependency on external embedding APIs.

- **Token-Aware Query Transformation**
  - Removes conversational filler.
  - Expands abbreviations.
  - Normalizes user queries before vector search.
  - Produces cleaner semantic retrieval.

- **Persistent Storage**
  - PostgreSQL stores:
    - Documents
    - Chunk metadata
    - Embeddings metadata
    - Version history

- **Enterprise Document Support**
  - Supports PDF uploads up to **50 MB**.

---

# Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.5 |
| AI Framework | LangChain4j 0.33.0 |
| Database | PostgreSQL |
| ORM | Spring Data JPA + Hibernate 6 |
| Embedding Model | all-minilm-l6-v2 (ONNX Runtime) |
| PDF Parsing | Apache PDFBox 3.0.1 |
| Connection Pool | HikariCP |
| Build Tool | Maven |

---

# Prerequisites

- Java 21+
- PostgreSQL
- Maven

---

# Database Setup

Create the database:

```sql
CREATE DATABASE knowledge_engine;
```

---

# Configuration

Update your `application.yml`.

```yaml
spring:
  application:
    name: adaptive-knowledge

  threads:
    virtual:
      enabled: true

  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

  datasource:
    url: jdbc:postgresql://localhost:5432/knowledge_engine
    username: postgres
    password: your_postgres_password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

> **Note:** The application sets the JVM timezone to UTC (`TimeZone.setDefault(TimeZone.getTimeZone("UTC"))`) to ensure consistent timestamp handling and PostgreSQL compatibility.

---

# Running the Application

## Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/adaptive-knowledge-engine.git
cd adaptive-knowledge-engine
```

## Build

```bash
./mvnw clean compile
```

## Run

```bash
./mvnw spring-boot:run
```

The application starts on:

```
http://localhost:8080
```

Hibernate automatically creates the required database tables during startup.

---

# 📡 API Reference

## 1. Upload Document

Uploads a document for asynchronous parsing, chunking, embedding, and persistence.

### Endpoint

```
POST /api/v1/ingestion/upload
```

### Content Type

```
multipart/form-data
```

### Example

```bash
curl -i -X POST http://localhost:8080/api/v1/ingestion/upload \
  -F "file=@/path/to/document.pdf" \
  -F "title=Enterprise Security Policy" \
  -F "domain=Security" \
  -F "type=Policy"
```

### Response (202 Accepted)

```json
{
  "status": "QUEUED",
  "trackingId": "ab0def0a-df01-4130-81a8-570ec85c1cd1",
  "message": "Document submitted for background transformation and vectorization."
}
```

---

## 2. Query Knowledge Base

Performs:

- Query transformation
- Semantic retrieval
- Answer synthesis

### Endpoint

```
POST /api/v1/query/search
```

### Content Type

```
application/json
```

### Example

```bash
curl -X POST http://localhost:8080/api/v1/query/search \
-H "Content-Type: application/json" \
-d '{
  "query":"What are the updated password complexity requirements in the security policy?",
  "domain":"Security"
}'
```

### Response (200 OK)

```json
{
  "transformedQuery": "security policy password complexity minimum length symbol rules",
  "answer": "According to Section 4.2 of the Enterprise Security Policy, passwords must be a minimum of 16 characters and contain at least one special character, one number, and one uppercase letter.",
  "sources": [
    {
      "documentTitle": "Enterprise Security Policy",
      "chunkId": "c1f729e2-8921-4f10-b99d-192a2a0951bd",
      "relevanceScore": 0.94
    }
  ]
}
```

---

# Database Schema

### documents

Stores the primary document metadata.

Fields include:

- Document title
- Domain
- Active version
- Creation metadata

---

### document_versions

Maintains document history.

Tracks:

- ACTIVE versions
- ARCHIVED versions
- Version lifecycle

---

### chunks

Stores parsed text chunks generated during ingestion.

Contains:

- Chunk sequence
- Chunk text
- Metadata
- Parent document mapping

---

# Highlights

- Java 21 Virtual Threads
- Fully asynchronous ingestion
- Local ONNX embedding pipeline
- Token-aware query transformation
- PostgreSQL-backed Canonical Document Model
- LangChain4j integration
- Enterprise-ready PDF ingestion
- Modular Spring Boot architecture

---

## 📄 License

This project is intended for educational and demonstration purposes.
