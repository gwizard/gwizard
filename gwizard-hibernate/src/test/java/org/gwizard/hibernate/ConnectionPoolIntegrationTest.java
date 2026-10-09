package org.gwizard.hibernate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.persist.PersistService;
import com.zaxxer.hikari.HikariDataSource;
import io.dropwizard.util.Duration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.Validation;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.POOL_SIZE;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Timeout(15)
class ConnectionPoolIntegrationTest {

	private Injector injector;
	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

	@AfterEach
	void stop() {
		if (injector != null)
			injector.getInstance(PersistService.class).stop();
		PoolJndiFactory.dataSource = null;
		registry.close();
	}

	private static DatabaseConfigProperties jdbcConfig() {
		// Driver discovery and an omitted password must work too.
		final DatabaseConfigProperties config = new DatabaseConfigProperties();
		config.setUrl("jdbc:h2:mem:" + UUID.randomUUID());
		config.setUser("sa");
		return config;
	}

	private ConnectionProvider open(final DatabaseConfig config, final HibernateModule module) {
		injector = Guice.createInjector(module, binder -> binder.bind(DatabaseConfig.class).toInstance(config));
		return provider();
	}

	private ConnectionProvider provider() {
		return injector.getInstance(EntityManagerFactory.class).unwrap(SessionFactoryImplementor.class)
				.getServiceRegistry().requireService(ConnectionProvider.class);
	}

	private ConnectionProvider openWithMetrics(final DatabaseConfig config, final HibernateModule module) {
		injector = Guice.createInjector(module, binder -> {
			binder.bind(DatabaseConfig.class).toInstance(config);
			binder.bind(MeterRegistry.class).toInstance(registry);
		});
		return provider();
	}

	@Test
	void defaultPoolMetricsMeasureAcquisitionContentionTimeoutsAndUsageAndAreRemovedOnClose() throws Exception {
		final DatabaseConfigProperties config = jdbcConfig();
		config.getPool().setMaxPoolSize(1);
		config.getPool().setConnectionTimeout(Duration.milliseconds(500));
		final ConnectionProvider provider = openWithMetrics(config, new HibernateModule());
		final HikariDataSource pool = provider.unwrap(HikariDataSource.class);
		assertThat(pool.getMetricRegistry()).isSameAs(registry);
		final var borrower = Executors.newSingleThreadExecutor();
		try (var held = provider.getConnection()) {
			final var waiting = borrower.submit(() -> {
				try (var connection = provider.getConnection()) {
					return (SQLException)null;
				} catch (SQLException failure) {
					return failure;
				}
			});
			await(() -> pool.getHikariPoolMXBean().getThreadsAwaitingConnection() == 1);
			assertThat(registry.get("hikaricp.connections.pending").gauge().value()).isEqualTo(1);
			assertThat(registry.get("hikaricp.connections.active").gauge().value()).isEqualTo(1);
			assertThat(registry.get("hikaricp.connections.max").gauge().value()).isEqualTo(1);
			assertThat((Throwable)waiting.get(5, TimeUnit.SECONDS)).isInstanceOf(SQLTransientConnectionException.class);
			assertThat(registry.get("hikaricp.connections.timeout").counter().count()).isEqualTo(1);
			assertThat(registry.get("hikaricp.connections.acquire").timer().count()).isEqualTo(2);
			assertThat(registry.get("hikaricp.connections.acquire").timer().totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(250);
		} finally {
			borrower.shutdownNow();
		}
		query(provider);
		assertThat(registry.get("hikaricp.connections.usage").timer().count()).isEqualTo(2);
		injector.getInstance(PersistService.class).stop();
		assertThat(registry.getMeters()).noneMatch(meter -> meter.getId().getName().startsWith("hikaricp."));
		assertThat(registry.isClosed()).isFalse();
	}

	@Test
	void explicitlyConfiguredHibernateHikariPoolGetsMetricsToo() throws Exception {
		final ConnectionProvider provider = openWithMetrics(new DatabaseConfigProperties(), new HibernateModule("xml-hikari"));
		assertThat(provider).isExactlyInstanceOf(HikariCPConnectionProvider.class);
		query(provider);
		assertThat(registry.get("hikaricp.connections.acquire").timer().count()).isEqualTo(1);
		assertThat(provider.unwrap(HikariDataSource.class).getMaximumPoolSize()).isEqualTo(3);
	}

	@Test
	void suppliedHikariPoolGetsMetricsAndKeepsItsLifecycle() throws Exception {
		try (var pool = applicationPool()) {
			injector = Guice.createInjector(new HibernateModule().withDataSource(DataSource.class), binder -> {
				binder.bind(DataSource.class).toInstance(pool);
				binder.bind(DatabaseConfig.class).toInstance(new DatabaseConfigProperties());
				binder.bind(MeterRegistry.class).toInstance(registry);
			});
			query(provider());
			assertThat(pool.getMetricRegistry()).isSameAs(registry);
			assertThat(registry.get("hikaricp.connections.acquire").timer().count()).isEqualTo(1);
			injector.getInstance(PersistService.class).stop();
			assertThat(pool.isClosed()).isFalse();
			assertThat(registry.find("hikaricp.connections.active").gauge()).isNotNull();
		}
		assertThat(registry.find("hikaricp.connections.active").gauge()).isNull();
	}

	@Test
	void suppliedPoolsExistingMetricsConfigurationIsPreserved() throws Exception {
		final var otherRegistry = new SimpleMeterRegistry();
		try (var pool = applicationPool()) {
			pool.setMetricRegistry(otherRegistry);
			injector = Guice.createInjector(new HibernateModule().withDataSource(DataSource.class), binder -> {
				binder.bind(DataSource.class).toInstance(pool);
				binder.bind(DatabaseConfig.class).toInstance(new DatabaseConfigProperties());
				binder.bind(MeterRegistry.class).toInstance(registry);
			});
			query(provider());
			assertThat(pool.getMetricRegistry()).isSameAs(otherRegistry);
			assertThat(otherRegistry.get("hikaricp.connections.acquire").timer().count()).isPositive();
			assertThat(registry.getMeters()).noneMatch(meter -> meter.getId().getName().startsWith("hikaricp."));
		} finally {
			otherRegistry.close();
		}
	}

	private static void await(final BooleanSupplier ready) throws Exception {
		final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!ready.getAsBoolean() && System.nanoTime() < deadline)
			Thread.sleep(5);
		assertThat(ready.getAsBoolean()).isTrue();
	}

