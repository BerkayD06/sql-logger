# Query Logger - Spring Boot SQL Query Logging & Cancellation Library

A powerful Spring Boot starter library that automatically captures, formats, and logs SQL queries with advanced features including query cancellation, performance monitoring, and multi-database support.

## Features

- Automatic SQL Capturing**: Intercepts all SQL queries executed through JDBC and Hibernate ORM
- Query Formatting: Formats raw SQL queries with keyword highlighting and proper indentation for readability
- Origin Tracking: Identifies whether queries originated from JPA, Hibernate, JDBC, or MongoDB
- Slow Query Detection: Logs queries exceeding configurable time thresholds
- Query Cancellation: Automatically cancels long-running queries that exceed specified duration limits
- Flexible Logging: Multiple logging formats (raw SQL, formatted, or with bound parameters)
- Query Filtering: Regex-based exclusion patterns for system or unwanted queries
- Sampling Support: Probabilistic logging using configurable sample rates
- Multi-Database Support: PostgreSQL, MySQL, Oracle, SQL Server, H2, SQLite, MongoDB
- Non-Intrusive: Works transparently with existing Spring Boot applications without code changes
- Zero Configuration: Works out-of-the-box with sensible defaults
- Spring Boot Auto-Configuration: Seamless integration via Spring Boot's auto-configuration mechanism

## Quick Start

### Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.berkayd06</groupId>
    <artifactId>spring-boot-query-logger-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

That's it! The library automatically configures itself when added to your classpath.

### Basic Usage

The library works automatically once added as a dependency. It intercepts all database queries and logs them based on the configured threshold.

**Default behavior:**
- Logs queries taking longer than 200ms
- Only logs slow queries (queries below threshold are not logged)
- Formats SQL with keywords highlighted
- Uses SLF4J for logging

```

When your application runs queries, you'll see logs like:

```
event=sql_query origin=JPA duration_ms=245.123 canceled=false sql="SELECT ... FROM ..." params={1: 'value1', 2: 'value2'}
```

## Configuration

Configure the Query Logger using application properties. All settings are optional and have sensible defaults.

### Basic Configuration

```properties
# Enable/disable the query logger
querylogger.enabled=true

# Threshold for logging queries (milliseconds)
querylogger.log-threshold-ms=200

# Only log queries that exceed the threshold
querylogger.only-slow=true

# Log parameter values bound to prepared statements
querylogger.log-params=true

# Set the SLF4J logger name
querylogger.logger-name=com.berkayd06.querylogger
```

### Advanced Configuration

```properties
# Query Cancellation
querylogger.cancel-enabled=true
querylogger.cancel-threshold-ms=5000

# Logging Format
# Formats: SQL (raw), FORMATTED (pretty-printed), BOUND (with parameter values)
querylogger.log-format=FORMATTED

# Sampling
# Log only a percentage of queries (0.0 to 1.0)
querylogger.sample-rate=1.0

# Exclude queries matching regex patterns
# Useful to skip system queries or internal monitoring queries
querylogger.exclude-sql-regex=^(SELECT|INSERT|UPDATE|DELETE)\\s+(.*)?\\s+(FROM|INTO)\\s+(pg_|information_schema|sqlite_)

