# Intelligent Accident Management Platform

An enterprise-grade **Modular Monolith** backend built with Java 21 and Spring Boot 4, designed to automate and augment First Notice of Loss (FNOL) processes for insurance companies through multimodal AI.

---

## 1. Project Overview
The platform enables road accident victims to submit high-fidelity reports including text descriptions, environmental GPS data, and visual/audio evidence. These reports are analyzed in real-time by AI model to perform expert accident reconstruction, severity classification, and entity extraction before being verified by human agents.

## 2. Architecture
The system follows a **Modular Monolith** design pattern. It strictly enforces module boundaries through:
*   **Separation of Concerns:** Each module owns its unique MongoDB collections and domain logic.
*   **Interface-based Communication:** Modules interact exclusively via "Ports" (Internal Interfaces).
*   **Zero-Trust Security:** Identity is derived cryptographically from tokens and propagated through an internal `IdentityContext`.
*   **Shared-Nothing Persistence:** Cross-module database queries are strictly prohibited to allow for future microservices decomposition if needed.

## 3. Detailed Module Breakdown

| Module | Name | Responsibility |
| :--- | :--- | :--- |
| **M1** | **Accident Submission** | Orchestrates report intake and maintains the central `AccidentReport` aggregate. |
| **M2** | **Context Enrichment** | Integrates with Open-Meteo and OpenStreetMap to provide weather and GIS metadata. |
| **M3** | **Intelligence Orchestration** | Communicates with **Google Gemini 2.0/Flash** for multimodal accident reconstruction. |
| **M4** | **Case Workflow & Review** | Manages human-in-the-loop tasking, agent locking (Leasing), and terminal verification. |
| **M5** | **Media & Asset Mgmt** | Handles binary streaming via **MongoDB GridFS**, PII ownership, and orphan cleanup. |
| **M6** | **User & Identity Authority** | Serves as the JWT Provider and Permission Oracle for the entire system. |
| **M7** | **Policy & Eligibility** | Validates insurance coverage at the moment of impact and handles insurer notifications. |

---

## 4. Tech Stack
*   **Language:** Java 21 (Records, Sealed Interfaces, Pattern Matching)
*   **Framework:** Spring Boot 4.0.3
*   **Persistence:** MongoDB (Primary store & GridFS for binaries)
*   **Security:** Stateless JWT, RS256 signing, RBAC/Permission-based authority
*   **AI:** Google GenAI Java SDK (Gemini Multimodal API)
*   **Observability:** Integrated audit logging per security event
*   **External APIs:** Open-Meteo (Archive), OpenStreetMap (Overpass API)

---

## 5. The AI Multimodal Pipeline
The **Intelligence Module** executes a sophisticated inference chain:
1.  **Context Consolidation:** Merges user text with GIS metadata (road type, daylight, weather).
2.  **Binary Extraction:** Pulls raw bytes from Module 5 (Media) via the internal Port.
3.  **Expert Prompting:** Constructs an expert system instruction commanding the LLM to return strict, raw JSON.
4.  **Multimodal Reasoning:** Gemini analyzes the physical damage in images against the textual claim.
5.  **Schema Enforcement:** Validates the AI response through the `AiSchemaEnforcer` to map "Natural Language" back into typed Java Records.

---

## 6. Security Implementation
The platform employs a **Zero-Trust Integration** strategy:
*   **Derivative Identity:** IDs (`userId`, `agentId`) are never accepted as raw headers. They are extracted from a validated `IdentityContext` provided by the security interceptor.
*   **Permission Registry:** Every service call (`lock`, `verify`, `upload`) is guarded by a capability check (e.g., `CASE_REVIEW_LOCK`).
*   **PII Hardening:** Media assets are burned with an `ownerId`. Streaming binaries is denied unless the requester is the owner or an authorized agent with `ASSET_VIEW` permissions.
*   **Immediate Eviction:** Tokens are checked against a MongoDB-based Revocation Store (Blacklist) during every session validation.

---

## 7. Key Technical Features
*   **Async Handshake:** The pipeline (`Submission -> AI -> Review Queue`) is fully asynchronous, providing a 202-Accepted response to users within milliseconds.
*   **Optimistic Locking:** Cross-module updates use version-controlled records to prevent race conditions during human audit.
*   **Memory Efficiency:** File transfers use `InputStreamResource` for end-to-end binary streaming from GridFS to the Client, preventing JVM OutOfMemory (OOM) errors.
*   **Auto-Orphan Cleanup:** A background scheduler purges unlinked media every 24 hours.

---

## 8. Setup & Installation

### Prerequisites
*   JDK 21
*   MongoDB Instance
*   Google Google Cloud API Key (with Gemini enabled)

### Environment Variables
```bash
export MONGO_URI="mongodb://localhost:27017/accident-platform"
export GOOGLE_API_KEY="your-gemini-api-key"
export JWT_SECRET="your-high-entropy-secret"
```

### Build & Run
```bash
./mvnw clean install
./mvnw spring-boot:run
```

---

## 9. API Reference (Partial)

### Identity & Dashboard
*   `POST /api/v1/auth/login`: Acquire JWT.
*   `GET /api/v1/auth/me`: Identity profile discovery.
*   `GET /api/v1/auth/dashboard`: Dynamic status summary based on User/Agent role.

### Customer Operations
*   `POST /api/v1/policies/declare`: Register coverage.
*   `POST /api/v1/media`: Upload images (Multipart).
*   `POST /api/v1/accidents`: Submit accident (Triggers AI).

### Agent Operations
*   `GET /api/v1/review/queue`: View pending cases.
*   `POST /api/v1/review/{id}/lock`: Start human audit.
*   `GET /api/v1/review/{id}`: View Consolidated Case File.
*   `PATCH /api/v1/review/{id}/verify`: finalize decision.