	private static void query(final ConnectionProvider provider) throws SQLException {
		final var connection = provider.getConnection();
		try (var statement = connection.createStatement(); var result = statement.executeQuery("select 1")) {
			assertThat(result.next()).isTrue();
			assertThat(result.getInt(1)).isEqualTo(1);
		} finally {
			provider.closeConnection(connection);
		}
	}

	@Test
	void defaultPoolWorksAndClosesWithPersistenceService() throws Exception {
		final ConnectionProvider provider = open(jdbcConfig(), new HibernateModule());
		final HikariDataSource pool = provider.unwrap(HikariDataSource.class);
		assertThat(pool.getMaximumPoolSize()).isEqualTo(20);
		assertThat(pool.getConnectionTimeout()).isEqualTo(1_000);
		query(provider);
		injector.getInstance(PersistService.class).stop();
		assertThat(pool.isClosed()).isTrue();
	}

	@Test
	void yamlPoolSettingsReachHikariAndExhaustionHasABoundedWait() throws Exception {
		final DatabaseConfigProperties config = new ObjectMapper(new YAMLFactory()).readValue("""
				pool:
				  maxPoolSize: 1
				  connectionTimeout: 250 ms
				""", DatabaseConfigProperties.class);
		config.setUrl(jdbcConfig().getUrl());
		final ConnectionProvider provider = open(config, new HibernateModule());
		try (var held = provider.getConnection()) {
			final long started = System.nanoTime();
			assertThatThrownBy(provider::getConnection).isInstanceOf(SQLTransientConnectionException.class);
			assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isBetween(150L, 5_000L);
		}
		query(provider); // Returning a connection makes the pool usable again.
	}

