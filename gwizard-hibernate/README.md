# GWizard Hibernate

When an application binds Micrometer's `MeterRegistry`, Hibernate integration automatically
records transaction attempts, retries, serialization failures, and retry exhaustion.
Hikari pools also expose acquisition/usage timers, timeouts, and capacity gauges unless
they already have metrics configured. See [the metrics reference](../gwizard-metrics/README.md#http-and-database-metrics).
A registry binding is optional; application-supplied pools retain ownership of their lifecycle.

Love it or hate it, you probably have had to use it. `HibernateModule` integrates
[Guice Persist](https://github.com/google/guice/wiki/GuicePersist) with Hibernate for JPA access to a relational database.
It supplies database configuration, a default HikariCP connection pool, and automatic transaction retries
while retaining Guice Persist's request scope.

## Maven

Install the gwizard bom, then:

```xml
    <dependency>
        <groupId>org.gwizard</groupId>
        <artifactId>gwizard-hibernate</artifactId>
    </dependency>
```

## Usage

[A self-contained example](src/test/java/org/gwizard/hibernate/example/HibernateModuleExample.java)

Hibernate does not fit easily into a pasted code snippet. Look at the self-contained example above, or
better yet the [full GWizard example](https://github.com/gwizard/gwizard-example).

What you need to know:

* Database configuration is drawn from a bound `DatabaseConfig` object. Supply JDBC connection settings,
an application-owned datasource, or explicit Hibernate connection configuration. Pool and retry settings
have defaults. You can configure additional Hibernate properties here.

* You must create a `persistence.xml` file and package it in your jar's `META-INF` directory. We hate this, but JPA
absolutely requires it. Fortunately, it need not contain anything other than this boilerplate:

```xml
<persistence version="2.0" xmlns="http://java.sun.com/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd">
    <persistence-unit name="gw-persistence-unit" transaction-type="RESOURCE_LOCAL">
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
    </persistence-unit>
</persistence>
```

* If for some reason you want to change the persistence name, it is an optional parameter to `HibernateModule`'s constructor.

* Read the documentation for [Guice Persist](https://github.com/google/guice/wiki/GuicePersist)! The most important
bits are understanding `@Transactional`, and if you choose to install it, `PersistFilter`. Speaking of which, you can
install that like any other filter if you are using the `WebModule` or `RestModule`.

## Connection pooling

With ordinary JDBC URL/credential configuration, GWizard uses HikariCP. Configure the default pool
through `DatabaseConfig.getPool()` or the `pool` section of `DatabaseConfigProperties`:

```yaml
database:
  url: jdbc:postgresql://localhost/app
  user: app
  password: your-password
  pool:
    maxPoolSize: 20
    connectionTimeoutMillis: 1000
```

These are the pool defaults. The JDBC driver can normally be discovered from the URL; specify
`driverClass` if needed. `maxPoolSize` must be at least 1 and `connectionTimeoutMillis` at least 250.
The timeout bounds waiting for an available connection; it is not a query or transaction timeout.
Exhaustion raises a JDBC exception after the wait. Validation timeout is derived from the acquisition
timeout; the other Hikari settings retain their defaults, including a fixed-size pool.

Pool selection happens after Hibernate merges GWizard configuration with `persistence.xml`:

* An explicit `HibernateModule.withDataSource(...)` binding takes precedence and is used directly.
* Explicit Hibernate connection providers, datasource/JNDI settings, and pool-specific configuration
  are honored. Invalid provider selections fail startup rather than silently falling back to Hikari.
* Custom provider auto-registration and multi-tenant connection management remain available.
* Otherwise, the default Hikari pool uses the settings above. The existing
  `hibernate.connection.pool_size` property overrides `pool.maxPoolSize`.

For direct Hikari configuration, use `hibernate.hikari.*` properties (for example,
`hibernate.hikari.maximumPoolSize` and `hibernate.hikari.connectionTimeout`). This is an explicit
Hibernate pool configuration: the `pool` defaults above no longer apply. Likewise, an explicit
`hibernate.connection.provider_class` retains that provider's configuration and lifecycle.

### Supplying your own pool

Select the Guice binding for an application-owned `javax.sql.DataSource`:

```java
install(new HibernateModule().withDataSource(DataSource.class));
bind(DataSource.class).toProvider(ApplicationPoolProvider.class).in(Singleton.class);
```

For a qualified binding, use a key:

```java
install(new HibernateModule().withDataSource(
    Key.get(DataSource.class, Names.named("mainDatabase"))));
```

`withDataSource()` returns a new module; use that returned module when installing it. The selected
binding is resolved once during Hibernate bootstrap, so the datasource must be usable then. Bind
`DatabaseConfig` as usual for retry and Hibernate settings, but JDBC URL, driver, user, and password
are optional; `new DatabaseConfigProperties()` is sufficient when supplying a datasource. With this
hook, any JDBC connection fields are ignored, including credentials: the datasource owns them.
GWizard does not wrap the datasource in another pool or change its pool size/acquisition timeout.

### Pool ownership and shutdown

Hibernate owns the default pool and closes it when the persistence unit closes. `PersistFilter`
stops persistence when the web context is destroyed, after GWizard's request drain. In an application
without `PersistFilter`, call `PersistService.stop()` (or close the `EntityManagerFactory`) during
shutdown, as before.

An application-supplied datasource remains application-owned: GWizard/Hibernate releases its reference
without closing the pool, including when Hibernate bootstrap fails. The application must close it
after persistence has stopped. JNDI datasources likewise retain their existing external ownership.
An explicitly configured Hibernate provider manages its resources according to that provider's contract.

## Transaction retries

Every Guice Persist `@Transactional` method or class automatically retries transactions that fail with
SQLSTATE `40001` (serialization failure), including errors raised during flush or commit. Each retry
rolls back the failed transaction, closes its entity manager, and reruns the complete method in a fresh
transaction and entity manager. Nested transactional methods join the outer transaction and share its
retry budget. Transactions started manually remain the caller's responsibility.

Configure retries through `DatabaseConfig.getTransactionRetry()`. `DatabaseConfigProperties` supports
the following YAML section alongside the connection settings; these are the defaults:

```yaml
database:
  transactionRetry:
    maxAttempts: 5
    maxBackoffMillis: 250
```

`maxAttempts` includes the first execution; `1` disables retries. `maxBackoffMillis` caps the delay
between attempts, not the total transaction duration. Both settings must be at least `1` and are
validated when the injector is created.

Backoff always uses full jitter: each delay is chosen uniformly between zero and the current ceiling.
The ceiling starts at one-sixteenth of `maxBackoffMillis`, doubles with each retry, and stops growing
at the cap. Fractional milliseconds are rounded down, with a minimum ceiling of 1 ms. With the default
cap, retry ceilings are 15, 31, 62, 125, and then 250 ms. The schedule is independent of `maxAttempts`.
No transaction or entity manager is held during backoff. Interrupting the thread stops retries and
preserves its interrupt status. Exhaustion propagates the final transaction failure. Cleanup failures
also stop retries and are attached to that failure as suppressed exceptions.

`PersistFilter` and static `EM.em()` access continue to work. Within a request, retries replace the
current entity manager, and the successful attempt's entity manager stays open until the filter ends
the request. Use `EM.em()` or an injected `Provider<EntityManager>` to resolve the current instance.
As with any retried transaction, load entities inside the transactional method and keep external
side effects outside the work that may execute again.

Existing `@Transactional(rollbackOn = ..., ignore = ...)` behavior is preserved for application
exceptions. Serialization failures always roll back regardless of those annotations. Other SQLSTATEs
are not retried.

## `EM` and scope

You are free to @Inject the `EntityManager` all over your code if you want to, but I personally think this sucks.
It's especially painful when you want to access the database from within entity POJO classes[1] or other places
where injection is not convenient. I find that aspects like transaction state and identity are best modeled as thread
locals, and so you get a convenient little helper method:

```java
EM.em().persist(new Thing(...));
```
Better yet, with a static import:
```java
em().persist(new Thing(...));
```

gwizard-hibernate provides this static method which always returns the correct `EntityManager`. Of course you
are free to ignore this and @Inject your EntityManagers like a heathen.

[1]: If you're screaming into your monitor "don't put data access code in your entity classes!", then you
have never used polymorphic entity objects in any sophisticated capacity. You're just wrong about this. Sorry.
