package org.gwizard.services;

import com.google.common.util.concurrent.ServiceManager;
import io.dropwizard.util.Duration;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RunTest {

	@Test
	void shutdownWaitPreservesTheConfiguredUnitAndValidatesTheDuration() throws Exception {
		final ServiceManager manager = mock(ServiceManager.class, RETURNS_SELF);
		final Run run = new Run(() -> manager);
		assertThat(run.getStopTimeout()).isEqualTo(Duration.seconds(40));
		run.setStopTimeout(Duration.milliseconds(250));
		run.stop();
		verify(manager).awaitStopped(250, TimeUnit.MILLISECONDS);
		assertThatIllegalArgumentException().isThrownBy(() -> run.setStopTimeout(null));
		assertThatIllegalArgumentException().isThrownBy(() -> run.setStopTimeout(Duration.nanoseconds(-1)));
		run.setStopTimeout(Duration.seconds(0));
		run.stop();
		verify(manager).awaitStopped(0, TimeUnit.SECONDS);
	}
}