	@Test
	void legacyHibernatePoolSizeStillOverridesTheDefault() {
		final DatabaseConfigProperties config = jdbcConfig();
		config.getProperties().put(POOL_SIZE, "3");
		final ConnectionProvider provider = open(config, new HibernateModule());
		assertThat(provider.unwrap(HikariDataSource.class).getMaximumPoolSize()).isEqualTo(3);
	}

	@Test
	void explicitProviderKeepsItsOwnLifecycle() throws Exception {
		final DatabaseConfigProperties config = jdbcConfig();
		config.getProperties().put(CONNECTION_PROVIDER, TrackingConnectionProvider.class.getName());
		final ConnectionProvider provider = open(config, new HibernateModule());
		assertThat(provider).isExactlyInstanceOf(TrackingConnectionProvider.class);
		query(provider);
		injector.getInstance(PersistService.class).stop();
		assertThat(((TrackingConnectionProvider)provider).stopped).isTrue();
	}

	@Test
	void persistenceXmlProviderAndCredentialsArePreserved() throws Exception {
		final ConnectionProvider provider = open(new DatabaseConfigProperties(), new HibernateModule("xml-provider"));
		assertThat(provider).isExactlyInstanceOf(TrackingConnectionProvider.class);
		final var connection = provider.getConnection();
		try {
			assertThat(connection.getMetaData().getUserName()).isEqualTo("XMLUSER");
		} finally {
			provider.closeConnection(connection);
		}
		query(provider);
	}

	@Test
	void persistenceXmlHikariPropertiesAreNotReplacedByDefaultPoolSettings() {
		final DatabaseConfigProperties config = new DatabaseConfigProperties();
		config.getPool().setMaxPoolSize(9);
		config.getPool().setConnectionTimeout(Duration.seconds(9));
		final ConnectionProvider provider = open(config, new HibernateModule("xml-hikari"));
		assertThat(provider).isExactlyInstanceOf(HikariCPConnectionProvider.class);
		final HikariDataSource pool = provider.unwrap(HikariDataSource.class);
		assertThat(pool.getMaximumPoolSize()).isEqualTo(3);
		assertThat(pool.getConnectionTimeout()).isEqualTo(750);
	}

	@Test
	void namedApplicationPoolWinsAndRemainsOpenAfterHibernateStops() throws Exception {
		try (var pool = applicationPool()) {
			final DatabaseConfigProperties config = jdbcConfig();
			config.setDriverClass("not.a.Driver");
			config.setUrl("jdbc:invalid:unused");
			config.setUser("wrong-user");
			config.setPassword("wrong-password");
			config.getProperties().put(CONNECTION_PROVIDER, TrackingConnectionProvider.class.getName());
			for (String setting : List.of("hibernate.connection.datasource", "jakarta.persistence.jtaDataSource",
					"javax.persistence.jtaDataSource", "javax.persistence.nonJtaDataSource", "jakarta.persistence.nonJtaDataSource"))
				config.getProperties().put(setting, "java:comp/env/jdbc/unused");
			final Key<DataSource> key = Key.get(DataSource.class, Names.named("application-db"));
			injector = Guice.createInjector(new HibernateModule().withDataSource(key), binder -> {
				binder.bind(DatabaseConfig.class).toInstance(config);
				binder.bind(key).toInstance(pool);
			});
			assertThat(provider().unwrap(DataSource.class)).isSameAs(pool);
			assertThat(injector.getInstance(EntityManagerFactory.class).getProperties())
					.containsEntry("jakarta.persistence.nonJtaDataSource", pool)
					.doesNotContainKeys("jakarta.persistence.jtaDataSource", "javax.persistence.jtaDataSource");
			assertThat(pool.getMaximumPoolSize()).isEqualTo(2);
			assertThat(pool.getConnectionTimeout()).isEqualTo(7_000);
			query(provider());
			injector.getInstance(PersistService.class).stop();
			assertThat(pool.isClosed()).isFalse();
			try (var connection = pool.getConnection()) {
				assertThat(connection.isValid(1)).isTrue();
			}
		}
	}