# Vendor-specific timeouts
querylogger.vendor.postgresql.statement-timeout-ms=30000
querylogger.vendor.mysql.max-execution-time-ms=30000
querylogger.vendor.oracle.statement-timeout-ms=30000
querylogger.vendor.sqlserver.query-timeout-ms=30
querylogger.vendor.h2.query-timeout-ms=30
querylogger.vendor.sqlite.busy-timeout-ms=30000
querylogger.vendor.mongodb.max-time-ms=30000
querylogger.vendor.mongodb.enabled=true
```

## Configuration Properties Reference

### Core Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `querylogger.enabled` | boolean | true | Enable/disable query logging |
| `querylogger.log-threshold-ms` | long | 200 | Minimum query duration (ms) to log |
| `querylogger.only-slow` | boolean | true | Only log queries exceeding threshold |
| `querylogger.log-params` | boolean | true | Include parameter values in logs |
| `querylogger.logger-name` | string | com.berkayd06.querylogger | SLF4J logger name |

### Query Cancellation

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `querylogger.cancel-enabled` | boolean | false | Enable automatic query cancellation |
| `querylogger.cancel-threshold-ms` | long | 0 | Duration (ms) before cancelling a query |

### Formatting and Filtering

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `querylogger.log-format` | enum | FORMATTED | Format of logged SQL (SQL, FORMATTED, BOUND) |
| `querylogger.exclude-sql-regex` | string | null | Regex pattern for queries to exclude from logging |
| `querylogger.sample-rate` | double | 1.0 | Probability (0.0-1.0) of logging each query |

### Vendor-Specific Timeouts

#### PostgreSQL
```properties
querylogger.vendor.postgresql.statement-timeout-ms=30000
```

#### MySQL
```properties
querylogger.vendor.mysql.max-execution-time-ms=30000
```

#### Oracle
```properties
querylogger.vendor.oracle.statement-timeout-ms=30000
```

#### SQL Server
```properties
querylogger.vendor.sqlserver.query-timeout-ms=30
```

#### H2
```properties
querylogger.vendor.h2.query-timeout-ms=30
```

#### SQLite
```properties
querylogger.vendor.sqlite.busy-timeout-ms=30000
```

#### MongoDB
```properties
querylogger.vendor.mongodb.max-time-ms=30000
querylogger.vendor.mongodb.enabled=true
```

## Usage Examples

### Example 1: Log All Queries

Log every query regardless of execution time:

```properties
querylogger.enabled=true
querylogger.only-slow=false
querylogger.log-threshold-ms=0
```

### Example 2: Monitor Slow Queries

Log only queries taking longer than 1 second with parameters:

```properties
querylogger.enabled=true
querylogger.log-threshold-ms=1000
querylogger.only-slow=true
querylogger.log-params=true
querylogger.log-format=BOUND
```

### Example 3: Cancel Long-Running Queries

Automatically cancel queries exceeding 5 seconds:

```properties
querylogger.enabled=true
querylogger.cancel-enabled=true
querylogger.cancel-threshold-ms=5000
querylogger.log-threshold-ms=1000
```

### Example 4: Exclude System Queries

Skip logging for system catalog queries:

```properties
querylogger.enabled=true
querylogger.exclude-sql-regex=^SELECT.*FROM\s+(pg_|information_schema|sqlite_)
querylogger.log-threshold-ms=200
```

### Example 5: Sampling for High-Volume Applications

Log only 10% of queries in production to reduce log volume:

```properties
querylogger.enabled=true
querylogger.sample-rate=0.1
querylogger.log-threshold-ms=100
```

### Example 6: Raw SQL Output

Log unformatted SQL for search/analysis:

```properties
querylogger.enabled=true
querylogger.log-format=SQL
querylogger.log-threshold-ms=200
```

### Example 7: Production Configuration

Balanced configuration for production environments:

```properties
querylogger.enabled=true
querylogger.log-threshold-ms=500
querylogger.only-slow=true
querylogger.log-params=false
querylogger.log-format=FORMATTED
querylogger.cancel-enabled=true
querylogger.cancel-threshold-ms=30000
querylogger.exclude-sql-regex=^SELECT.*FROM\s+(pg_|information_schema)
querylogger.sample-rate=1.0
```

## How It Works

### Architecture

The Query Logger uses a non-intrusive architecture based on proxying:

1. DataSource Wrapping: The library wraps the Spring `DataSource` bean automatically via `BeanPostProcessor`
2. Connection Interception: When a connection is obtained, it's wrapped to intercept statement creation
3. Statement Proxying: All SQL statements are proxied to capture execution details
4. Query Origin Tracking: Hibernates's `StatementInspector` marks JPA-originated queries
5. Formatting: SQL queries are formatted using regex-based pattern matching
6. Async Cancellation: A daemon thread scheduler handles long-running query cancellation

### Query Flow

```
Application Code
    ↓
DataSource (Wrapped)
    ↓
Connection (Wrapped)
    ↓
Statement (Wrapped)
    ↓
SQL Execution Intercepted
    ↓
Query Origin Identified
    ↓
Query Formatted
    ↓
Performance Metrics Collected
    ↓
Cancellation Scheduled (if enabled)
    ↓
