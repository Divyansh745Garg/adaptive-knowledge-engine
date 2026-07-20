# Adaptive Knowledge Change Engine

> A production-grade Retrieval-Augmented Generation (RAG) platform designed for **continuously evolving enterprise knowledge**. Unlike traditional RAG systems that assume documents are static, this engine detects content mutations, performs incremental indexing, preserves document history, and enables version-aware retrieval using a Canonical Document Model (CDM).

---

# The Engineering Problem

Most Retrieval-Augmented Generation (RAG) tutorials demonstrate a simple workflow:

1. Upload a few PDFs.
2. Generate embeddings.
3. Store them in a vector database.
4. Query the knowledge base.

While this works for demonstrations, it breaks down in production.

Enterprise knowledge is never static.

Examples include:

- HR policies
- Government regulations
- Banking compliance documents
- Insurance policies
- Product documentation
- API references
- Standard Operating Procedures (SOPs)

These documents evolve continuously.

If a single paragraph changes inside a 200-page PDF, most traditional RAG pipelines:

- Re-parse the entire document
- Re-generate embeddings for every chunk
- Re-upload everything to the vector database
- Overwrite previous knowledge
- Lose historical context

This approach is computationally expensive, increases API costs, wastes embedding computation, and completely removes the ability to answer questions like:

- *What changed between Policy Version 3 and Version 4?*
- *Which compliance rule was active in January 2025?*
- *How has this government scheme evolved over time?*

---

# The Solution

Adaptive Knowledge Change Engine introduces a **Canonical Document Model (CDM)** that separates business documents from the retrieval pipeline.

Instead of treating every upload as a brand-new document, the engine identifies whether:

- the document is completely new,
- unchanged,
- or an updated version of an existing document.

Only changed content is processed.

Previous versions remain available for auditing and historical reasoning.

---

# Core Features

## Incremental Indexing

Every uploaded document is assigned an MD5 checksum.

Before any expensive processing begins, the checksum is compared against the latest active version.

If the checksum matches:

- PDF parsing is skipped
- Chunking is skipped
- Embedding generation is skipped
- Vector database writes are skipped

Result:

- Zero unnecessary compute
- Zero embedding cost
- Instant response

---

## Version-Aware Document Lifecycle

Instead of deleting documents, the engine maintains a complete version history.

Document lifecycle:

```
ACTIVE
   │
New upload detected
   │
   ▼
SUPERSEDED
   │
New Version becomes ACTIVE
```

Every version remains queryable.

This enables:

- historical retrieval
- compliance auditing
- temporal reasoning
- document rollback

---

## Asynchronous Ingestion using Java 21 Virtual Threads

Large PDF parsing and embedding generation are expensive operations.

Instead of blocking incoming HTTP requests, the ingestion pipeline executes inside Java Virtual Threads (Project Loom).

Benefits:

- Massive concurrency
- Lightweight threads
- Better scalability
- Tomcat thread pool never blocks
- Faster response times

The API immediately returns:

```
HTTP 202 Accepted
```

while processing continues in the background.

---

## Canonical Document Model (CDM)

Every uploaded document is represented using a normalized metadata model.

Example:

```text
Document
├── Title
├── Domain
├── Type
├── Version
├── Status
├── MD5 Checksum
├── Metadata (JSONB)
└── Vector References
```

The retrieval layer is completely decoupled from business-specific schemas.

---

## Flexible Metadata using PostgreSQL JSONB

Enterprise documents often contain different metadata.

Examples:

HR

```json
{
  "department": "HR",
  "country": "India"
}
```

Government

```json
{
  "scheme": "PMAY",
  "state": "Rajasthan"
}
```

Healthcare

```json
{
  "hospital": "AIIMS",
  "category": "Emergency"
}
```

Instead of modifying SQL schemas for every new domain, metadata is stored inside PostgreSQL JSONB columns using Hibernate 6.

This allows the system to support multiple industries without schema migrations.

---

# System Architecture

