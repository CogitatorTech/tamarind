## Feature Roadmap

This document includes the roadmap for the Tamarind project.
It outlines features to be implemented and their current status.

> [!IMPORTANT]
> This roadmap is a work in progress and is subject to change.

---

## 1. Query Engine Support

* **DuckDB Engine**
    * [x] Basic query execution via JDBC
    * [x] Connection pooling with HikariCP
    * [x] View creation from file paths
    * [x] httpfs extension for S3 support
    * [x] Parquet, CSV, and JSON file support
    * [x] In-memory and persistent database modes
    * [x] Connection pool initialization and management
    * [x] Thread-safe transaction management (RequestScoped)
    * [x] Analytical query engine interface
    * [x] Fixed URL parameter handling (PRAGMA-based configuration)
    * [x] Connection pool health checks with query validation
    * [ ] DuckDB extensions management API
    * [ ] Custom aggregate functions registration
* **Other Engines**
    * [x] Pluggable query engine interface
    * [x] Analytical engine abstraction
    * [ ] ClickHouse engine implementation
    * [ ] Apache Druid engine implementation
    * [ ] Apache Pinot engine implementation
    * [ ] Engine hot-swapping without restart
* **Query Optimization**
    * [x] Query validation and sanitization
    * [x] Basic query rewriting and optimization
    * [x] SQL injection prevention (input validation)
    * [x] Query result normalization and caching
    * [ ] Query plan caching
    * [ ] Automatic query rewriting
    * [ ] Predicate pushdown for cloud storage
    * [ ] Adaptive query execution

---

## 2. REST API

* **Query Endpoints**
    * [x] Basic POST/query endpoint
    * [x] Batch query execution
    * [x] Versioned API (v1)
    * [x] Query timeout configuration
    * [x] Structured error responses with details
    * [x] Query result pagination
    * [ ] Streaming query results
    * [ ] Query cancellation endpoint
    * [x] Query history and audit trail
    * [ ] Saved queries management
* **Response Formats**
    * [x] JSON array output
    * [x] Structured API responses with metadata
    * [x] Error messages with details and line numbers
    * [x] Execution time and row count metadata
    * [x] CSV streaming response with RFC 4180 escaping
    * [ ] Apache Arrow IPC format
    * [ ] Parquet file downloads
* **API Features**
    * [x] Pagination support
    * [x] Result truncation with warnings
    * [x] Cache hit and miss metadata
    * [ ] GraphQL endpoint
    * [ ] WebSocket support for live queries
    * [ ] OpenAPI (Swagger) documentation

---

## 3. Apache Arrow Flight

* **Flight SQL Protocol**
    * [x] Basic Flight SQL server
    * [x] GetTables command
    * [x] GetSchemas command
    * [x] ExecuteQuery command
    * [x] GetFlightInfo implementation
    * [ ] PreparedStatement support
    * [ ] Transactions support
    * [ ] Bulk ingestion via DoAction
* **Performance**
    * [x] Zero-copy data transfer
    * [x] Columnar data format
    * [ ] Compression support (LZ4 and ZSTD)
    * [ ] Connection pooling for Flight clients
    * [ ] Dictionary encoding for strings
* **Security**
    * [ ] TLS/SSL encryption
    * [ ] Token-based authentication
    * [ ] mTLS for client certificates

---

## 4. Data Source Management

* **Cloud Storage**
    * [x] AWS S3 integration
    * [x] Automatic file discovery
    * [x] View creation from S3 paths
    * [ ] Azure Blob Storage support
    * [ ] Google Cloud Storage support
    * [ ] MinIO support
    * [ ] Multi-cloud federation
* **Local Storage**
    * [x] Local file system scanning
    * [x] Parquet file support
    * [x] CSV file support
    * [x] JSON file support
    * [ ] Delta Lake support
    * [ ] Apache Iceberg support
    * [ ] Apache Hudi support
* **Data Catalogs**
    * [x] Basic metadata caching
    * [ ] AWS Glue catalog integration
    * [ ] Hive Metastore integration
    * [ ] Unity Catalog support
    * [ ] Custom catalog plugins

