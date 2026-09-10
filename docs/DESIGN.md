# Design — JSON Dataset Group-By / Sort-By Operators

## 1. Goals

* Spring Boot 3.5 / Java 21 / Maven service.
* Two public APIs (plus one small convenience GET so that `Location` headers resolve):
  * `POST /api/dataset/{datasetName}/record` — persist a JSON object into a dataset.
  * `GET  /api/dataset/{datasetName}/query?groupBy=..&sortBy=..&order=..` — group / sort records.
  * `GET  /api/dataset/{datasetName}/record/{recordId}` — fetch a single record (target of `Location`).
* Relational storage (H2 by default, PostgreSQL via profile). JSON is stored as text in a column
  and materialised as Jackson `JsonNode` in the domain.
* Robust error handling with RFC 7807 `application/problem+json` bodies.
* TDD-friendly: every collaborator is a small, single-purpose Spring bean with unit tests.

## 2. Package layout (`com.assignment.datasetops`)

| Package | Responsibility | Key types |
|---|---|---|
| `api` | HTTP boundary | `DatasetController`, `api.dto.*`, `api.error.ApiExceptionHandler`, `api.OpenApiConfig` |
| `service` | Use-cases / orchestration | `DatasetRecordService` (insert + get), `DatasetQueryService` (query) |
| `query` | Pure, DB-agnostic operators (Strategy pattern) | `QueryOperation`, `SortByOperation`, `GroupByOperation`, `QueryOperationResolver`, `RecordSorter`, `RecordGrouper`, `JsonFieldExtractor`, `JsonValueComparator`, `QueryRequest`, `QueryResult`, `SortOrder` |
| `persistence` | JPA | `DatasetRecordEntity`, `DatasetRecordRepository`, `JsonNodeAttributeConverter` |
| `domain.exception` | Business exceptions | `DatasetOpsException` + 5 subclasses |

Dependency direction: `api -> service -> {query, persistence}`. `query` depends only on Jackson.

## 3. Design patterns used

* **Strategy** — `QueryOperation` implementations (`SortByOperation`, `GroupByOperation`) are discovered
  as a `List<QueryOperation>` and selected by `QueryOperationResolver` (a *resolver/factory*).
  Adding a new operator (e.g. `filterBy`) is a new class, no `if/else` chain edits.
* **Composition over inheritance** — `GroupByOperation` reuses `RecordGrouper` and, when `sortBy` is also
  present, `RecordSorter` to sort inside each bucket.
* **Value objects** — `QueryRequest` (validated on construction), `QueryResult` (sealed interface with
  `Grouped` / `Sorted` records) so the controller maps results exhaustively.
* **Attribute Converter** — `JsonNodeAttributeConverter` keeps JPA entity typed as `JsonNode`.
* **Template of exceptions → ProblemDetail** — one `@RestControllerAdvice` maps each exception type to a
  status; no try/catch in controllers.

## 4. Contracts (already written — do not change signatures)

```java
// query
public enum SortOrder { ASC, DESC; static SortOrder fromParameter(String raw); boolean isDescending(); }
public record QueryRequest(String groupBy, String sortBy, SortOrder order) {
    static QueryRequest of(String groupBy, String sortBy, String order); // validates
    boolean hasGroupBy(); boolean hasSortBy(); Optional<String> groupByField(); Optional<String> sortByField();
}
public sealed interface QueryResult permits Grouped, Sorted {
    record Grouped(Map<String, List<JsonNode>> groupedRecords) implements QueryResult {}
    record Sorted(List<JsonNode> sortedRecords) implements QueryResult {}
}
public interface QueryOperation {
    boolean supports(QueryRequest request);
    QueryResult execute(List<JsonNode> records, QueryRequest request);
}
// api.dto
public record InsertRecordResponse(String message, String dataset, long recordId) { static success(dataset, id) }
```

### 4.1 Contracts to implement (signatures are binding so parallel work integrates)