```
                  PDF Upload
                      │
                      ▼
              REST Controller
                      │
                      ▼
        Java 21 Virtual Thread
                      │
                      ▼
          PDF Parsing (PDFBox)
                      │
                      ▼
          Chunk Generation
                      │
                      ▼
         MD5 Change Detection
          ┌───────────┴───────────┐
          │                       │
    Unchanged                Changed
          │                       │
          ▼                       ▼
   Skip Processing        Generate Embeddings
                                  │
                                  ▼
                      Vector Store (In-Memory /
                      Pinecone / Milvus)
                                  │
                                  ▼
                     PostgreSQL Metadata Registry
```

---

# Technology Stack

| Layer | Technology |
|---------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.x |
| Concurrency | Java Virtual Threads (Project Loom) |
| AI Framework | LangChain4j |
| ORM | Spring Data JPA + Hibernate 6 |
| Database | PostgreSQL |
| Metadata Storage | JSONB |
| Embeddings | All-MiniLM-L6-v2 |
| Vector Store | In-Memory (Pinecone/Milvus Ready) |
| PDF Parsing | Apache PDFBox |
| Build Tool | Maven |
| Containerization | Docker |

---

# Project Structure

```
src
├── controller
├── service
├── repository
├── entity
├── dto
├── configuration
├── ingestion
├── parser
├── embedding
├── vectorstore
└── utils
```

---

# Getting Started

## 1. Clone the Repository

```bash
git clone https://github.com/Divyansh745Garg/adaptive-knowledge.git

cd adaptive-knowledge
```

---

## 2. Start PostgreSQL

```bash
docker compose up -d
```

Verify:

```bash
docker ps
```

---

## 3. Configure Environment

Edit:

```
application.yml
```

Example:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/adaptive_knowledge
    username: YOUR_USERNAME
    password: YOUR_PASSWORD

  jpa:
    hibernate:
      ddl-auto: update

  threads:
    virtual:
      enabled: true
```

---

## 4. Start the Application

```bash
./mvnw spring-boot:run
```

or

```bash
mvn spring-boot:run
```

The application starts on

```
http://localhost:8080
```

---

# REST API

## Upload Document

```
POST /api/v1/ingestion/upload
```

Example:

```bash
curl -X POST http://localhost:8080/api/v1/ingestion/upload \
  -F "file=@policy.pdf" \
  -F "title=Company Security Policy" \
  -F "domain=Security" \
  -F "type=Policy"
```

Response

```json
{
  "trackingId": "4dcbd83a-9b0b-4b73-89ea",
  "status": "ACCEPTED"
}
```

---

# Mutation Test

## Step 1

Upload

```
policy.pdf
```

Result

```
Version 1

Status: ACTIVE
```

---

## Step 2

Upload the exact same file again.

Console output:

```
Document checksum matched.

Skipping ingestion.

Embedding Cost: $0
```

No new vectors are generated.

---

## Step 3

Modify one sentence inside the PDF.

Upload again.

The engine detects:

```
Checksum changed
```

Actions performed:

- Previous Version marked SUPERSEDED
- Old vectors removed
- New vectors generated
- New Version registered
- Metadata updated

---

# PostgreSQL Audit Trail

| Title | Version | Status | Checksum |
|--------|----------|---------|----------|
| Leave Policy | 1 | SUPERSEDED | 9e107d9d372bb682... |
| Leave Policy | 2 | ACTIVE | 4b227777d4dd1fc6... |

---

# Future Enhancements

- Pinecone Integration
- Milvus Support
- Weaviate Support
- Qdrant Support
- Semantic Chunking
- Hybrid Search (BM25 + Vector Search)
- Document Diff Viewer
- Version Comparison API
- Scheduled Background Synchronization
- Kafka-based Ingestion Pipeline
- Multi-Tenant Knowledge Bases
- OAuth2 / JWT Authentication
- OpenTelemetry Tracing
- Kubernetes Deployment

---

# Why This Project?

This project goes beyond a traditional RAG chatbot.

It demonstrates production-oriented engineering concepts including:

- Incremental document indexing
- Version-aware knowledge management
- Asynchronous ingestion pipelines
- Java 21 Virtual Threads
- Flexible JSONB-based metadata storage
- Canonical document modeling
- Cost-aware embedding generation
- Enterprise-ready document lifecycle management

The architecture is designed to scale from small internal knowledge bases to large enterprise documentation systems while minimizing compute costs and preserving complete historical context.

---