---

## 5. Caching and Performance

* **Query Result Caching**
    * [x] Caffeine-based in-memory cache
    * [x] Configurable TTL and max size
    * [x] Cache key generation from queries
    * [x] Cache hit tracking in metadata
    * [ ] Distributed cache with Redis
    * [ ] Cache warming strategies
    * [ ] Smart cache invalidation
* **Metadata Caching**
    * [x] Schema cache
    * [x] File list cache
    * [x] Table metadata cache
    * [ ] Statistics caching
    * [ ] Partition information cache
* **Connection Management**
    * [x] HikariCP connection pooling
    * [x] Configurable pool sizes
    * [x] Connection pool health monitoring
    * [ ] Connection health checks
    * [ ] Automatic connection recovery

---

## 6. Storage Layer

* **Persistent Storage**
    * [x] Pluggable UserRepository interface
    * [x] In-memory implementation (default: ConcurrentHashMap)
    * [x] JPA-based implementation (H2, PostgreSQL, and MySQL support)
    * [x] Database migrations with Flyway
    * [x] User management (CRUD operations)
    * [x] Zero-configuration defaults
    * [ ] ScheduledQuery entity persistence
    * [ ] AuditLog entity persistence
    * [ ] Automatic schema versioning
* **Ephemeral Storage (for Caching, etc.)**
    * [x] Pluggable EphemeralRepository interface
    * [x] In-memory implementation (default: ConcurrentHashMap with TTL)
    * [x] Pattern matching (glob syntax)
    * [x] Atomic increment/decrement operations
    * [x] TTL support with automatic expiration
    * [ ] Redis implementation for distributed caching
    * [ ] Memcached implementation
    * [ ] Hazelcast implementation
* **Configuration**
    * [x] Configurable backend selection
    * [x] Environment variable overrides
    * [x] Multiple database support (SQLite, H2, and PostgreSQL)
    * [x] Migration from in-memory to database
    * [ ] Hot-swapping storage backends

---

## 7. Security and Authentication

* **Authentication**
    * [x] JWT token support (Quarkus SmallRye JWT)
    * [x] Custom token-based authentication
    * [x] User registration and login API
    * [x] Token validation and management
    * [x] Pluggable storage for user accounts
    * [x] Token storage in ephemeral repository
    * [ ] OAuth2/OIDC integration
    * [ ] LDAP/Active Directory support
    * [ ] API key authentication
    * [ ] SAML support
* **Authorization**
    * [x] Basic security configuration
    * [x] User-based access control
    * [ ] Role-based access control (RBAC) support
    * [ ] Row-level security
    * [ ] Column-level security
    * [ ] Data masking policies
* **Audit and Compliance**
    * [x] Audit logging for queries
    * [x] Authentication event logging
    * [x] Rate limit violation tracking
    * [x] Security violation logging
    * [x] Error logging with user context
    * [ ] Persistent audit log storage (database)
    * [ ] Query lineage tracking
    * [ ] PII detection and logging
    * [ ] Compliance reports (GDPR and HIPAA)

---

## 8. Rate Limiting and Throttling

* **Request Throttling**
    * [x] Per-user rate limiting (Bucket4j)
    * [x] Configurable rate limits
    * [x] Burst capacity support
    * [x] Rate limit violation tracking
    * [ ] Per-endpoint rate limits
    * [ ] Tenant-based quotas
    * [ ] Cost-based throttling
* **Resource Management**
    * [x] Query timeout configuration
    * [ ] Query memory limits
    * [ ] CPU usage throttling
    * [ ] Concurrent query limits
    * [ ] Priority queue for queries

---

## 9. Monitoring and Observability

* **Metrics**
    * [x] Prometheus metrics export
    * [x] JVM metrics
    * [x] Query execution metrics
    * [x] Cache hit rate metrics
    * [ ] Custom business metrics
