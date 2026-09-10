# JSON Dataset Operators

A Spring Boot 3.5 / Java 17 REST service that stores arbitrary JSON records in named datasets
(backed by a relational database) and exposes **group-by** and **sort-by** query operators over them.

Features:

- Insert any JSON object into a dataset; datasets are created implicitly on first insert.
- Query a dataset with `groupBy`, `sortBy` (+ `order`), or both combined (sorted inside each group).
- Nested fields via dot-paths (`address.city`), mixed-type aware comparisons, stable sorting.
- Relational storage: embedded H2 by default, PostgreSQL through a Spring profile. Identical behaviour on both.
- RFC 7807 `application/problem+json` error responses for every failure mode.
- OpenAPI 3 documentation with Swagger UI; Postman collection and `.http` file included.
- Layered design (controller / service / strategy-based query operators / JPA) with unit, slice and integration tests.

## Tech stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 17 |
| Framework | Spring Boot 3.5.16 (Web MVC, Validation) |
| Persistence | Spring Data JPA / Hibernate |
| Database | H2 in-memory (default), PostgreSQL 16 (`postgres` profile) |
| API docs | springdoc-openapi 2.8 (Swagger UI) |
| Testing | JUnit 5, Mockito, AssertJ, Spring Boot Test (MockMvc) |
| Build | Maven (wrapper included) |

## Prerequisites

