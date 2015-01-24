package com.voodoodyne.gwizard.healthchecks;

import com.codahale.metrics.CachedGauge;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import io.dropwizard.util.Duration;
import lombok.extern.slf4j.Slf4j;

/**
 * classes which derive from this class will expose their healthcheck as a Gauge Metric. So, the health check will be
 * run whenever metrics are reported.
 *
 * <p/>By default, gwizard-metrics creates a JmxReporter. So, if any tool is scraping the JMX metrics tree, the health
 * checks will be run at whatever rate the tools is performing JMX queries that include the Metrics MBeans.
 *
 * <p/>The name of the metric will be constructed fromt he prefix read from the configuration, plus the name passed to
 * the ctor. By default, the metric mbean will have the ObjectName:
 * {@code metrics:name=gwizard.healthChecks.&lt;healthcheckName&gt;}
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
	 * @see CachedGauge
	 */
	public AbstractMetricReportingHealthCheck(HealthChecks healthChecks, String healthCheckName, Duration cacheInterval) {
		this(healthChecks, healthCheckName);
		this.cacheInterval = Optional.of(cacheInterval);
	}

	/**
	 * calls the health check's check() method, and converts the returned value to an integer.
	 * @return Integer value representing the result of the health check (1: healthy, 0: unhealthy, null: exception during check)
	 */
	private Integer checkAndConvert() {
		try {
			Result result = check();
			if (!result.isHealthy()) {
				log.warn("{} : unhealthy - {}", healthCheckName, Strings.nullToEmpty(result.getMessage()), result.getError());
			}
			return result.isHealthy() ? 1 : 0;
		} catch (Exception e) {
			log.warn("exception performing health check: ", e.getMessage());
			return null;
		}
	}

	/**
	 * creates the Gauge/CachedGauge. uses method injection so that subclasses of this class don't have to have cluttered
	 * ctor params.
	 */
	@Inject
	private void init(HealthChecksConfig healthChecksConfig, MetricRegistry metricRegistry) {
		Metric m;
		if (cacheInterval.isPresent()) {
			m = new CachedGauge<Integer>(cacheInterval.get().getQuantity(), cacheInterval.get().getUnit()) {
				@Override
				protected Integer loadValue() {
					return checkAndConvert();
				}
			};

		} else {
			m = new Gauge<Integer>() {
				@Override
				public Integer getValue() {
					return checkAndConvert();
				}
			};
		}
		metricRegistry.register(MetricRegistry.name(healthChecksConfig.getMetricsPrefix(), healthCheckName), m);
	}
}