	@Test
	void suppliedPoolNeedsNoJdbcConfigAndSurvivesFailedBootstrap() {
		try (var pool = applicationPool(); var validation = Validation.buildDefaultValidatorFactory()) {
			final DatabaseConfigProperties config = new DatabaseConfigProperties();
			assertThat(validation.getValidator().validate(config)).isEmpty();
			config.getProperties().put(HBM2DDL_AUTO, "validate"); // The empty database has no Thing table.
			assertThatThrownBy(() -> Guice.createInjector(new HibernateModule().withDataSource(DataSource.class), binder -> {
				binder.bind(DatabaseConfig.class).toInstance(config);
				binder.bind(DataSource.class).toInstance(pool);
			})).hasStackTraceContaining("Schema validation");
			assertThat(pool.isRunning()).isTrue();
			assertThat(pool.isClosed()).isFalse();
		}
	}

	@Test
	void persistenceXmlJndiDataSourceIsUsedDirectlyAndNotClosed() throws Exception {
		try (var pool = applicationPool()) {
			PoolJndiFactory.dataSource = pool;
			final ConnectionProvider provider = open(new DatabaseConfigProperties(), new HibernateModule("xml-datasource"));
			assertThat(provider.unwrap(DataSource.class)).isSameAs(pool);
			query(provider);
			injector.getInstance(PersistService.class).stop();
			assertThat(pool.isClosed()).isFalse();
		}
	}

	@Test
	void invalidExplicitProviderFailsInsteadOfFallingBack() {
		final DatabaseConfigProperties config = jdbcConfig();
		config.getProperties().put(CONNECTION_PROVIDER, "not.a.ConnectionProvider");
		assertThatThrownBy(() -> open(config, new HibernateModule())).hasStackTraceContaining("not.a.ConnectionProvider");
	}

	@Test
	void unavailableExplicitPoolFailsInsteadOfSilentlyChoosingHikari() {
		final DatabaseConfigProperties config = jdbcConfig();
		config.getProperties().put("hibernate.c3p0.max_size", "3");
		assertThatThrownBy(() -> open(config, new HibernateModule())).hasStackTraceContaining("c3p0");
	}

	@Test
	void rejectsInvalidDefaultPoolConfiguration() {
		final DatabaseConfigProperties config = jdbcConfig();
		config.getPool().setMaxPoolSize(0);
		assertThatThrownBy(() -> open(config, new HibernateModule())).hasStackTraceContaining("pool.maxPoolSize");
		config.getPool().setMaxPoolSize(1);
		config.getPool().setConnectionTimeout(Duration.milliseconds(249));
		assertThatThrownBy(() -> open(config, new HibernateModule())).hasStackTraceContaining("pool.connectionTimeout");
	}

	@Test
	void previouslyAutoSelectedProviderIsNotDisplacedByTheHikariDependency() throws Exception {
		final var bootstrap = new BootstrapServiceRegistryBuilder().applyStrategySelectors(() -> List.of(
				new SimpleStrategyRegistrationImpl<>(ConnectionProvider.class, TrackingConnectionProvider.class, "custom")))
				.build();
		try (var registry = new StandardServiceRegistryBuilder(bootstrap)
				.applySetting(ConnectionPoolContributor.POOL_CONFIG, new ConnectionPoolConfig())
				.applySetting("hibernate.connection.url", jdbcConfig().getUrl()).build()) {
			final ConnectionProvider provider = registry.requireService(ConnectionProvider.class);
			assertThat(provider).isExactlyInstanceOf(TrackingConnectionProvider.class);
			query(provider);
		} finally {
			bootstrap.close();
		}
	}

	private static HikariDataSource applicationPool() {
		final var pool = new HikariDataSource();
		pool.setJdbcUrl(jdbcConfig().getUrl());
		pool.setMaximumPoolSize(2);
		pool.setConnectionTimeout(7_000);
		return pool;
	}

	public static class TrackingConnectionProvider extends DriverManagerConnectionProvider {
		boolean stopped;
		@Override public void stop() { super.stop(); stopped = true; }
	}

	public static class PoolJndiFactory implements InitialContextFactory {
		static DataSource dataSource;
		@Override
		public Context getInitialContext(final Hashtable<?, ?> environment) {
			try {
				final Context context = mock(Context.class);
				when(context.getNameParser(anyString())).thenReturn(CompositeName::new);
				when(context.lookup(any(Name.class))).thenReturn(dataSource);
				return context;
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}
}