* **Health Checks**
    * [x] Liveness checks
    * [x] Readiness checks
    * [x] Database connection health
    * [x] Connection pool health
    * [ ] S3 connectivity checks
    * [ ] Dependency health checks
* **Logging**
    * [x] Structured logging with Log4j2
    * [x] JSON audit logs
    * [x] Query execution logging
    * [ ] Distributed tracing (OpenTelemetry)
    * [ ] Log aggregation support
    * [ ] Query execution plans logging

---

## 10. Command-Line Interface

* **Interactive Mode**
    * [x] REPL for SQL queries
    * [x] Picocli-based CLI
    * [x] Table output formatting
    * [ ] Auto-completion
    * [ ] Command history
    * [ ] Multi-line query editing
* **Batch Mode**
    * [x] Execute queries from command line
    * [ ] Execute SQL files
    * [ ] CSV import and export
    * [ ] Script execution mode
* **Configuration**
    * [ ] Connection profiles
    * [ ] Output format selection
    * [ ] Query result limits

---

## 11. Scheduled Queries

* **Scheduling**
    * [x] Cron-based scheduling
    * [x] Query registration API
    * [x] Multiple scheduled queries
    * [x] Execution tracking and statistics
    * [ ] Time-based triggers
    * [ ] Event-based triggers
    * [ ] Dependency management
* **Results Handling**
    * [ ] Save results to S3
    * [ ] Save results to database
    * [ ] Email notifications
    * [ ] Webhook callbacks
* **Management**
    * [x] List scheduled queries
    * [x] Unregister queries
    * [x] Execution count tracking
    * [x] Error tracking
    * [ ] Pause and resume schedules
    * [ ] Execution history
    * [ ] Retry policies

---

## 12. Web User Interface

* **Notebook Interface**
    * [x] Jupyter-style notebook interface
    * [x] Multiple cells for SQL queries
    * [x] Interactive query execution
    * [x] Result table display with pagination
    * [x] Error display with proper formatting
    * [x] SQL syntax highlighting (Prism.js)
    * [x] Keyboard shortcuts (Shift+Enter to run)
* **Notebook Management**
    * [x] Create new notebooks
    * [x] Save notebooks to browser storage
    * [x] Load saved notebooks
    * [x] Rename notebooks
    * [x] Delete notebooks
    * [x] Clear notebooks (reset cell counter)
    * [x] Duplicate notebooks
    * [x] Export notebooks as JSON
    * [x] Import notebooks from JSON
    * [x] List all notebooks with metadata
    * [x] Auto-save functionality
* **Result Display**
    * [x] Paginated table view (10/25/50/100/500/All rows)
    * [x] Column headers with sticky positioning
    * [x] NULL value styling
    * [x] Cell expansion on double-click
    * [x] Row hover effects
    * [x] Execution time display
    * [x] Row count information
* **Data Export**
    * [x] Copy results as table (tab-separated)
    * [x] Copy results as CSV
    * [x] Export to CSV file
    * [x] Export to JSON file
    * [x] Export to SQL INSERT statements
    * [x] Toast notifications for actions
* **UI Features**
    * [x] User authentication (login/logout)
    * [x] User avatar and profile display
    * [x] Sidebar with database tables
    * [x] Quick query templates
    * [x] Responsive design
    * [x] Modern gradient UI
    * [x] Loading indicators
    * [x] Error handling with user-friendly messages
    * [x] User management modal
    * [ ] Dark mode support
    * [ ] Query history panel
    * [ ] Schema browser with column details
    * [ ] SQL autocomplete
    * [ ] Query visualization
    * [ ] Collaborative editing

---

## 13. Data Quality and Validation

* **Schema Validation**
    * [ ] Enforce schema on read
    * [ ] Schema evolution tracking
    * [ ] Type checking and coercion
* **Data Quality Checks**
    * [ ] Null value detection
    * [ ] Duplicate detection
    * [ ] Range validation
    * [ ] Custom validation rules
* **Data Profiling**
    * [ ] Column statistics
    * [ ] Data distribution analysis
    * [ ] Anomaly detection