```java
// ---- query package ----
@Component class JsonFieldExtractor {
    /** Resolve a dot-path ("a.b.c") in the node. Returns Optional.empty() for missing or JSON null. */
    Optional<JsonNode> extract(JsonNode record, String fieldPath);
}
class JsonValueComparator implements Comparator<JsonNode> {   // plain class, stateless, also a @Component
    // Ordering rules:
    //  * numbers compared numerically (decimalValue()), text lexicographically (String.compareTo),
    //    booleans false < true, containers/other by toString().
    //  * mixed types ordered by rank: NUMBER < TEXT < BOOLEAN < OTHER.
    //  * this comparator never receives null — RecordSorter handles missing values (see below).
}
@Component class RecordSorter {
    /** Stable sort by field. Records whose field is missing/null are placed LAST regardless of order. Returns new list. */
    List<JsonNode> sort(List<JsonNode> records, String fieldPath, SortOrder order);
}
@Component class RecordGrouper {
    /** Group by field. Key = value.asText() for value nodes, toString() for containers.
        Records with missing/null field go into key RecordGrouper.MISSING_GROUP_KEY = "null".
        Returns LinkedHashMap preserving first-seen order of groups. */
    Map<String, List<JsonNode>> group(List<JsonNode> records, String fieldPath);
}
@Component class SortByOperation implements QueryOperation   // supports: hasSortBy && !hasGroupBy -> QueryResult.Sorted
@Component class GroupByOperation implements QueryOperation  // supports: hasGroupBy (sortBy optional -> sort inside each group) -> QueryResult.Grouped
@Component class QueryOperationResolver {
    QueryOperationResolver(List<QueryOperation> operations);
    /** First operation that supports the request; throws InvalidQueryException if none. */
    QueryOperation resolve(QueryRequest request);
}

// ---- persistence package ----
@Entity @Table(name = "dataset_records",
       uniqueConstraints = @UniqueConstraint(name = "uk_dataset_record", columnNames = {"dataset_name", "record_id"}),
       indexes = @Index(name = "idx_dataset_name", columnList = "dataset_name"))
class DatasetRecordEntity {
    @Id @GeneratedValue(strategy = IDENTITY) Long id;
    @Column(name = "dataset_name", nullable = false, length = 100) String datasetName;
    @Column(name = "record_id", nullable = false) Long recordId;
    @Convert(converter = JsonNodeAttributeConverter.class) @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "payload", nullable = false) JsonNode payload;
    @Column(name = "created_at", nullable = false, updatable = false) Instant createdAt; // set in @PrePersist
    // protected no-arg ctor for JPA + public ctor (datasetName, recordId, payload) + getters
}
@Converter class JsonNodeAttributeConverter implements AttributeConverter<JsonNode, String>  // uses a shared ObjectMapper; throws IllegalStateException on bad stored JSON
interface DatasetRecordRepository extends JpaRepository<DatasetRecordEntity, Long> {
    List<DatasetRecordEntity> findByDatasetNameOrderByIdAsc(String datasetName);
    boolean existsByDatasetName(String datasetName);
    boolean existsByDatasetNameAndRecordId(String datasetName, Long recordId);
    Optional<DatasetRecordEntity> findByDatasetNameAndRecordId(String datasetName, Long recordId);
}

// ---- service package ----
@Service class DatasetRecordService {
    DatasetRecordService(DatasetRecordRepository repository);
    /** Validates payload (must be a JSON object, non-empty, with integral numeric "id" >= 0 that fits in long),
        rejects duplicates (DuplicateRecordException), persists, returns the recordId. @Transactional. */
    long insert(String datasetName, JsonNode payload);
    /** @throws RecordNotFoundException */
    JsonNode get(String datasetName, long recordId);          // @Transactional(readOnly = true)
}
@Service class DatasetQueryService {
    DatasetQueryService(DatasetRecordRepository repository, QueryOperationResolver resolver);
    /** Loads all records of the dataset (404 DatasetNotFoundException if none), resolves the operation, executes it. */
    QueryResult query(String datasetName, QueryRequest request);  // @Transactional(readOnly = true)
}

// ---- api package ----
@RestController @RequestMapping("/api/dataset") @Validated class DatasetController {
    // datasetName path variable: @Pattern(regexp = "^[A-Za-z0-9_-]{1,100}$") — else 400
    POST /{datasetName}/record   body: JsonNode  -> 201 Created, Location: /api/dataset/{name}/record/{recordId}, body InsertRecordResponse
    GET  /{datasetName}/record/{recordId} -> 200 JsonNode
    GET  /{datasetName}/query?groupBy&sortBy&order -> 200 QueryResult (Jackson serialises the record -> {"groupedRecords":{..}} or {"sortedRecords":[..]})
}
@RestControllerAdvice class ApiExceptionHandler extends ResponseEntityExceptionHandler  // returns ProblemDetail
```

## 5. Behavioural rules (documented in README)

| Situation | Behaviour |
|---|---|
| Payload not a JSON object (array/scalar) or empty `{}` | 400 `InvalidRecordException` |
| Payload has no integral `id` (or negative / non-numeric) | 400 `InvalidRecordException` — message: `Record must contain a non-negative integral 'id' field` |
| `(dataset, id)` already exists | 409 `DuplicateRecordException` |
| Malformed JSON body / wrong content type | 400 / 415 via `ResponseEntityExceptionHandler` |
| Dataset name fails regex | 400 (ConstraintViolationException) |
| Query with neither `groupBy` nor `sortBy` | 400 `InvalidQueryException` |
| `order` present without `sortBy` | 400 |
| `order` not `asc`/`desc` (case-insensitive) | 400 |
| Dataset has no records | 404 `DatasetNotFoundException` |
| Record field missing in some records — sort | those records last (stable) |
| Record field missing in some records — group | bucket key `"null"` |
| `groupBy` + `sortBy` together | groups, each group's list sorted |
| Nested field | dot-path, e.g. `address.city` |
| Unknown route | 404 problem detail |
| Unexpected exception | 500 problem detail, message hidden, logged |

Error body (RFC 7807), e.g.
```json
{ "type": "about:blank", "title": "Dataset not found", "status": 404,
  "detail": "Dataset 'x' not found", "instance": "/api/dataset/x/query", "timestamp": "2026-09-05T10:00:00Z" }
```

## 6. Persistence model

Single table `dataset_records(id PK, dataset_name, record_id, payload TEXT, created_at)`, unique
`(dataset_name, record_id)`. Datasets are implicit: a dataset exists iff it has at least one record.
Group/sort is executed in the service layer over `JsonNode` so that behaviour is identical on H2 and
PostgreSQL and any field (including nested ones) is supported without per-vendor JSON SQL.

## 7. Testing strategy

* Unit (`src/test/java/.../query`): comparator, extractor, sorter, grouper, both operations, resolver, `QueryRequest`, `SortOrder`.
* Unit (`service`): Mockito for repository/resolver, covers all validation branches.
* Unit (`persistence`): converter round-trip.
* Slice (`api`): `@WebMvcTest(DatasetController.class)` with `@MockitoBean` services — status codes & problem details.
* Integration (`DatasetApiIntegrationTest`): `@SpringBootTest` + `@AutoConfigureMockMvc` on H2, replays the
  three examples from the assignment PDF end to end.