SLF4J Logger
```

### Components

#### QueryLoggingDataSource
The central component that wraps the actual `DataSource`. It:
- Intercepts connection creation
- Wraps connections and statements
- Manages the query cancellation scheduler
- Measures execution time and tracks cancellations

#### HibernateSqlCapturingInspector
Integrates with Hibernate's `StatementInspector` to:
- Mark queries originating from Hibernate/JPA
- Provide origin information for logging

#### SqlFormatter
A high-performance SQL formatter that:
- Uppercases SQL keywords
- Normalizes whitespace
- Adds line breaks for readability
- Binds parameters safely for BOUND format
- Prevents SQL injection in parameter binding

#### QueryOrigin
A ThreadLocal-based utility that tracks:
- Whether queries originated from JPA/Hibernate
- Whether queries originated from MongoDB
- Falls back to "NATIVE" for direct JDBC queries

## Log Output Format

### Standard Log Format

```
event=sql_query origin={ORIGIN} duration_ms={TIME} canceled={CANCELED} sql="{SQL}" params={PARAMS}
```

**Fields:**
- `event`: Always "sql_query" for easy filtering
- `origin`: Query source (JPA, MONGODB, or NATIVE)
- `duration_ms`: Execution time in milliseconds (formatted to 3 decimal places)
- `canceled`: Whether the query was cancelled (true/false)
- `sql`: The formatted SQL query
- `params`: Parameter values if `log-params=true` (JSON-like format: {1: value1, 2: value2})

### Example Logs

**JPA Query:**
```
event=sql_query origin=JPA duration_ms=245.123 canceled=false sql="SELECT p FROM Person p WHERE p.age > ? AND p.email LIKE ?" params={1: 25, 2: '%gmail.com%'}
```

**Slow Query:**
```
event=sql_query origin=NATIVE duration_ms=5432.567 canceled=false sql="UPDATE large_table SET status = 1 WHERE created_at < ?"
```

**Cancelled Query:**
```
event=sql_query origin=JPA duration_ms=30001.234 canceled=true sql="SELECT * FROM very_large_table"
```

**Formatted SQL Example:**
```sql
SELECT
  p.name,
  p.age,
  p.email
FROM
  person p
WHERE
  p.age > ?
  AND p.email LIKE ?
ORDER BY
  p.created_at DESC