---

## 14. Multi-Tenancy

* **Tenant Isolation**
    * [ ] Tenant identification
    * [ ] Separate schemas per tenant
    * [ ] Data isolation
    * [ ] Resource quotas per tenant
* **Configuration**
    * [ ] Tenant-specific settings
    * [ ] Custom catalogs per tenant
    * [ ] Per-tenant caching

---

## 15. Developer Experience

* **Testing**
    * [x] Unit tests with JUnit 5
    * [x] Integration tests
    * [x] Mockito for mocking
    * [x] JMH benchmarks
    * [x] Test coverage reporting
    * [ ] Performance regression tests
    * [ ] Load testing suite
* **Code Quality**
    * [x] Google Java Format
    * [x] Spotless Maven plugin
    * [x] Checkstyle validation
    * [x] JaCoCo code coverage
    * [x] Qodana code analysis
    * [ ] SonarQube integration
* **Documentation**
    * [x] README with examples
    * [x] Architecture documentation
    * [x] API design documentation
    * [x] Getting started guide
    * [x] Web UI guide
    * [x] Feature status tracking (ROADMAP)
    * [x] Bug analysis and fixes documentation
    * [x] Storage configuration guide
    * [x] OLTP implementation guide
    * [x] Complete achievement summary
    * [ ] Interactive tutorials
    * [ ] Video guides
    * [ ] API reference documentation

---

## 16. Deployment and Operations

* **Containerization**
    * [x] Dockerfile for containerization
    * [ ] Docker Compose setup
    * [ ] Kubernetes manifests
    * [ ] Helm charts
* **Cloud Deployment**
    * [ ] AWS deployment guide
    * [ ] Azure deployment guide
    * [ ] GCP deployment guide
    * [ ] Terraform modules
* **High Availability**
    * [x] Stateless design
    * [ ] Load balancing support
    * [ ] Failover mechanisms
    * [ ] Backup and restore
* **Build and Operations**
    * [x] Makefile for common operations
    * [x] Development mode support
    * [x] Production build pipeline
    * [x] Release packaging

---

## 17. Client Libraries and Integrations

* **Official Clients**
    * [x] Python client example (Arrow Flight)
    * [ ] JavaScript/TypeScript client
    * [ ] Go client library
    * [ ] Rust client library
* **BI Tool Integration**
    * [ ] Tableau connector
    * [ ] Power BI connector
    * [ ] Apache Superset integration
    * [ ] Metabase integration
* **Data Integration**
    * [ ] Apache Airflow provider
    * [ ] dbt adapter
    * [ ] Kafka Connect integration
    * [ ] Spark connector

---

## 18. Advanced Analytics

* **Machine Learning**
    * [ ] Model serving endpoints
    * [ ] Feature store integration
    * [ ] Model versioning
* **Time Series**
    * [ ] Time series functions
    * [ ] Window functions optimization
    * [ ] Downsampling support
* **Geospatial**
    * [ ] PostGIS-compatible functions
    * [ ] Geometry types support
    * [ ] Spatial indexing

---

## 19. Performance Optimizations

* **Query Execution**
    * [x] Async query execution support
    * [x] Query result caching
    * [x] Connection pooling
    * [ ] Parallel query execution
    * [ ] Vectorized execution
    * [ ] JIT compilation
    * [ ] Cost-based optimization
* **Storage**
    * [ ] Local file caching
    * [ ] Predictive prefetching
    * [ ] Tiered storage support
* **Network**
    * [x] HTTP/1.1 optimization
    * [ ] HTTP/2 support
    * [ ] Connection multiplexing
    * [ ] Response compression

---

## 20. Extensibility

* **Plugin System**
    * [ ] Custom function plugins
    * [ ] Custom data source plugins
    * [ ] Custom authentication plugins
    * [ ] Plugin marketplace
* **Scripting**
    * [ ] User-defined functions (UDF)
    * [ ] JavaScript/Python UDFs
    * [ ] Custom aggregates
