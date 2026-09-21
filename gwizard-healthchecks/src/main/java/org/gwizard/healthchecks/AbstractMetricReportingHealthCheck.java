package org.gwizard.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;
import io.dropwizard.util.Duration;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Exposes a health check as a Micrometer gauge, evaluated whenever the registry samples it.
 * Install MetricsModule alongside HealthChecksModule and bind a singleton MeterRegistry.
 *
 * <p>The metric name combines the configured prefix and health check name, for example
 * {@code gwizard.healthChecks.database}. Publishing is determined by the chosen registry.</p>
 */
@Slf4j
public abstract class AbstractMetricReportingHealthCheck extends HealthCheck {

	private final String healthCheckName;
	private Optional<Duration> cacheInterval = Optional.absent();

	/**
	 * ctor for creating a simple gauge metric that calls a health check
	 * @see Gauge
	 */
	public AbstractMetricReportingHealthCheck(HealthChecks healthChecks, String healthCheckName) {
		this.healthCheckName = healthCheckName;
		healthChecks.add(healthCheckName, this);
	}


	/**
	 * ctor for creating a cached gauge metric that calls a health check. If your metric might be queried very often,
	 * and your health check is expensive, you might want to throttle how often the check is called.
	 * <p/>NOTE: this will only serve as a throttle for the healthcheck when called via metrics reporting. Any other
	 * mechanisms that might call the healthcheck will not be throttled (e.g. if the HealthCheckService is configured
	 * to run periodically, or you expose the healthchecks via REST)
	 */
	public AbstractMetricReportingHealthCheck(HealthChecks healthChecks, String healthCheckName, Duration cacheInterval) {
		this(healthChecks, healthCheckName);
		this.cacheInterval = Optional.of(cacheInterval);
	}

	/**
	 * Calls the health check's check() method and converts the result to a gauge value.
	 * @return 1 for healthy, 0 for unhealthy, or NaN if the check throws
	 */
	private Double checkAndConvert() {
		try {
			Result result = check();
			if (!result.isHealthy()) {
				log.warn("{} : unhealthy - {}", healthCheckName, Strings.nullToEmpty(result.getMessage()), result.getError());
			}
			return result.isHealthy() ? 1.0 : 0.0;
		} catch (Exception e) {
			log.warn("Exception performing health check {}", healthCheckName, e);
			return Double.NaN;
		}
	}

	/**
	 * Creates the gauge. Uses method injection so that subclasses don't need additional
	 * ctor params.
	 */
	@Inject
	private void init(final HealthChecksConfig healthChecksConfig, final MeterRegistry registry) {
		Supplier<Double> value = this::checkAndConvert;
		if (cacheInterval.isPresent()) {
			value = Suppliers.memoizeWithExpiration(value, cacheInterval.get().getQuantity(), cacheInterval.get().getUnit());
		}
		final String prefix = healthChecksConfig.getMetricsPrefix();
		final String name = Strings.isNullOrEmpty(prefix) ? healthCheckName : prefix + "." + healthCheckName;
		Gauge.builder(name, value, Supplier::get).strongReference(true).register(registry);
	}
}