- **JDK 17+** (the only hard requirement). Verify with `java -version`.
- Maven is not required: use the bundled wrapper `./mvnw` (Linux/macOS) or `mvnw.cmd` (Windows, see [Running on Windows](#running-on-windows)).
  A locally installed Maven 3.9+ also works; substitute `mvn` for `./mvnw` in the commands below.
- Docker (optional) to run PostgreSQL via `docker-compose.yml`.

## Running the application

### Default profile (embedded H2, zero setup)

```bash
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080`. Data lives in memory and is discarded on shutdown.

### As a packaged jar

```bash
./mvnw clean package
java -jar target/json-dataset-operators-1.0.0.jar
```

### PostgreSQL profile

```bash
docker compose up -d                                        # starts postgres:16-alpine on :5432
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
# or: java -jar target/json-dataset-operators-1.0.0.jar --spring.profiles.active=postgres
```

Connection settings are read from environment variables with defaults matching the compose file:

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/datasetops` |
| `DB_USERNAME` | `datasetops` |
| `DB_PASSWORD` | `datasetops` |

Schema is created/updated automatically by Hibernate (`ddl-auto: update`). Stop the database with `docker compose down` (add `-v` to drop the volume).

### Running the tests

```bash
./mvnw test
```

### Running on Windows

Everything above works on Windows; only the wrapper name, environment-variable syntax and shell
quoting differ. Commands below are for **PowerShell**; `cmd.exe` equivalents are noted where they differ.

1. **Install JDK 17** (any distribution), e.g. with winget:

   ```powershell
   winget install EclipseAdoptium.Temurin.17.JDK
   ```

   Open a new terminal, then verify and, if needed, point `JAVA_HOME` at the installation:

   ```powershell
   java -version
   $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.12.7-hotspot"   # adjust to your path
   # cmd.exe: set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.12.7-hotspot
   ```

   To make `JAVA_HOME` permanent, use *System Properties > Environment Variables* or
   `setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.12.7-hotspot"` (takes effect in new terminals).

2. **Run with the bundled wrapper** — use `mvnw.cmd` instead of `./mvnw` (no Maven install needed;
   the wrapper downloads Maven 3.9.9 on first use):

   ```powershell
   .\mvnw.cmd spring-boot:run                  # default H2 profile, http://localhost:8080
   .\mvnw.cmd test                             # run the test suite
   .\mvnw.cmd clean package                    # build the jar
   java -jar target\json-dataset-operators-1.0.0.jar
   ```

   In `cmd.exe` drop the leading `.\`: `mvnw.cmd spring-boot:run`.

3. **PostgreSQL profile** (requires Docker Desktop):

   ```powershell
   docker compose up -d
   $env:DB_URL = "jdbc:postgresql://localhost:5432/datasetops"    # optional, these are the defaults
   $env:DB_USERNAME = "datasetops"
   $env:DB_PASSWORD = "datasetops"
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
   # or: java -jar target\json-dataset-operators-1.0.0.jar --spring.profiles.active=postgres
   ```

   `cmd.exe`: `set DB_URL=jdbc:postgresql://localhost:5432/datasetops` etc. Note the quotes around
   `-D...` arguments in PowerShell; it otherwise splits them on the dot.

4. **Calling the API from PowerShell.** `curl` is an alias for `Invoke-WebRequest` there, so either
   call `curl.exe` explicitly or use `Invoke-RestMethod`. JSON must be single-quoted so the inner
   double quotes survive:

   ```powershell
   curl.exe -X POST http://localhost:8080/api/dataset/employee_dataset/record `
     -H "Content-Type: application/json" `
     -d '{"id":1,"name":"John Doe","age":30,"department":"Engineering"}'

   curl.exe "http://localhost:8080/api/dataset/employee_dataset/query?sortBy=age&order=asc"

   # Native alternative
   Invoke-RestMethod -Method Post -ContentType "application/json" `
     -Uri http://localhost:8080/api/dataset/employee_dataset/record `
     -Body '{"id":2,"name":"Jane Smith","age":25,"department":"Engineering"}'
   ```

   In `cmd.exe` the JSON needs escaped double quotes:
   `curl -X POST ... -d "{\"id\":3,\"name\":\"Alice Brown\",\"age\":28,\"department\":\"Marketing\"}"`.
   The Postman collection in `docs/postman/` avoids all quoting issues and is the recommended way to test.

5. **Troubleshooting**
   - *"'java' is not recognized"*: JDK not on `PATH`; open a new terminal after installing, or set `JAVA_HOME` as above.
   - *Port 8080 already in use*: find the owner with `netstat -ano | findstr :8080`, or start on another
     port with `java -jar target\json-dataset-operators-1.0.0.jar --server.port=8081`.
   - *Script execution blocked*: `mvnw.cmd` is a batch file and is not affected by the PowerShell
     execution policy. If your policy blocks anything, run `Set-ExecutionPolicy -Scope Process RemoteSigned`.
   - *Long path / permission errors during the first wrapper download*: the wrapper caches Maven under
     `%USERPROFILE%\.m2\wrapper`; make sure that folder is writable.

### Useful URLs

| Resource | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| H2 console (default profile only) | http://localhost:8080/h2-console — JDBC URL `jdbc:h2:mem:datasetops`, user `sa`, empty password |

## API reference

Base path: `/api/dataset`. All request and response bodies are JSON. Errors use `application/problem+json`.

`{datasetName}` must match `^[A-Za-z0-9_-]{1,100}$` (letters, digits, `_`, `-`; 1–100 characters).

### 1. Insert record — `POST /api/dataset/{datasetName}/record`

| Parameter | In | Type | Required | Description |
|---|---|---|---|---|
| `datasetName` | path | string | yes | Dataset to insert into; created implicitly if it does not exist |

Request body: any non-empty JSON **object** containing a non-negative integral `id` field. The `id` is used as the record identifier within the dataset. All other fields are free-form and are stored verbatim.

```json
{ "id": 1, "name": "John Doe", "age": 30, "department": "Engineering" }
```

Success: `201 Created`, header `Location: /api/dataset/employee_dataset/record/1`, body:

```json
{
  "message": "Record added successfully",
  "dataset": "employee_dataset",
  "recordId": 1
}
```

| Status | Title | When |
|---|---|---|
| 400 | Invalid record | Body is not a JSON object, is `{}`, or lacks a non-negative integral `id` |
| 400 | Bad Request | Malformed JSON body |
| 400 | Invalid request parameter | `datasetName` fails the pattern |
| 409 | Duplicate record | A record with the same `id` already exists in this dataset |
| 415 | Unsupported Media Type | `Content-Type` is not `application/json` |

### 2. Query dataset — `GET /api/dataset/{datasetName}/query`

| Parameter | In | Type | Required | Description |
|---|---|---|---|---|
| `datasetName` | path | string | yes | Dataset to query |
| `groupBy` | query | string | one of `groupBy`/`sortBy` | Field (dot-path allowed) to group records by |
| `sortBy` | query | string | one of `groupBy`/`sortBy` | Field (dot-path allowed) to sort records by |
| `order` | query | `asc` \| `desc` | no | Sort direction, case-insensitive, default `asc`; only valid with `sortBy` |

Response shape depends on the parameters:

`groupBy=department` → `200 OK`

```json
{
  "groupedRecords": {
    "Engineering": [
      { "id": 1, "name": "John Doe", "age": 30, "department": "Engineering" },
      { "id": 2, "name": "Jane Smith", "age": 25, "department": "Engineering" }
    ],
    "Marketing": [
      { "id": 3, "name": "Alice Brown", "age": 28, "department": "Marketing" }
    ]
  }
}
```

`sortBy=age&order=asc` → `200 OK`

```json
{
  "sortedRecords": [
    { "id": 2, "name": "Jane Smith", "age": 25, "department": "Engineering" },
    { "id": 3, "name": "Alice Brown", "age": 28, "department": "Marketing" },
    { "id": 1, "name": "John Doe", "age": 30, "department": "Engineering" }
  ]
}
```

`groupBy=department&sortBy=age` → `200 OK` with `groupedRecords`, each group's list sorted by `age` ascending.

| Status | Title | When |
|---|---|---|
| 400 | Invalid query | Neither `groupBy` nor `sortBy` given; `order` without `sortBy`; `order` not `asc`/`desc` |
| 400 | Invalid request parameter | `datasetName` fails the pattern |
| 404 | Dataset not found | The dataset has no records |

### 3. Get record — `GET /api/dataset/{datasetName}/record/{recordId}`

Convenience endpoint; it is the target of the `Location` header returned by the insert API.

| Parameter | In | Type | Required | Description |
|---|---|---|---|---|
| `datasetName` | path | string | yes | Dataset name |
| `recordId` | path | integer | yes | The `id` supplied in the record payload |

Success: `200 OK` with the stored JSON object exactly as submitted.

| Status | Title | When |
|---|---|---|
| 400 | Invalid request parameter | `datasetName` fails the pattern |
| 400 | Bad Request | `recordId` is not an integer |
| 404 | Record not found | No record with that `id` in the dataset |

### Error format (RFC 7807)

Every error is an `application/problem+json` document with a `timestamp` extension:

```json
{
  "type": "about:blank",
  "title": "Invalid query",
  "status": 400,
  "detail": "At least one of 'groupBy' or 'sortBy' query parameters must be provided",
  "instance": "/api/dataset/employee_dataset/query",
  "timestamp": "2026-09-05T10:00:00Z"
}
```

Unknown routes return a 404 problem detail; unexpected failures return a 500 problem detail with a generic message (details are logged server-side, never exposed).

### curl examples

```bash
BASE=http://localhost:8080/api/dataset/employee_dataset

# Insert the three records from the assignment
curl -s -X POST "$BASE/record" -H 'Content-Type: application/json' \
  -d '{"id": 1, "name": "John Doe", "age": 30, "department": "Engineering"}'
curl -s -X POST "$BASE/record" -H 'Content-Type: application/json' \
  -d '{"id": 2, "name": "Jane Smith", "age": 25, "department": "Engineering"}'
curl -s -X POST "$BASE/record" -H 'Content-Type: application/json' \
  -d '{"id": 3, "name": "Alice Brown", "age": 28, "department": "Marketing"}'

# Group by department
curl -s "$BASE/query?groupBy=department"

# Sort by age ascending / descending
curl -s "$BASE/query?sortBy=age&order=asc"
curl -s "$BASE/query?sortBy=age&order=desc"

# Group by department, sorted by age inside each group
curl -s "$BASE/query?groupBy=department&sortBy=age"

# Fetch a single record
curl -s "$BASE/record/1"

# Error: no query parameters -> 400 application/problem+json
curl -s -i "$BASE/query"
```

The last call returns:

```http
HTTP/1.1 400
Content-Type: application/problem+json

{"type":"about:blank","title":"Invalid query","status":400,
 "detail":"At least one of 'groupBy' or 'sortBy' query parameters must be provided",
 "instance":"/api/dataset/employee_dataset/query","timestamp":"2026-09-05T10:00:00Z"}
```

## Behaviour rules and edge cases

| Situation | Behaviour |
|---|---|
| Payload is not a JSON object (array/scalar) or is empty `{}` | 400 Invalid record |
| Payload has no integral `id`, or `id` is negative / non-numeric / fractional | 400 Invalid record — `Record must contain a non-negative integral 'id' field` |
| `(dataset, id)` already exists | 409 Duplicate record |
| Malformed JSON body / wrong content type | 400 / 415 |
| Dataset name does not match `^[A-Za-z0-9_-]{1,100}$` | 400 |
| Query with neither `groupBy` nor `sortBy` | 400 Invalid query |
| `order` supplied without `sortBy` | 400 Invalid query |
| `order` not `asc` / `desc` (matched case-insensitively) | 400 Invalid query |
| `order` omitted | defaults to `asc` |
| Dataset has no records | 404 Dataset not found |
| Sort field missing (or JSON `null`) in some records | those records are placed last, in their original relative order (stable sort), for both `asc` and `desc` |
| Group field missing (or JSON `null`) in some records | bucketed under the key `"null"` |
| `groupBy` and `sortBy` together | records grouped; each group's list sorted by `sortBy`/`order` |
| Nested field | dot-path, e.g. `address.city`; missing intermediate objects are treated as missing |
| Mixed value types in a sort field | numbers < strings < booleans < objects/arrays; within a type: numeric, lexicographic, `false < true`, `toString()` |
| Group order in `groupedRecords` | first-seen order of the group value across records in insertion order |
| Unknown route | 404 problem detail |
| Unexpected exception | 500 problem detail; message hidden, stack trace logged |

## Design overview

Request flow: `DatasetController` → `DatasetRecordService` / `DatasetQueryService` → `DatasetRecordRepository` (JPA) and the `query` operators. The controller only translates HTTP to typed calls; validation and business rules live in the services and value objects. Dependency direction is strictly `api -> service -> {query, persistence}`; the `query` package depends only on Jackson.

| Package (`com.assignment.datasetops`) | Responsibility | Key types |
|---|---|---|
| `api` | HTTP boundary, OpenAPI config, error mapping | `DatasetController`, `api.dto.InsertRecordResponse`, `api.error.ApiExceptionHandler`, `OpenApiConfig` |
| `service` | Use cases / orchestration, transactions | `DatasetRecordService`, `DatasetQueryService` |
| `query` | Pure, DB-agnostic operators | `QueryOperation`, `SortByOperation`, `GroupByOperation`, `QueryOperationResolver`, `RecordSorter`, `RecordGrouper`, `JsonFieldExtractor`, `JsonValueComparator`, `QueryRequest`, `QueryResult`, `SortOrder` |
| `persistence` | JPA entity, repository, converter | `DatasetRecordEntity`, `DatasetRecordRepository`, `JsonNodeAttributeConverter` |
| `domain.exception` | Business exceptions | `DatasetOpsException` and subclasses |

Design patterns:

- **Strategy + resolver** — each operator implements `QueryOperation` (`supports`/`execute`). `QueryOperationResolver` receives all implementations as a `List<QueryOperation>` and picks the first that supports the request. A new operator (e.g. `filterBy`) is a new class; no `if/else` chain is edited.
- **Composition** — `GroupByOperation` reuses `RecordGrouper` and `RecordSorter` to sort inside each bucket instead of duplicating logic.
- **Value objects** — `QueryRequest` validates its invariants on construction (`QueryRequest.of(...)`); `QueryResult` is a sealed interface (`Grouped` / `Sorted`) so the response shape is mapped exhaustively and serialises directly to `{"groupedRecords": ...}` or `{"sortedRecords": ...}`.
- **JPA `AttributeConverter`** — `JsonNodeAttributeConverter` stores the payload as text while the entity stays typed as `JsonNode`.
- **`@RestControllerAdvice` + `ProblemDetail`** — one handler maps every exception type to a status and RFC 7807 body; controllers contain no try/catch.

Why group/sort runs in the service layer over `JsonNode` rather than in SQL: the assignment requires a relational store, but JSON querying syntax differs per vendor (H2 vs PostgreSQL `jsonb`) and nested-path support is uneven. Loading a dataset's records and applying the operators in Java keeps behaviour identical across databases, supports arbitrary and nested fields with a single code path, and keeps the operators trivially unit-testable. Storage is a single table `dataset_records(id, dataset_name, record_id, payload, created_at)` with a unique constraint on `(dataset_name, record_id)`; datasets are implicit (a dataset exists iff it has at least one record).

Full specification: [docs/DESIGN.md](docs/DESIGN.md).

## Testing

The suite follows a test pyramid:

| Level | Scope | Tooling |
|---|---|---|
| Unit | `query` operators (comparator, extractor, sorter, grouper, operations, resolver, `QueryRequest`, `SortOrder`), services with mocked repository/resolver, converter round-trip | JUnit 5, Mockito, AssertJ |
| Slice | `DatasetController` with `@WebMvcTest` and `@MockitoBean` services: status codes, headers, problem-detail bodies | Spring MockMvc |
| Integration | `DatasetApiIntegrationTest` — `@SpringBootTest` + `@AutoConfigureMockMvc` on H2, replays the three assignment examples end to end | Spring Boot Test |

Run everything with `./mvnw test`; a single class with `./mvnw test -Dtest=RecordSorterTest`.

## Postman

1. In Postman choose **Import** and select `docs/postman/json-dataset-operators.postman_collection.json`.
2. Import `docs/postman/local.postman_environment.json` the same way and select the **local** environment (variables `baseUrl` = `http://localhost:8080`, `datasetName` = `employee_dataset`).
3. Run the requests top-to-bottom (or use the Collection Runner). Each request carries a test asserting the expected status code; the "Error" folder demonstrates the problem-detail responses.

Alternatively, `docs/requests.http` contains the same requests for the JetBrains HTTP Client or the VS Code REST Client extension.

## Project structure

```text
.
├── docker-compose.yml                  PostgreSQL 16 for the "postgres" profile
├── docs/
│   ├── DESIGN.md                       Architecture and behaviour specification
│   ├── requests.http                   REST Client requests
│   └── postman/                        Postman collection + environment
├── pom.xml
└── src/
    ├── main/java/com/assignment/datasetops/
    │   ├── DatasetOpsApplication.java
    │   ├── api/                        controller, dto, error handler, OpenAPI config
    │   ├── service/                    DatasetRecordService, DatasetQueryService
    │   ├── query/                      operators, sorter, grouper, extractor, comparator
    │   ├── persistence/                entity, repository, JsonNode converter
    │   └── domain/exception/           business exceptions
    ├── main/resources/
    │   ├── application.yml             default (H2) configuration
    │   └── application-postgres.yml    PostgreSQL profile
    └── test/java/com/assignment/datasetops/
                                        unit, slice and integration tests
```
