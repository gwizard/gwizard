package org.gwizard.healthchecks.autoconfig;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import org.gwizard.healthchecks.HealthChecks;
import org.mockito.Mock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Provider;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class AutoHealthCheckListenerTest {

	@Mock
	TypeEncounter typeEncounter;

	@Mock
	Provider<HealthChecks> healthChecksProvider;

	AutoHealthCheckListener autoServiceListener;

	@BeforeClass
	public void setUp() {
		initMocks(this);
	}

	@BeforeMethod
	public void resetMock() {
		reset(typeEncounter);
		autoServiceListener = new AutoHealthCheckListener(healthChecksProvider);
	}

	@Test
	public void shouldMatchHealthCheck() {
		// given
		TypeLiteral typeLiteral = TypeLiteral.get(AutoHealthCheckTest.ExampleHealthCheck.class);

		// when
		autoServiceListener.hear(typeLiteral, typeEncounter);

		// then
		verify(typeEncounter, times(1)).register(any(InjectionListener.class));
	}
}