```

## Performance Considerations

### Minimal Overhead

- Uses Java reflection proxy pattern for interception
- Lazy formatting (only formats SQL when logging occurs)
- Pre-compiled regex patterns for efficient matching
- Daemon thread pool for async cancellation
- ThreadLocal for origin tracking (no synchronization overhead)

### Optimization Tips

1. **Set `only-slow=true`** to avoid processing fast queries
2. **Increase `log-threshold-ms`** if log volume is too high
3. **Use `sample-rate`** to reduce logging in high-volume scenarios
4. **Disable `log-params`** if parameter logging is not needed
5. **Use `exclude-sql-regex`** to skip system or monitoring queries

## Supported Databases

The library automatically adapts to different database systems:

| Database | Timeout Method | Configuration |
|----------|---|---|
| PostgreSQL | SET statement_timeout | `querylogger.vendor.postgresql.statement-timeout-ms` |
| MySQL | MAX_EXECUTION_TIME() hint | `querylogger.vendor.mysql.max-execution-time-ms` |
| Oracle | DBMS_SESSION.SET_CONTEXT | `querylogger.vendor.oracle.statement-timeout-ms` |
| SQL Server | SET QUERY_GOVERNOR_COST_LIMIT | `querylogger.vendor.sqlserver.query-timeout-ms` |
| H2 | SET QUERY_TIMEOUT | `querylogger.vendor.h2.query-timeout-ms` |
| SQLite | PRAGMA busy_timeout | `querylogger.vendor.sqlite.busy-timeout-ms` |
| MongoDB | maxTimeMS | `querylogger.vendor.mongodb.max-time-ms` |

## Troubleshooting

### Query Logger Not Logging

**Problem**: No logs appear even with `querylogger.enabled=true`

**Solutions:**
1. Check your SLF4J logger is configured: `com.berkayd06.querylogger` at INFO level
2. Verify `log-threshold-ms` is not set too high
3. Check if `only-slow=true` and queries are executing too fast
4. Verify `sample-rate` is not set to 0.0
5. Check if queries match `exclude-sql-regex` pattern

### High Memory Usage

**Problem**: Application memory usage increases significantly

**Solutions:**
1. Increase `log-threshold-ms` to log fewer queries
2. Decrease `sample-rate` to log fewer queries probabilistically
3. Disable `log-params` if parameter logging is not needed
4. Exclude unnecessary queries with `exclude-sql-regex`

### Queries Not Being Cancelled

**Problem**: Long-running queries continue despite `cancel-enabled=true`

**Solutions:**
1. Ensure `cancel-enabled=true` is set
2. Verify `cancel-threshold-ms` is reasonable (e.g., 5000 for 5 seconds)
3. Some databases don't support statement cancellation
4. Check database logs for timeout exceptions
5. Verify the database user has permission to cancel queries

### DataSource Wrapping Issues

**Problem**: "QueryLoggingDataSource already wrapping" error

**Solutions:**
1. Ensure only one `DataSource` bean is defined
2. If using multiple databases, define them with different bean names
3. Check for multiple Spring contexts

## Dependency Requirements

- **Java 8+**: Minimum Java version
- **Spring Boot 2.1.3+**: Auto-configuration support
- **Spring Framework 5.1+**: Required by Spring Boot
- **SLF4J 1.7+**: Logging API (any implementation)
- **Hibernate 5.4+** (optional): For Hibernate/JPA query interception
- **JDBC Driver**: For your target database

## Integration Examples

### Spring Data JPA Application

```java
@SpringBootApplication
@EnableJpaRepositories
public class JpaApplication {
    public static void main(String[] args) {
        SpringApplication.run(JpaApplication.class, args);
    }
}

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByEmail(String email);
    List<User> findByAgeGreaterThan(int age);
}

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public List<User> getAdultUsers() {
        return userRepository.findByAgeGreaterThan(18);
    }
}
```

**Configuration (application.properties):**
```properties
querylogger.enabled=true
querylogger.log-threshold-ms=200
querylogger.log-params=true
querylogger.log-format=FORMATTED
```

### Monitoring & Observability

The library integrates with standard SLF4J logging. Use your preferred log aggregation tool:

**Logback Configuration (logback-spring.xml):**
```xml
<logger name="com.berkayd06.querylogger" level="INFO">
    <appender-ref ref="ASYNC_FILE"/>
</logger>

<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>512</queueSize>
    <appender-ref ref="FILE"/>
</appender>

<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/queries.log</file>
    <encoder>
        <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/queries.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
</appender>
```

## Best Practices

1. **Development**: Set `log-threshold-ms=0` and `only-slow=false` to see all queries
2. **Testing**: Use similar configuration as production for accurate performance metrics
3. **Staging**: Enable `cancel-enabled=true` and test query cancellation
4. **Production**: 
   - Use reasonable `log-threshold-ms` (e.g., 500-1000ms)
   - Enable `cancel-enabled=true` with appropriate threshold
   - Use `exclude-sql-regex` to skip system queries
   - Consider `sample-rate` for high-volume applications
   - Disable `log-params` if sensitive data is in parameters

5. **Performance Testing**:
   - Use `sample-rate=0.01` to get statistical data with low overhead
   - Monitor `log-threshold-ms` impact on logging volume
   - Profile query execution in isolated test environment

## Limitations

- **Read-Only Logging**: Query Logger is designed for logging and monitoring, not for query modification
- **Statement Caching**: Some prepared statement caches may bypass interception
- **Async Drivers**: May not work with async JDBC drivers
- **Reactive Drivers**: No support for Spring WebFlux R2DBC drivers
- **Custom DataSource Proxies**: May conflict with other DataSource wrappers

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Author

**Berkay Demirbas** - [GitHub](https://github.com/berkayd06)

## Changelog

### Version 1.0.1
- Removed comment lines from codebase
- Improved code readability
- Bug fixes and minor optimizations

### Version 1.0.0
- Initial release
- SQL query logging and formatting
- Query cancellation support
- Hibernate/JPA integration
- Multi-database support
- Configurable thresholds and filtering

## Support

For issues, questions, or feature requests, please visit the [GitHub Issues](https://github.com/berkayd06/query-logger/issues) page.

## Related Projects
